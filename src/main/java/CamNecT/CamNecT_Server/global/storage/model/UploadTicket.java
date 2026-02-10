package CamNecT.CamNecT_Server.global.storage.model;

import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.StorageErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "upload_tickets",
        indexes = {
                @Index(name = "idx_upload_ticket_user_purpose_status", columnList = "user_id,purpose,status"),
                @Index(name = "idx_upload_ticket_expires", columnList = "expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_upload_ticket_storage_key", columnNames = "storage_key")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UploadTicket {

    public enum Status { PENDING, USED, EXPIRED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 50)
    private UploadPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "used_ref_type", length = 30)
    private UploadRefType usedRefType;

    @Column(name = "used_ref_id")
    private Long usedRefId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isUsable(LocalDateTime now) {
        return status == Status.PENDING && expiresAt.isAfter(now);
    }

    public void markUsed(UploadRefType refType, Long refId) {
        this.status = Status.USED;
        this.usedAt = LocalDateTime.now();
        this.usedRefType = refType;
        this.usedRefId = refId;
    }

    public void markExpired() {
        this.status = Status.EXPIRED;
    }

    public void updateStorageKey(String newStorageKey) {
        if (newStorageKey == null || newStorageKey.isBlank()) {
            throw new CustomException(StorageErrorCode.STORAGE_KEY_REQUIRED);
        }
        this.storageKey = newStorageKey;
    }
}