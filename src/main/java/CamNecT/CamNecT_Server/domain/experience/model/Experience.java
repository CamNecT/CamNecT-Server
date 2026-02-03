package CamNecT.CamNecT_Server.domain.experience.model;

import CamNecT.CamNecT_Server.domain.users.model.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "experience")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "experience_id")
    private Long experienceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName; // 회사명

    @Column(name = "major_name", length = 100)
    private String majorName; // 직무

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // 취업일

    @Column(name = "end_date")
    private LocalDate endDate; // 퇴직일

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent; // 재직중 여부

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public void updateExperience(
            String companyName,
            String majorName,
            LocalDate startDate,
            LocalDate endDate,
            Boolean isCurrent,
            String description
    ) {
        this.companyName = companyName;
        this.majorName = majorName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isCurrent = isCurrent;
        this.description = description;
    }
}