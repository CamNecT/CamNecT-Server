package CamNecT.CamNecT_Server.domain.profile.components.archive.service;

import CamNecT.CamNecT_Server.domain.activity.repository.external_activity.ExternalActivityBookmarkRepository;
import CamNecT.CamNecT_Server.domain.activity.repository.recruitment.RecruitmentBookmarkRepository;
import CamNecT.CamNecT_Server.domain.community.model.Posts.Posts;
import CamNecT.CamNecT_Server.domain.community.model.enums.PostStatus;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostsRepository;
import CamNecT.CamNecT_Server.domain.profile.components.archive.dto.response.MyArchiveResponse;
import CamNecT.CamNecT_Server.domain.profile.components.archive.service.util.CommunityArchiveAssembler;
import CamNecT.CamNecT_Server.domain.profile.components.archive.service.util.ExternalArchiveAssembler;
import CamNecT.CamNecT_Server.domain.profile.components.archive.service.util.RecruitmentArchiveAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPostQueryServiceImpl implements MyPostQueryService {

    private final PostsRepository postsRepository;
    private final ExternalActivityBookmarkRepository externalActivityBookmarkRepository;
    private final RecruitmentBookmarkRepository recruitmentBookmarkRepository;

    private final CommunityArchiveAssembler communityArchiveAssembler;
    private final ExternalArchiveAssembler externalArchiveAssembler;
    private final RecruitmentArchiveAssembler recruitmentArchiveAssembler;

    @Override
    public MyArchiveResponse getMyPosts(Long userId, MyArchiveResponse.Tab tab, MyArchiveResponse.Sort sort,
                                        Long cursorId, Long cursorValue, int size) {
        return switch (tab) {
            case COMMUNITY -> myCommunityPosts(userId, sort, cursorId, cursorValue, size);
            case EXTERNAL -> myExternalPosts(userId, sort, cursorId, cursorValue, size);
            case RECRUITMENT -> myRecruitmentPosts(userId, sort, cursorId, cursorValue, size);
        };
    }

    private MyArchiveResponse myCommunityPosts(Long userId, MyArchiveResponse.Sort sort,
                                               Long cursorId, Long cursorValue, int size) {
        Pageable pageable = PageRequest.of(0, size);

        Slice<Posts> slice = (sort == MyArchiveResponse.Sort.LATEST)
                ? postsRepository.findMyPostsLatest(userId, PostStatus.PUBLISHED, cursorId, pageable)
                : postsRepository.findMyPostsRecommended(userId, PostStatus.PUBLISHED, cursorValue, cursorId, pageable);

        List<Posts> posts = slice.getContent();
        if (posts.isEmpty()) {
            return new MyArchiveResponse(MyArchiveResponse.Tab.COMMUNITY, sort, List.of(), slice.hasNext(), null, null);
        }

        var assembled = communityArchiveAssembler.assemble(posts);
        Posts last = posts.getLast();

        Long nextCursorValue = (sort == MyArchiveResponse.Sort.RECOMMENDED) ? assembled.nextHotScore() : null;

        return new MyArchiveResponse(
                MyArchiveResponse.Tab.COMMUNITY,
                sort,
                assembled.items(),
                slice.hasNext(),
                last.getId(),
                nextCursorValue
        );
    }

    private MyArchiveResponse myExternalPosts(Long userId, MyArchiveResponse.Sort sort,
                                              Long cursorId, Long cursorValue, int size) {
        Pageable pageable = PageRequest.of(0, size);

        Slice<Object[]> slice = (sort == MyArchiveResponse.Sort.LATEST)
                ? externalActivityBookmarkRepository.findMyActivitiesLatestWithCounts(userId, cursorId, pageable)
                : externalActivityBookmarkRepository.findMyActivitiesRecommendedWithCounts(userId, cursorValue, cursorId, pageable);

        List<Object[]> rows = slice.getContent();
        if (rows.isEmpty()) {
            return new MyArchiveResponse(MyArchiveResponse.Tab.EXTERNAL, sort, List.of(), slice.hasNext(), null, null);
        }

        var assembled = externalArchiveAssembler.assemble(rows);

        Long nextCursorValue = (sort == MyArchiveResponse.Sort.RECOMMENDED) ? assembled.lastCursorValue() : null;

        return new MyArchiveResponse(
                MyArchiveResponse.Tab.EXTERNAL,
                sort,
                assembled.items(),
                slice.hasNext(),
                assembled.lastId(),
                nextCursorValue
        );
    }

    private MyArchiveResponse myRecruitmentPosts(Long userId, MyArchiveResponse.Sort sort,
                                                 Long cursorId, Long cursorValue, int size) {
        Pageable pageable = PageRequest.of(0, size);

        Slice<Object[]> slice = (sort == MyArchiveResponse.Sort.LATEST)
                ? recruitmentBookmarkRepository.findMyRecruitmentsLatestWithCounts(userId, cursorId, pageable)
                : recruitmentBookmarkRepository.findMyRecruitmentsRecommendedWithCounts(userId, cursorValue, cursorId, pageable);

        List<Object[]> rows = slice.getContent();
        if (rows.isEmpty()) {
            return new MyArchiveResponse(MyArchiveResponse.Tab.RECRUITMENT, sort, List.of(), slice.hasNext(), null, null);
        }

        var assembled = recruitmentArchiveAssembler.assemble(rows);

        Long nextCursorValue = (sort == MyArchiveResponse.Sort.RECOMMENDED) ? assembled.lastCursorValue() : null;

        return new MyArchiveResponse(
                MyArchiveResponse.Tab.RECRUITMENT,
                sort,
                assembled.items(),
                slice.hasNext(),
                assembled.lastId(),
                nextCursorValue
        );
    }
}