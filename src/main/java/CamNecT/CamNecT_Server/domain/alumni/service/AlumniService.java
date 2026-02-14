package CamNecT.CamNecT_Server.domain.alumni.service;

import CamNecT.CamNecT_Server.domain.alumni.dto.AlumniHomeResponse;
import CamNecT.CamNecT_Server.domain.alumni.dto.ProfileCardDto;
import CamNecT.CamNecT_Server.domain.alumni.dto.response.AlumniPreviewResponse;
import CamNecT.CamNecT_Server.domain.alumni.dto.UserProfileDto;
import CamNecT.CamNecT_Server.domain.alumni.repository.AlumniRepository;
import CamNecT.CamNecT_Server.domain.home.dto.HomeResponse;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.domain.users.repository.UserTagMapRepository;
import CamNecT.CamNecT_Server.global.storage.service.PublicUrlIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(targetIds).stream()
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

                    UserProfileDto profileDto = UserProfileDto.from(profile)
                            .withProfileImageUrl(publicUrlIssuer.issuePublicUrl(profile.getProfileImageKey()));

                    return new AlumniPreviewResponse(
                            id,
                            user.getName(),
                            profileDto,
                            tagMap.getOrDefault(id, List.of())
                    );
                })
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

                    ProfileCardDto card = ProfileCardDto.from(p); // 홈에서만 우선 적용

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