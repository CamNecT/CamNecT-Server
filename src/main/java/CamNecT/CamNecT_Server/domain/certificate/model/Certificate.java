package CamNecT.CamNecT_Server.domain.certificate.model;

import CamNecT.CamNecT_Server.domain.users.model.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificate")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id")
    private Long certificateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "certificate_name", nullable = false, length = 100)
    private String certificateName;

    @Column(name = "issuer_name", length = 100)
    private String issuerName;

    @Column(name = "acquired_date", nullable = false)
    private LocalDate acquiredDate; // 취득일

    @Column(name = "expire_date")
    private LocalDate expireDate; // 만료일

    @Column(name = "credential_url", length = 500)
    private String credentialUrl; // 증명 URL

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;


    public void updateCertificate(String certificateName, LocalDate acquiredDate, String credentialUrl) {
        this.certificateName = certificateName;
        this.acquiredDate = acquiredDate;
        this.credentialUrl = credentialUrl;
//        this.issuerName = issuerName;
//        this.expireDate = expireDate;
//        this.description = description;
    }
}