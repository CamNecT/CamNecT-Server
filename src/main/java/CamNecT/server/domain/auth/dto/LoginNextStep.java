package CamNecT.server.domain.auth.dto;

public enum LoginNextStep {
    HOME, //정상 로그인
    DOCUMENT_REQUIRED, //서류 미제출
    DOCUMENT_REVIEW_WAITING, //서류 검토중
    VERIFICATION_COMPLETE, //관리자 인증 완료 후 초기 설정 안내 필요
    ADMIN_DASHBOARD //어드민 화면 이동
}
