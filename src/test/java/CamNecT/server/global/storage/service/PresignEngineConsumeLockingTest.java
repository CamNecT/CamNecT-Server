package CamNecT.server.global.storage.service;

import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.StorageErrorCode;
import CamNecT.server.global.storage.config.PresignProps;
import CamNecT.server.global.storage.config.S3Props;
import CamNecT.server.global.storage.model.UploadPurpose;
import CamNecT.server.global.storage.model.UploadRefType;
import CamNecT.server.global.storage.model.UploadTicket;
import CamNecT.server.global.storage.repository.UploadTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PresignEngineConsumeLockingTest {

    private static final String TEMP_KEY = "temp/reports/evidence.jpg";

    @Mock S3Presigner presigner;
    @Mock S3Client s3;
    @Mock UploadTicketRepository ticketRepository;

    private PresignEngine presignEngine;

    @BeforeEach
    void setUp() {
        presignEngine = new PresignEngine(
                presigner,
                s3,
                new S3Props("bucket", "ap-northeast-2", "camnect"),
                new PresignProps(600, 600),
                ticketRepository,
                Clock.systemDefaultZone()
        );
    }

    @Test
    void locksTicketBeforeRejectingReuseByAnotherUser() {
        when(ticketRepository.findByStorageKeyForUpdate(TEMP_KEY))
                .thenReturn(Optional.of(ticket(2L, UploadPurpose.REPORT_EVIDENCE, UploadTicket.Status.PENDING)));

        assertConsumeFailsWith(StorageErrorCode.UPLOAD_TICKET_FORBIDDEN, 1L, UploadPurpose.REPORT_EVIDENCE, 10L);

        verify(ticketRepository).findByStorageKeyForUpdate(TEMP_KEY);
        verifyNoInteractions(s3);
    }

    @Test
    void locksTicketBeforeRejectingReuseForAnotherPurpose() {
        when(ticketRepository.findByStorageKeyForUpdate(TEMP_KEY))
                .thenReturn(Optional.of(ticket(1L, UploadPurpose.PROFILE_IMAGE, UploadTicket.Status.PENDING)));

        assertConsumeFailsWith(StorageErrorCode.UPLOAD_TICKET_FORBIDDEN, 1L, UploadPurpose.REPORT_EVIDENCE, 10L);

        verify(ticketRepository).findByStorageKeyForUpdate(TEMP_KEY);
        verifyNoInteractions(s3);
    }

    @Test
    void locksTicketBeforeRejectingReuseForAnotherReference() {
        when(ticketRepository.findByStorageKeyForUpdate(TEMP_KEY))
                .thenReturn(Optional.of(ticket(1L, UploadPurpose.REPORT_EVIDENCE, UploadTicket.Status.USED)));

        assertConsumeFailsWith(StorageErrorCode.UPLOAD_TICKET_EXPIRED_OR_USED, 1L, UploadPurpose.REPORT_EVIDENCE, 20L);

        verify(ticketRepository).findByStorageKeyForUpdate(TEMP_KEY);
        verifyNoInteractions(s3);
    }

    @Test
    void finalizesTheInspectedContentWithTheTicketsMediaType() {
        UploadTicket pending = stubPendingTicket();
        stubHead("\"original\"");

        String key = presignEngine.consume(1L, UploadPurpose.REPORT_EVIDENCE, UploadRefType.REPORT,
                10L, TEMP_KEY, "reports/final");

        var request = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3).copyObject(request.capture());
        assertThat(request.getValue().copySourceIfMatch()).isEqualTo("\"original\"");
        assertThat(request.getValue().metadataDirective()).isEqualTo(MetadataDirective.REPLACE);
        assertThat(request.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(pending.getStorageKey()).isEqualTo(key);
        assertThat(pending.getStatus()).isEqualTo(UploadTicket.Status.USED);
    }

    @Test
    void changedContentLeavesTheTicketPending() {
        UploadTicket pending = stubPendingTicket();
        stubHead("\"original\"");
        when(s3.copyObject(any(CopyObjectRequest.class))).thenThrow(S3Exception.builder().statusCode(412).build());

        assertConsumeFailsWith(StorageErrorCode.UPLOAD_TICKET_MISMATCHED_OBJECT, 1L, UploadPurpose.REPORT_EVIDENCE, 10L);

        assertThat(pending.getStorageKey()).isEqualTo(TEMP_KEY);
        assertThat(pending.getStatus()).isEqualTo(UploadTicket.Status.PENDING);
    }

    @Test
    void missingContentIdentifierPreventsFinalization() {
        stubPendingTicket();
        stubHead(null);

        assertConsumeFailsWith(StorageErrorCode.UPLOAD_TICKET_MISMATCHED_OBJECT, 1L, UploadPurpose.REPORT_EVIDENCE, 10L);

        verify(s3, never()).copyObject(any(CopyObjectRequest.class));
    }

    private UploadTicket stubPendingTicket() {
        UploadTicket pending = ticket(1L, UploadPurpose.REPORT_EVIDENCE, UploadTicket.Status.PENDING);
        when(ticketRepository.findByStorageKeyForUpdate(TEMP_KEY)).thenReturn(Optional.of(pending));
        return pending;
    }

    private void stubHead(String eTag) {
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder()
                .contentLength(100L).contentType("image/jpeg").eTag(eTag).build());
    }

    private void assertConsumeFailsWith(
            StorageErrorCode expected,
            Long userId,
            UploadPurpose purpose,
            Long refId
    ) {
        assertThatThrownBy(() -> presignEngine.consume(
                userId,
                purpose,
                UploadRefType.REPORT,
                refId,
                TEMP_KEY,
                "reports/final"
        )).isInstanceOfSatisfying(CustomException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getErrorCode()).isEqualTo(expected)
        );
    }

    private static UploadTicket ticket(
            Long userId,
            UploadPurpose purpose,
            UploadTicket.Status status
    ) {
        return UploadTicket.builder()
                .userId(userId)
                .purpose(purpose)
                .status(status)
                .storageKey(TEMP_KEY)
                .originalFilename("evidence.jpg")
                .contentType("image/jpeg")
                .size(100L)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
    }
}
