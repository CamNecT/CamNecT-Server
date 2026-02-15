package CamNecT.server.domain.profile.components.experience.model;

import CamNecT.server.domain.users.model.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // 취업일

    @Column(name = "end_date")
    private LocalDate endDate; // 퇴직일

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent; // 재직중 여부

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "experience_responsibilities",
            joinColumns = @JoinColumn(name = "experience_id")
    )
    @Column(name = "responsibility_content", columnDefinition = "TEXT")
    @Builder.Default
    private List<String> responsibilities = new ArrayList<>();

    public void updateExperience(
            String companyName,
            LocalDate startDate,
            LocalDate endDate,
            Boolean isCurrent,
            List<String> responsibilities
    ) {
        this.companyName = companyName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isCurrent = isCurrent;
        this.responsibilities.clear();
        if (responsibilities != null) {
            this.responsibilities.addAll(responsibilities);
        }
    }
}