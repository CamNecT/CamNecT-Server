package CamNecT.CamNecT_Server.domain.community.service;

import CamNecT.CamNecT_Server.domain.community.dto.AuthorDto;
import CamNecT.CamNecT_Server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import CamNecT.CamNecT_Server.global.storage.service.PublicUrlIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthorAssembler {

    private final UserProfileRepository userProfileRepository;
    private final MajorRepository majorRepository;
    private final PublicUrlIssuer publicUrlIssuer;

    public Map<Long, AuthorDto> buildAuthorMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        // 1) profile + user 벌크
        Map<Long, UserProfile> profileMap = userProfileRepository.findAllByUserIdInWithUser(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        // 2) majorId 벌크
        List<Long> majorIds = profileMap.values().stream()
                .map(UserProfile::getMajorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> majorNameMap = new HashMap<>();
        if (!majorIds.isEmpty()) {
            for (Object[] row : majorRepository.findNameKorByIdIn(majorIds)) {
                majorNameMap.put((Long) row[0], (String) row[1]);
            }
        }

        // 3) authorMap 생성
        Map<Long, AuthorDto> authorMap = new HashMap<>();
        for (Long uid : userIds) {
            UserProfile p = profileMap.get(uid);
            if (p == null) continue;

            String majorName = (p.getMajorId() == null)
                    ? "전공 미입력"
                    : majorNameMap.getOrDefault(p.getMajorId(), "알 수 없는 전공");

            String imgUrl = publicUrlIssuer.issuePublicUrl(p.getProfileImageKey()); // key -> CDN URL (null이면 null)

            authorMap.put(uid, new AuthorDto(
                    uid,
                    p.getUser().getName(),
                    imgUrl,
                    majorName,
                    p.getYearLevel()
            ));
        }
        return authorMap;
    }
}