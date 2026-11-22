package CamNecT.server.domain.community.service;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.CommunityErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CommentPostingLimit {
    private final JdbcTemplate jdbc;

    /** Caller holds the author's account row lock. Deleted comments also count. */
    public void requireAvailable(Long userId) {
        var cutoff = LocalDateTime.now().minusSeconds(30);
        boolean recent = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM comments WHERE user_id = ? AND created_at > ?)
                    OR EXISTS(SELECT 1 FROM activity_comments WHERE user_id = ? AND created_at > ?)
                """, Boolean.class, userId, cutoff, userId, cutoff));
        if (recent) throw new CustomException(CommunityErrorCode.COMMENT_CREATE_COOLDOWN);
    }
}
