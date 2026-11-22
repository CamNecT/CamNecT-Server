package CamNecT.server.domain.report.service;

import CamNecT.server.global.storage.service.PresignEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.LocalDateTime;

/** Each item is rechecked under the same case lock used by moderation. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportRetentionService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final PresignEngine storage;
    private final Clock clock;

    @Scheduled(cron = "${app.report.retention-cron:0 */10 * * * *}")
    public void expire() {
        var now = LocalDateTime.now(clock);
        var cutoff = now.minusYears(1);
        var candidates = jdbc.query("""
                SELECT e.evidence_id, r.case_id FROM report_evidence e
                JOIN report r ON r.report_id = e.report_id JOIN report_case c ON c.case_id = r.case_id
                WHERE e.expired_at IS NULL AND e.created_at <= ? AND (c.processed_at IS NULL OR c.processed_at <= ?)
                ORDER BY e.evidence_id LIMIT 200
                """, (rs, i) -> new Candidate(rs.getLong(1), rs.getLong(2)), cutoff, cutoff);
        for (var item : candidates) {
            tx.executeWithoutResult(status -> {
                if (caseStillExpired(item.caseId(), cutoff)) {
                    jdbc.update("UPDATE report_evidence SET expired_at = ? WHERE evidence_id = ? AND expired_at IS NULL", now, item.id());
                }
            });
        }

        // A committed expiry marker is a retry record. Case decisions cannot revive a file once deletion starts.
        var marked = jdbc.queryForList("SELECT evidence_id FROM report_evidence WHERE expired_at IS NOT NULL ORDER BY expired_at, evidence_id LIMIT 200", Long.class);
        for (long id : marked) {
            try {
                tx.executeWithoutResult(status -> purgeFile(id));
            } catch (RuntimeException failure) {
                // Requeue behind other pending files after storage failure.
                jdbc.update("UPDATE report_evidence SET expired_at = ? WHERE evidence_id = ? AND expired_at IS NOT NULL", now, id);
                log.warn("Report evidence deletion will retry, evidenceId={}", id, failure);
            }
        }

        var penalties = jdbc.query("""
                SELECT p.penalty_id, p.case_id FROM user_report_penalty p JOIN report_case c ON c.case_id = p.case_id
                WHERE p.created_at <= ? AND (c.processed_at IS NULL OR c.processed_at <= ?)
                  AND (p.suspension_end_date IS NULL OR p.suspension_end_date <= ?)
                ORDER BY p.penalty_id LIMIT 200
                """, (rs, i) -> new Candidate(rs.getLong(1), rs.getLong(2)), cutoff, cutoff, now);
        for (var item : penalties) {
            tx.executeWithoutResult(status -> {
                if (!caseStillExpired(item.caseId(), cutoff)) return;
                jdbc.query("SELECT user_id, penalty_type FROM user_report_penalty WHERE penalty_id = ? FOR UPDATE",
                        (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                            jdbc.update("""
                                    INSERT INTO expired_penalty_summary (user_id, penalty_count, permanently_banned)
                                    VALUES (?, 1, ?) ON DUPLICATE KEY UPDATE penalty_count = penalty_count + 1,
                                    permanently_banned = permanently_banned OR VALUES(permanently_banned)
                                    """, rs.getLong(1), "PERMANENT_BAN".equals(rs.getString(2)));
                            jdbc.update("DELETE FROM user_report_penalty WHERE penalty_id = ?", item.id());
                            jdbc.update("UPDATE report_case SET applied_penalty = NULL, moderation_reason = NULL WHERE case_id = ?", item.caseId());
                            jdbc.update("UPDATE report SET applied_penalty = NULL WHERE case_id = ?", item.caseId());
                        }, item.id());
            });
        }
    }

    private boolean caseStillExpired(long caseId, LocalDateTime cutoff) {
        return jdbc.query("SELECT processed_at FROM report_case WHERE case_id = ? FOR UPDATE", rs -> {
            if (!rs.next()) return false;
            var processed = rs.getTimestamp(1);
            return processed == null || !processed.toLocalDateTime().isAfter(cutoff);
        }, caseId);
    }

    private void purgeFile(long id) {
        var filesToRemove = jdbc.query("""
                SELECT e.storage_key, r.reporter_id, r.report_id FROM report_evidence e
                JOIN report r ON r.report_id = e.report_id
                WHERE e.evidence_id = ? AND e.expired_at IS NOT NULL FOR UPDATE
                """, (rs, i) -> new EvidenceFile(rs.getString(1), rs.getLong(2), rs.getLong(3)), id);
        if (filesToRemove.isEmpty()) return;
        var evidence = filesToRemove.getFirst();
        String key = evidence.key();
        // A consumed upload ticket links the exact S3 key to both the reporter and the case submission.
        int owned = jdbc.update("""
                DELETE FROM upload_tickets WHERE storage_key = ? AND user_id = ? AND purpose = 'REPORT_EVIDENCE'
                  AND status = 'USED' AND used_ref_type = 'REPORT' AND used_ref_id = ?
                """, key, evidence.reporterId(), evidence.reportId());
        if (owned == 1) {
            // Storage failure rolls back the ticket deletion so the marked row can be retried.
            storage.deleteUploadedObject(key);
        } else {
            log.warn("Expired report evidence has no matching upload ticket, evidenceId={}", id);
        }
        jdbc.update("DELETE FROM report_evidence WHERE evidence_id = ?", id);
    }

    private record EvidenceFile(String key, long reporterId, long reportId) {}
    private record Candidate(long id, long caseId) {}
}
