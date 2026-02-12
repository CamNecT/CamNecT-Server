package CamNecT.CamNecT_Server.domain.community.service;

import CamNecT.CamNecT_Server.domain.community.dto.AuthorDto;
import CamNecT.CamNecT_Server.domain.profile.dto.ProfileGlobalDto;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import CamNecT.CamNecT_Server.global.storage.service.PublicUrlIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthorAssembler {

    private final UserProfileRepository userProfileRepository;
    private final PublicUrlIssuer publicUrlIssuer;

    public Map<Long, AuthorDto> buildAuthorMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        Map<Long, ProfileGlobalDto> globalMap = userProfileRepository.findGlobalsByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(ProfileGlobalDto::userId, it -> it));

        Map<Long, AuthorDto> authorMap = new HashMap<>();
        for (Long uid : userIds) {
            ProfileGlobalDto g = globalMap.get(uid);
            if (g == null) continue;

            String studentNo = StringUtils.hasText(g.studentNo()) ? g.studentNo() : "학번 미입력";
            String majorName = StringUtils.hasText(g.majorName()) ? g.majorName() : "전공 미입력";

            String imgUrl = null;
            if (StringUtils.hasText(g.profileImageKey())) {
                imgUrl = publicUrlIssuer.issuePublicUrl(g.profileImageKey());
            }

            authorMap.put(uid, new AuthorDto(
                    uid,
                    g.userName(),
                    imgUrl,                 // nullable
                    studentNo,
                    majorName
            ));
        }
        return authorMap;
    }
}