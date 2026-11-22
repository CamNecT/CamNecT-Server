package CamNecT.server.domain.report.service;

import CamNecT.server.domain.community.model.Boards;
import CamNecT.server.domain.community.model.Posts.Posts;
import CamNecT.server.domain.community.model.enums.BoardCode;
import CamNecT.server.domain.community.repository.BoardsRepository;
import CamNecT.server.domain.community.repository.Posts.PostsRepository;
import CamNecT.server.domain.report.dto.request.ReportProcessRequest;
import CamNecT.server.domain.users.model.UserRole;
import org.junit.jupiter.params.provider.ValueSource;
import CamNecT.server.domain.report.dto.request.ReportCreateRequest;
import CamNecT.server.domain.report.model.ReportCategory;
import CamNecT.server.domain.report.model.ReportStatus;
import CamNecT.server.domain.report.model.TargetType;
import CamNecT.server.domain.report.repository.ReportCaseRepository;
import CamNecT.server.domain.report.repository.ReportRepository;
import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.ReportErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import CamNecT.server.domain.report.repository.UserReportPenaltyRepository;
import CamNecT.server.domain.report.repository.ReportEvidenceRepository;
import CamNecT.server.domain.report.model.UserReportPenalty;
import CamNecT.server.domain.report.model.ReportEvidence;
import CamNecT.server.domain.report.model.PenaltyType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import CamNecT.server.global.storage.config.S3Props;
import CamNecT.server.global.storage.model.UploadPurpose;
import CamNecT.server.global.storage.model.UploadRefType;
import CamNecT.server.global.storage.model.UploadTicket;
import CamNecT.server.global.storage.repository.UploadTicketRepository;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Transactional
class ReportCaseLifecycleIntegrationTest {
    @Autowired ReportService service;
    @Autowired ReportRepository reports;
    @Autowired ReportCaseRepository cases;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactionManager;

    @Autowired PostsRepository posts;
    @Autowired BoardsRepository boards;
    @Autowired ReportRetentionService retention;
    @Autowired JdbcTemplate jdbc;
    @Autowired UploadTicketRepository tickets;
    @Autowired S3Props s3Props;
    @MockitoBean S3Client s3;
    @Autowired ReportEvidenceRepository evidence;
    @Autowired UserReportPenaltyRepository penalties;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void approvalAfterPhysicalDeletionRequiresVerifiedAuthor(boolean verifiedAtSubmission) {
        var admin = users.saveAndFlush(Users.builder().username(UUID.randomUUID().toString())
                .passwordHash("unused").name("admin").status(UserStatus.ACTIVE).role(UserRole.ADMIN).build());
        var author = user();
        var board = boards.findByCode(BoardCode.INFO).orElseGet(() -> boards.save(Boards.of(BoardCode.INFO, "정보")));
        var post = posts.saveAndFlush(Posts.create(board, author, "title", "content", false));
        var reportId = service.createReport(user().getUserId(), new ReportCreateRequest(author.getUserId(),
                post.getId(), TargetType.COMMUNITY, ReportCategory.OTHER, "title", "context", List.of()));
        long caseId = reports.findById(reportId).orElseThrow().getReportCase().getCaseId();
        posts.deleteById(post.getId());
        var decision = new ReportProcessRequest(ReportStatus.RESOLVED, ReportCategory.OTHER, "reviewed");
        if (verifiedAtSubmission) {
            service.processReport(admin.getUserId(), caseId, decision);
            assertThat(cases.findById(caseId).orElseThrow().getStatus()).isEqualTo(ReportStatus.RESOLVED);
            assertThat(penalties.countByUser_UserId(author.getUserId())).isOne();
        } else {
            jdbc.update("UPDATE report_case SET verified_author_id = NULL WHERE case_id = ?", caseId);
            assertThat(assertThrows(CustomException.class, () -> service.processReport(admin.getUserId(), caseId, decision))
                    .getErrorCode()).isEqualTo(ReportErrorCode.REPORT_TARGET_INTEGRITY_UNVERIFIED);
            assertThat(cases.findById(caseId).orElseThrow().getStatus()).isEqualTo(ReportStatus.RECEIVED);
            assertThat(penalties.countByUser_UserId(author.getUserId())).isZero();
        }
    }

