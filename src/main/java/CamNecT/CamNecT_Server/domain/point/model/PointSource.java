package CamNecT.CamNecT_Server.domain.point.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointSource {
    // 적립(EARN) 관련
    COMMENT_SELECTION("댓글 채택 적립"),
    COFFEECHAT_ACCEPTANCE("커피챗 수락 적립"),
    SIGNUP("회원가입 시 지급"),

    // 사용(SPEND) 관련
    COFFEECHAT_REQUEST("커피챗 요청 사용"),
    POST_ACCESS_PURCHASE("정보글 열람권 구매"),

    // 기타
    ADMIN_ADJUSTMENT("관리자 수동 조정");

    private final String description;
}