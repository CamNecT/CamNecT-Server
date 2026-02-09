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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyBookmarkQueryServiceImpl implements MyBookmarkQueryService {

    private final PostsRepository postsRepository;
    private final ExternalActivityBookmarkRepository externalActivityBookmarkRepository;
    private final RecruitmentBookmarkRepository recruitmentBookmarkRepository;

    private final CommunityArchiveAssembler communityArchiveAssembler;
    private final ExternalArchiveAssembler externalArchiveAssembler;
    private final RecruitmentArchiveAssembler recruitmentArchiveAssembler;

    @Override
    public MyArchiveResponse getBookmarks(Long userId, MyArchiveResponse.Tab tab, MyArchiveResponse.Sort sort,
                                          Long cursorId, Long cursorValue, int size) {
        return switch (tab) {
            case COMMUNITY -> communityBookmarks(userId, sort, cursorId, cursorValue, size);
            case EXTERNAL -> externalBookmarks(userId, sort, cursorId, cursorValue, size);
            case RECRUITMENT -> recruitmentBookmarks(userId, sort, cursorId, cursorValue, size);
        };
    }

    private MyArchiveResponse communityBookmarks(Long userId, MyArchiveResponse.Sort sort,
                                                 Long cursorId, Long cursorValue, int size) {
        Pageable pageable = PageRequest.of(0, size);

        Slice<Posts> slice = (sort == MyArchiveResponse.Sort.LATEST)
                ? postsRepository.findBookmarkedPostsLatest(userId, PostStatus.PUBLISHED, cursorId, pageable)
                : postsRepository.findBookmarkedPostsRecommended(userId, PostStatus.PUBLISHED, cursorValue, cursorId, pageable);

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

    private MyArchiveResponse externalBookmarks(Long userId, MyArchiveResponse.Sort sort,
                                                Long cursorId, Long cursorValue, int size) {
        Pageable pageable = PageRequest.of(0, size);

        Slice<Object[]> slice = (sort == MyArchiveResponse.Sort.LATEST)
                ? externalActivityBookmarkRepository.findBookmarkedActivitiesLatestWithCounts(userId, cursorId, pageable)
                : externalActivityBookmarkRepository.findBookmarkedActivitiesRecommendedWithCounts(userId, cursorValue, cursorId, pageable);

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

    private MyArchiveResponse recruitmentBookmarks(Long userId, MyArchiveResponse.Sort sort,
                                                   Long cursorId, Long cursorValue, int size) {
        Pageable pageable = PageRequest.of(0, size);

        Slice<Object[]> slice = (sort == MyArchiveResponse.Sort.LATEST)
                ? recruitmentBookmarkRepository.findBookmarkedRecruitmentsLatestWithCounts(userId, cursorId, pageable)
                : recruitmentBookmarkRepository.findBookmarkedRecruitmentsRecommendedWithCounts(userId, cursorValue, cursorId, pageable);

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