package CamNecT.CamNecT_Server.domain.profile.components.education.model;

public enum EducationStatus {
    ATTENDING("재학"),
    LEAVE_OF_ABSENCE("휴학"),
    GRADUATED("졸업"),
    EXCHANGE("교환학생"),
    DROPPED_OUT("중퇴"),
    TRANSFERRED("편입");

    private final String description;

    EducationStatus(String description) {
        this.description = description;
    }
}