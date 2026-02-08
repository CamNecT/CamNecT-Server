package CamNecT.CamNecT_Server.domain.community.service;

import CamNecT.CamNecT_Server.domain.community.dto.response.CommunityHomeResponse;
import CamNecT.CamNecT_Server.domain.community.dto.response.PostListResponse;
import CamNecT.CamNecT_Server.domain.users.repository.UserTagMapRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityHomeServiceImpl implements CommunityHomeService {

    private final PostQueryService postQueryService;
    private final UserTagMapRepository userTagMapRepository; // 유저-태그 매핑

    @Override
    public CommunityHomeResponse getHome(Long userId) {
        if (userId == null) { throw new CustomException(UserErrorCode.USER_NOT_FOUND); }

        Tag picked = pickRandomTagOrNull(userId);

        Long tagId = (picked == null) ? null : picked.getId();
        String tagName = (picked == null) ? null : picked.getName();

        var recommended = (tagId == null)
                ? empty()
                : postQueryService.getPostsByTag(tagId, null, null, 10); // 여기 "추천순" 기본인지 확인 필요

        var waiting = postQueryService.getWaitingQuestions(3);

        return new CommunityHomeResponse(
                tagId,
                tagName,
                recommended.items(),
                waiting.items()
        );
    }

    private Tag pickRandomTagOrNull(Long userId) {
        List<Tag> tags = userTagMapRepository.findAllTagsByUserId(userId);

        tags = tags.stream()
                .filter(Tag::isActive)
                .toList();

        if (tags.isEmpty()) return null;

        int idx = java.util.concurrent.ThreadLocalRandom.current().nextInt(tags.size());
        return tags.get(idx);
    }

    private static PostListResponse empty() {
        return PostListResponse.of(List.of(), false, null);
    }
}
