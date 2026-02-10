package CamNecT.CamNecT_Server.domain.profile.components.archive.service.util;

import CamNecT.CamNecT_Server.domain.community.model.Posts.PostStats;
import CamNecT.CamNecT_Server.domain.community.model.Posts.Posts;
import CamNecT.CamNecT_Server.domain.community.model.enums.PostAccessType;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostAttachmentsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostStatsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostTagsRepository;
import CamNecT.CamNecT_Server.domain.profile.components.archive.dto.response.MyArchiveResponse;
import CamNecT.CamNecT_Server.domain.profile.components.majors.model.Majors;
import CamNecT.CamNecT_Server.domain.profile.components.majors.repository.MajorRepository;
import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import CamNecT.CamNecT_Server.domain.users.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityArchiveAssembler {

    private static final int MAX_CONTENT = 80;

    private final PostStatsRepository postStatsRepository;
    private final PostTagsRepository postTagsRepository;
    private final PostAttachmentsRepository postAttachmentsRepository;

    private final UserProfileRepository userProfileRepository;
    private final MajorRepository majorRepository;

    private final ArchiveUtils archiveUtils;

    public CommunityAssembleResult assemble(List<Posts> posts) {
        if (posts == null || posts.isEmpty()) {
            return new CommunityAssembleResult(List.of(), null);
        }

        List<Long> postIds = posts.stream().map(Posts::getId).toList();

        // stats bulk
        Map<Long, PostStats> statsMap = postStatsRepository.findByPost_IdIn(postIds).stream()
                .collect(Collectors.toMap(ps -> ps.getPost().getId(), ps -> ps));

        // tags bulk
        Map<Long, List<String>> tagsMap = new HashMap<>();
        postTagsRepository.findAllByPostIdsWithTag(postIds).forEach(pt -> {
            Long pid = pt.getPost().getId();
            tagsMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(pt.getTag().getName());
        });

        // thumbnail key bulk
        Map<Long, String> thumbKeyMap = new HashMap<>();
        postAttachmentsRepository.findThumbCandidates(postIds).forEach(a -> {
            Long pid = a.getPost().getId();
            if (!thumbKeyMap.containsKey(pid) && StringUtils.hasText(a.getFileKey())) {
                thumbKeyMap.put(pid, a.getFileKey());
            }
        });

        // author bulk (profile + major)
        List<Long> authorIds = posts.stream()
                .map(p -> p.getUser().getUserId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        Set<Long> majorIds = profileMap.values().stream()
                .map(UserProfile::getMajorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> majorNameMap = majorRepository.findAllById(majorIds).stream()
                .collect(Collectors.toMap(Majors::getMajorId, Majors::getMajorNameKor));

        // build items
        List<MyArchiveResponse.Item> items = new ArrayList<>(posts.size());
        for (Posts p : posts) {
            PostStats ps = statsMap.get(p.getId());

            long bookmarkCount = (ps == null) ? 0 : ps.getBookmarkCount();
            long commentCount = (ps == null) ? 0 : ps.getCommentCount();

            String preview = archiveUtils.makePreview(p.getContent(), MAX_CONTENT);

            String thumbUrl = null;
            String thumbKey = thumbKeyMap.get(p.getId());
            if (p.getAccessType() != PostAccessType.POINT_REQUIRED) {
                thumbUrl = archiveUtils.thumbnailUrlOrNull(thumbKey);
            }

            UserProfile prof = profileMap.get(p.getUser().getUserId());
            String majorName = null;
            Integer yearLevel = null;
            if (prof != null) {
                if (prof.getMajorId() != null) majorName = majorNameMap.get(prof.getMajorId());
                yearLevel = prof.getYearLevel();
            }

            items.add(new MyArchiveResponse.CommunityItem(
                    p.getId(),
                    p.getBoard().getCode().name(),
                    tagsMap.getOrDefault(p.getId(), List.of()),
                    new MyArchiveResponse.Author(
                            p.getUser().getUserId(),
                            p.getUser().getName(),
                            majorName,
                            yearLevel
                    ),
                    p.getTitle(),
                    preview,
                    bookmarkCount,
                    commentCount,
                    p.getCreatedAt(),
                    thumbUrl
            ));
        }

        // recommended cursorValue = hotScore of last
        Posts last = posts.getLast();
        PostStats lastStats = statsMap.get(last.getId());
        Long nextHotScore = (lastStats == null) ? 0L : lastStats.getHotScore();

        return new CommunityAssembleResult(items, nextHotScore);
    }

    public record CommunityAssembleResult(
            List<MyArchiveResponse.Item> items,
            Long nextHotScore
    ) {}
}