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
    public List<AlumniPreviewResponse> searchAlumni(Long userId, String name, List<Long> tagIdList) {

        // 1. 조건에 맞는 유저 ID 목록 조회 (QueryDSL 동적 쿼리 사용)
        List<Long> targetIds = alumniRepository.findAlumniIdsByConditions(userId, name, tagIdList);

        if (targetIds.isEmpty()) return List.of();

        // 2-1. Users 정보 조회 및 Map 변환 (userName 조회용)
        Map<Long, Users> usersMap = usersRepository.findAllById(targetIds).stream()
                .collect(Collectors.toMap(Users::getUserId, u -> u));

        // 2-2. 프로필 정보 조회 및 Map 변환
        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(targetIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        // 3. 태그 정보 조회 및 Map 변환 (통합 로직)
        List<Object[]> tagResults = userTagMapRepository.findTagNamesWithUserIdByUserIdIn(targetIds);
        Map<Long, List<String>> tagMap = tagResults.stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0], // userId
                        Collectors.mapping(
                                row -> (String) row[1], // tag name
                                Collectors.toList()
                        )
                ));

        // 4. targetIds의 정렬 순서를 유지하며 최종 DTO 생성 (프로필 이미지 CDN URL 적용)
        return targetIds.stream()
                .map(id -> {
                    Users user = usersMap.get(id);
                    UserProfile profile = profileMap.get(id);

                    // UserProfile 엔티티 → DTO 변환 + 프로필 이미지 CDN URL 적용
                    UserProfileDto profileDto = UserProfileDto.from(profile)
                            .withProfileImageUrl(
                                    publicUrlIssuer.issuePublicUrl(profile.getProfileImageKey())
                            );

                    return new AlumniPreviewResponse(
                            id,
                            user.getName(), // Users 엔티티에서 name 가져오기
                            profileDto,
                            tagMap.getOrDefault(id, List.of())
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public HomeResponse.AlumniSection getHomePreview(Long myId, int limit) {

        // 1) 추천 정렬된 ID 목록
        List<Long> orderedIds = alumniRepository.findAlumniIdsByConditions(myId, null, List.of());
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