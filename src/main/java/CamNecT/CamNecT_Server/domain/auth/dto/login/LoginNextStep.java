package CamNecT.CamNecT_Server.domain.auth.dto.login;

public enum LoginNextStep {
    HOME, //정상 로그인
    DOCUMENT_REQUIRED, //서류 미제출
    ONBOARDING_REQUIRED, //서류는 제출했으나, 초기설정 필요
    DOCUMENT_REVIEW_WAITING, //서류 검토중
    VERIFICATION_COMPLETE, //인증완료
    ADMIN_DASHBOARD //어드민 화면 이동
}