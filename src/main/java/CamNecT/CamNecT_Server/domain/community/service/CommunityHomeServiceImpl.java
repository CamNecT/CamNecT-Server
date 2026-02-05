package CamNecT.CamNecT_Server.domain.community.service;

import CamNecT.CamNecT_Server.domain.community.dto.response.CommunityHomeResponse;
import CamNecT.CamNecT_Server.domain.community.dto.response.PostListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityHomeServiceImpl implements CommunityHomeService {

    private final PostQueryService postQueryService;

    @Override
    public CommunityHomeResponse getHome(Long tagId) {
        var recommended = (tagId == null)
                ? PostQueryServiceImpl_empty()
                : postQueryService.getPostsByTag(tagId, null, null,10);

        var waiting = postQueryService.getWaitingQuestions(3);

        return new CommunityHomeResponse(
                tagId,
                recommended.items(),
                waiting.items()
        );
    }

    // tagId 없을 때는 비워두기
    private static PostListResponse PostQueryServiceImpl_empty() {
        return PostListResponse.of(List.of(), false, null);
    }
}
