package CamNecT.server.domain.community.service;

import CamNecT.server.domain.community.dto.AuthorDto;
import CamNecT.server.domain.profile.dto.ProfileGlobalDto;
import CamNecT.server.domain.users.repository.UserProfileRepository;
import CamNecT.server.global.storage.service.PublicUrlIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthorAssembler {

    private static final Pattern STUDENT_NUMBER_WITH_ENTRY_YEAR = Pattern.compile("((?:19|20)[0-9]{2})[0-9]*");

    private final UserProfileRepository userProfileRepository;
    private final PublicUrlIssuer publicUrlIssuer;

    public AuthorDto buildGuestAuthor(Long userId) {
        return userProfileRepository.findGlobalByUserId(userId)
                .map(profile -> {
                    String majorName = StringUtils.hasText(profile.majorName()) ? profile.majorName() : null;
                    String entryYear = null;
                    if (StringUtils.hasText(profile.studentNo())) {
                        var matcher = STUDENT_NUMBER_WITH_ENTRY_YEAR.matcher(profile.studentNo().strip());
                        if (matcher.matches()) {
                            entryYear = matcher.group(1);
                        }
                    }
                    return majorName == null && entryYear == null
                            ? null
                            : new AuthorDto(null, null, null, entryYear, majorName);
                })
                .orElse(null);
    }

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
