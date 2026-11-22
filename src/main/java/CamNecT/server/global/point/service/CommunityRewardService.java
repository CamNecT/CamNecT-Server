package CamNecT.server.global.point.service;

import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.point.model.PointSource;
import CamNecT.server.global.point.model.PointEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import CamNecT.server.domain.report.service.UserReportPenaltyService;
import CamNecT.server.domain.users.model.UserStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityRewardService {
    private final PointService points;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final UserReportPenaltyService penalties;
    private final Clock clock;

    /** The caller locks the post or its stats before evaluating a community reward. */
    @Transactional
    public boolean tryCommunityReward(long recipient, int amount, PointEvent event, Long questionOwner) {
        PointSource source = event.source();
        boolean likes = source == PointSource.THREELIKES_REWARD;
        if (!likes && (source != PointSource.COMMENT_SELECTION || questionOwner == null)) {
            throw new IllegalArgumentException("Unsupported community reward");
        }
        points.lockRewardWallet(recipient);
        var status = userRepository.findStatusByUserId(recipient).orElseThrow();
        if (status != UserStatus.ACTIVE || penalties.hasActiveRestriction(recipient, status)) return false;

        var seoul = ZoneId.of("Asia/Seoul");
        var day = LocalDate.now(clock.withZone(seoul));
        long dayStart = day.atStartOfDay(seoul).toEpochSecond();
        long nextDayStart = day.plusDays(1).atStartOfDay(seoul).toEpochSecond();
        // Historical event keys included the comment ID. Consult the ledger by post as well.
        boolean previouslyPaid = jdbcTemplate.query("""
                SELECT 1 FROM point_transaction WHERE source_type = ? AND post_id = ? AND transaction_type = 'EARN' LIMIT 1
                """, rs -> { return rs.next(); }, source.name(), event.postId());
        if (previouslyPaid) return false;
        boolean withinDailyLimit = jdbcTemplate.query("""
                SELECT COALESCE(SUM(point_change), 0), COUNT(CASE WHEN source_type = ? THEN 1 END)
                FROM point_transaction WHERE user_id = ? AND transaction_type = 'EARN'
                  AND source_type IN ('THREELIKES_REWARD', 'COMMENT_SELECTION')
                  AND created_at >= FROM_UNIXTIME(?) AND created_at < FROM_UNIXTIME(?)
                """, rs -> { rs.next(); return rs.getLong(1) + amount <= 500 && rs.getInt(2) < (likes ? 1 : 2); },
                source.name(), recipient, dayStart, nextDayStart);
        if (!withinDailyLimit) return false;

        // Pair timestamps and their ledger backfill use the database session clock.
        var now = jdbcTemplate.queryForObject("SELECT CURRENT_TIMESTAMP(6)", LocalDateTime.class);
        var cutoff = now.minusDays(30);
        var peers = likes ? jdbcTemplate.queryForList("""
                SELECT l.user_id FROM post_likes l
                LEFT JOIN community_reward_pairs p ON p.source_type = 'THREELIKES_REWARD'
                  AND p.low_user_id = LEAST(l.user_id, ?) AND p.high_user_id = GREATEST(l.user_id, ?)
                WHERE l.post_id = ? AND l.user_id <> ? AND (p.rewarded_at IS NULL OR p.rewarded_at <= ?)
                ORDER BY l.post_like_id LIMIT 3
                """, Long.class, recipient, recipient, event.postId(), recipient, cutoff) : List.of(questionOwner);
        if (peers.size() != (likes ? 3 : 1)) return false;

        // A reverse-role award on another post must see the same locked pair.
        for (long peer : peers.stream().sorted().toList()) {
            long low = Math.min(recipient, peer);
            long high = Math.max(recipient, peer);
            jdbcTemplate.update("""
                    INSERT INTO community_reward_pairs (source_type, low_user_id, high_user_id) VALUES (?, ?, ?)
                    ON DUPLICATE KEY UPDATE low_user_id = low_user_id
                    """, source.name(), low, high);
            boolean eligible = jdbcTemplate.query("""
                    SELECT rewarded_at FROM community_reward_pairs
                    WHERE source_type = ? AND low_user_id = ? AND high_user_id = ? FOR UPDATE
                    """, rs -> { rs.next(); var last = rs.getTimestamp(1);
                        return last == null || !last.toLocalDateTime().isAfter(cutoff); }, source.name(), low, high);
            if (!eligible) return false;
        }
        for (long peer : peers) {
            jdbcTemplate.update("""
                    UPDATE community_reward_pairs SET rewarded_at = ?
                    WHERE source_type = ? AND low_user_id = ? AND high_user_id = ?
                    """, now, source.name(), Math.min(recipient, peer), Math.max(recipient, peer));
        }
        points.earnPoint(recipient, amount, event);
        return true;
    }

}
