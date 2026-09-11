package CamNecT.server.domain.report.service;

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
