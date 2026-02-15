package CamNecT.server.domain.activity.model.enums;

public enum ActivityCategory {
    STUDY("스터디"),
    CLUB("동아리"),
    EXTERNAL("대외활동"),
    RECRUITMENT("취업정보");

    private final String description;

    ActivityCategory(String description) { this.description = description; }
}