    @Test
    void decisionRestartsRetentionAndPermanentRestrictionSurvivesDetailExpiry() {
        var reporter = user();
        var target = user();
        var report = reports.findById(service.createReport(reporter.getUserId(), request(target))).orElseThrow();
        var reportCase = report.getReportCase();
        var image = evidence.saveAndFlush(ReportEvidence.create(
                report, "https://example.invalid/legacy", "image.png", "image/png", 3L, 0));
        var penalty = penalties.saveAndFlush(UserReportPenalty.permanentlyBanned(reportCase, target, "reason"));
        var old = LocalDateTime.now().minusYears(2);
        reportCase.resolve(reporter.getUserId(), ReportCategory.OTHER,
                PenaltyType.PERMANENT_BAN, "reason", LocalDateTime.now().minusMonths(6));
        cases.flush();
        jdbc.update("UPDATE report_evidence SET created_at=? WHERE evidence_id=?", old, image.getEvidenceId());
        jdbc.update("UPDATE user_report_penalty SET created_at=? WHERE penalty_id=?", old, penalty.getPenaltyId());
        retention.expire();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM report_evidence WHERE evidence_id=?", Integer.class, image.getEvidenceId())).isOne();
        jdbc.update("UPDATE report_case SET processed_at=? WHERE case_id=?", old, reportCase.getCaseId());
        retention.expire();
        retention.expire();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM report_evidence WHERE evidence_id=?", Integer.class, image.getEvidenceId())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_report_penalty WHERE penalty_id=?", Integer.class, penalty.getPenaltyId())).isZero();
        assertThat(penalties.countByUser_UserId(target.getUserId())).isOne();
        assertThat(penalties.existsActiveRestriction(target.getUserId(), PenaltyType.PERMANENT_BAN,
                PenaltyType.SUSPENDED_7_DAYS, LocalDateTime.now())).isTrue();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void expiredUploadRetriesTheSameS3ObjectAndIgnoresUnownedKeys() {
        var reporter = user();
        var report = reports.findById(service.createReport(reporter.getUserId(), request(user()))).orElseThrow();
        long id = report.getReportId();
        String prefix = "reports/user-" + reporter.getUserId() + "/report-" + id + "/evidence/";
        String owned = prefix + "owned.png";
        String unowned = prefix + "legacy.png";
        var a = evidence.saveAndFlush(ReportEvidence.create(report, owned, "owned.png", "image/png", 3L, 0));
        evidence.saveAndFlush(ReportEvidence.create(report, unowned, "legacy.png", "image/png", 3L, 1));
        var ticket = UploadTicket.builder().userId(reporter.getUserId()).purpose(UploadPurpose.REPORT_EVIDENCE)
                .storageKey(owned).status(UploadTicket.Status.PENDING).size(3L).contentType("image/png")
                .expiresAt(LocalDateTime.now()).build();
        ticket.markUsed(UploadRefType.REPORT, id);
        tickets.saveAndFlush(ticket);
        jdbc.update("UPDATE report_evidence SET created_at = ? WHERE report_id = ?", LocalDateTime.now().minusYears(2), id);
        var delete = DeleteObjectRequest.builder().bucket(s3Props.bucket()).key(owned).build();
        when(s3.deleteObject(delete)).thenThrow(new IllegalStateException("storage unavailable")).thenReturn(null);
        retention.expire();
        assertThat(evidence.findById(a.getEvidenceId()).orElseThrow().getExpiredAt()).isNotNull();
        assertThat(tickets.findByStorageKey(owned)).isPresent();
        retention.expire();
        assertThat(evidence.findById(a.getEvidenceId())).isEmpty();
        assertThat(tickets.findByStorageKey(owned)).isEmpty();
        verify(s3, times(2)).deleteObject(delete);
        verifyNoMoreInteractions(s3);
    }

    @ParameterizedTest
    @EnumSource(value = ReportStatus.class, names = {"RESOLVED", "REJECTED"})
    void closedHistoryDoesNotPreventANewCaseForTheSameReporter(ReportStatus status) {
        Users reporter = user();
        Users target = user();
        ReportCreateRequest request = request(target);
        Long oldReportId = service.createReport(reporter.getUserId(), request);
        var oldCase = reports.findById(oldReportId).orElseThrow().getReportCase();
        if (status == ReportStatus.RESOLVED) {
            oldCase.resolve(99L, ReportCategory.OTHER, null, "old decision", LocalDateTime.now());
        } else {
            oldCase.reject(99L, "old decision", LocalDateTime.now());
        }
        cases.flush();

        Long newReportId = service.createReport(reporter.getUserId(), request);
        var newCase = reports.findById(newReportId).orElseThrow().getReportCase();

        assertThat(newCase.getCaseId()).isNotEqualTo(oldCase.getCaseId());
        assertThat(newCase.getStatus()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(oldCase.getStatus()).isEqualTo(status);
        assertThat(reports.findAllByReportCase_CaseIdOrderByCreatedAtAsc(oldCase.getCaseId()))
                .extracting(report -> report.getReportId()).containsExactly(oldReportId);
        assertThat(assertThrows(CustomException.class,
                () -> service.createReport(reporter.getUserId(), request)).getErrorCode())
                .isEqualTo(ReportErrorCode.REPORT_DUPLICATE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentReportsAfterRejectionShareOneNewReceivedCase() throws Exception {
        Users first = user();
        Users second = user();
        Users target = user();
        ReportCreateRequest request = request(target);
        Long oldReport = service.createReport(first.getUserId(), request);
        Long oldCaseId = new TransactionTemplate(transactionManager).execute(tx -> {
            var oldCase = reports.findById(oldReport).orElseThrow().getReportCase();
            oldCase.reject(99L, "old decision", LocalDateTime.now());
            return oldCase.getCaseId();
        });
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstReport = executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return service.createReport(first.getUserId(), request);
            });
            var secondReport = executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return service.createReport(second.getUserId(), request);
            });
            try {
                assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                Long firstReportId = firstReport.get(20, TimeUnit.SECONDS);
                Long secondReportId = secondReport.get(20, TimeUnit.SECONDS);
                new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                    var openCase = cases.findByTargetKeyAndStatus("USER:" + target.getUserId(), ReportStatus.RECEIVED)
                            .orElseThrow();
                    assertThat(openCase.getCaseId()).isNotEqualTo(oldCaseId);
                    assertThat(openCase.getReportCount()).isEqualTo(2);
                    assertThat(reports.findById(firstReportId).orElseThrow().getReportCase().getCaseId())
                            .isEqualTo(openCase.getCaseId());
                    assertThat(reports.findById(secondReportId).orElseThrow().getReportCase().getCaseId())
                            .isEqualTo(openCase.getCaseId());
                    assertThat(cases.findById(oldCaseId).orElseThrow().getStatus()).isEqualTo(ReportStatus.REJECTED);
                });
            } finally {
                start.countDown();
            }
        }
    }

    private Users user() {
        return new TransactionTemplate(transactionManager).execute(tx -> users.saveAndFlush(Users.builder()
                .username("report-" + UUID.randomUUID()).passwordHash("unused-test-hash")
                .name("report test").status(UserStatus.ACTIVE).build()));
    }

    private ReportCreateRequest request(Users target) {
        return new ReportCreateRequest(target.getUserId(), null, TargetType.USER,
                ReportCategory.OTHER, "title", "context", List.of());
    }
}
