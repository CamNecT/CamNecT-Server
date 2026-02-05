package CamNecT.CamNecT_Server.domain.community.service;

import CamNecT.CamNecT_Server.domain.community.dto.response.PostListResponse;

public interface PostQueryService {

    enum Sort {
        RECOMMENDED, LATEST, LIKE, BOOKMARK
    }

    enum Tab {
        ALL, INFO, QUESTION
    }

    PostListResponse getPosts(Tab tab, Sort sort, Long tagId, String keyword,
                              Long cursorId, Long cursorValue, int size);

    // 관심태그 기반 추천(게시판 상관없이)
    PostListResponse getPostsByTag(Long tagId, Long cursorValue, Long cursorId, int size);

    // 답변대기 질문(QUESTION + rootCommentCount==0)
    PostListResponse getWaitingQuestions(int size);
}
