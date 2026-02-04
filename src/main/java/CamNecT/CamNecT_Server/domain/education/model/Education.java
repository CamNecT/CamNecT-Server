package CamNecT.CamNecT_Server.domain.education.model;

import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.global.tag.model.Institutions;
import CamNecT.CamNecT_Server.global.tag.model.Majors;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

// 학력
@Entity
@Table(name = "Education")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "education_id")
    private Long educationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institutions institution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id", nullable = false)
    private Majors major;

    @Column(name = "degree", length = 50)
    private String degree;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EducationStatus status;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public void updateEducation(Institutions institution,
                                LocalDate startDate, LocalDate endDate,
                                EducationStatus status, String description) {
        this.institution = institution;
//        this.major = major;
//        this.degree = degree;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.description = description;
    }
}