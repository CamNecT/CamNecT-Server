package CamNecT.server.domain.auth.dto;

public enum LoginNextStep {
    HOME, //정상 로그인
    DOCUMENT_REQUIRED, //서류 미제출
    ONBOARDING_REQUIRED, //선택 프로필 입력 또는 건너뛰기 필요
    DOCUMENT_REVIEW_WAITING, //서류 검토중
    VERIFICATION_COMPLETE, //승인 및 온보딩 완료 후 최초 로그인에 한 번 반환
    ADMIN_DASHBOARD //어드민 화면 이동
}
