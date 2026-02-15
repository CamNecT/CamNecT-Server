package CamNecT.server.domain.alumni.service;

import CamNecT.server.domain.alumni.dto.AlumniHomeResponse;
import CamNecT.server.domain.alumni.dto.ProfileCardDto;
import CamNecT.server.domain.alumni.dto.response.AlumniPreviewResponse;
import CamNecT.server.domain.alumni.dto.UserProfileDto;
import CamNecT.server.domain.alumni.repository.AlumniRepository;
import CamNecT.server.domain.home.dto.HomeResponse;
import CamNecT.server.domain.users.model.UserProfile;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserProfileRepository;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.domain.users.repository.UserTagMapRepository;
import CamNecT.server.global.storage.service.PublicUrlIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlumniService {

    private final UserTagMapRepository userTagMapRepository;
    private final UserProfileRepository userProfileRepository;
    private final AlumniRepository alumniRepository;
    private final UserRepository usersRepository;
    private final PublicUrlIssuer publicUrlIssuer;

    @Transactional(readOnly = true)
    public Slice<AlumniPreviewResponse> searchAlumni(Long userId, String name, List<Long> tagIdList, Pageable pageable) {

        // 1. ID 페이징 조회
        List<Long> targetIds = alumniRepository.findAlumniIdsByConditions(userId, name, tagIdList, pageable);

        boolean hasNext = targetIds.size() > pageable.getPageSize();
        if (hasNext) {
            targetIds = targetIds.subList(0, pageable.getPageSize());
        }

        if (targetIds.isEmpty()) {
            return new SliceImpl<>(List.of(), pageable, false);
        }

        // 2. Users 조회
        Map<Long, Users> usersMap = usersRepository.findAllById(targetIds).stream()
                .collect(Collectors.toMap(Users::getUserId, u -> u));

        // 3. Profile 조회
        Map<Long, UserProfile> profileMap = userProfileRepository.findAllByUserIdIn(targetIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        // 4. Tags 조회
        Map<Long, List<String>> tagMap = userTagMapRepository.findTagNamesWithUserIdByUserIdIn(targetIds)
                .stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())
                ));

        // 5. DTO 변환 (정렬 유지)
        List<AlumniPreviewResponse> content = targetIds.stream()
                .map(id -> {
                    Users user = usersMap.get(id);
                    UserProfile profile = profileMap.get(id);
                    if (user == null || profile == null) return null;

                    String imgUrl = null;
                    if (StringUtils.hasText(profile.getProfileImageKey())) {
                        imgUrl = publicUrlIssuer.issuePublicUrl(profile.getProfileImageKey());
                    }

                    UserProfileDto dto = UserProfileDto.from(profile).withProfileImageUrl(imgUrl);

                    return new AlumniPreviewResponse(id, user.getName(), dto, tagMap.getOrDefault(id, List.of()));
                })
                .filter(Objects::nonNull)
                .toList();

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Transactional(readOnly = true)
    public HomeResponse.AlumniSection getHomePreview(Long myId, int limit) {

        Pageable defaultPageable = PageRequest.of(0, 20);

        // 1) 추천 정렬된 ID 목록
        List<Long> orderedIds = alumniRepository.findAlumniIdsByConditions(myId, null, List.of(), defaultPageable);
        if (orderedIds.isEmpty()) {
            return HomeResponse.AlumniSection.empty();
        }

        boolean hasMore = orderedIds.size() > limit;
        List<Long> topIds = orderedIds.stream().limit(limit).toList();

        // 2) 프로필 + 유저(이름) 한 번에
        Map<Long, UserProfile> profileMap = userProfileRepository.findAllByUserIdInWithUser(topIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        Map<Long, List<String>> tagMap =
                userTagMapRepository.findTagNamesWithUserIdByUserIdIn(topIds).stream()
                        .collect(Collectors.groupingBy(
                                row -> (Long) row[0],
                                Collectors.mapping(row -> (String) row[1], Collectors.toList())
                        ));


        // 4) 정렬 유지하면서 AlumniHomeResponse 생성
        List<AlumniHomeResponse> items = topIds.stream()
                .map(id -> {
                    UserProfile p = profileMap.get(id);
                    if (p == null || p.getUser() == null) return null;
                    String imgUrl = null;
                    if (StringUtils.hasText(p.getProfileImageKey())) {
                        imgUrl = publicUrlIssuer.issuePublicUrl(p.getProfileImageKey());
                    }

                    ProfileCardDto card = ProfileCardDto.createCard(p,imgUrl); // 홈에서만 우선 적용

                    return new AlumniHomeResponse(
                            id,
                            p.getUser().getName(),
                            card,
                            tagMap.getOrDefault(id, List.of())
                    );
                })
                .filter(Objects::nonNull)
                .toList();

        return new HomeResponse.AlumniSection(items, hasMore);
    }


}