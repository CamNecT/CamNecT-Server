package CamNecT.CamNecT_Server.domain.community.service;

import CamNecT.CamNecT_Server.domain.community.dto.response.PostListResponse;
import CamNecT.CamNecT_Server.domain.community.dto.response.PostSummaryResponse;
import CamNecT.CamNecT_Server.domain.community.model.Posts.PostStats;
import CamNecT.CamNecT_Server.domain.community.model.Posts.PostTags;
import CamNecT.CamNecT_Server.domain.community.model.Posts.Posts;
import CamNecT.CamNecT_Server.domain.community.model.enums.BoardCode;
import CamNecT.CamNecT_Server.domain.community.model.enums.PostStatus;
import CamNecT.CamNecT_Server.domain.community.repository.Comments.AcceptedCommentsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostStatsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostTagsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostsRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.ErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.CommunityErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryServiceImpl implements PostQueryService {

    private final PostsRepository postsRepository;
    private final PostStatsRepository postStatsRepository;
    private final PostTagsRepository postTagsRepository;
    private final AcceptedCommentsRepository acceptedCommentsRepository;

    @Override
    public PostListResponse getPosts(Tab tab, Sort sort, Long tagId, String keyword,
                                     Long cursorId, Long cursorValue, int size) {
        int limit = Math.min(Math.max(size, 1), 50);

        BoardCode code = toBoardCode(tab);
        String kw = normalizeKeyword(keyword);

        Long cv = cursorValue;
        if (cv == null && cursorId != null && sort != Sort.LATEST) {

            // cursorId 자체가 존재하는지 먼저 확인하고 싶으면(선택)
            if (!postsRepository.existsById(cursorId)) {
                throw new CustomException(CommunityErrorCode.INVALID_CURSOR);
            }

            PostStats ps = postStatsRepository.findByPost_Id(cursorId)
                    .orElseThrow(() -> new CustomException(CommunityErrorCode.POST_STATS_NOT_FOUND));

            cv = switch (sort) {
                case RECOMMENDED -> ps.getHotScore();
                case LIKE        -> ps.getLikeCount();
                case BOOKMARK    -> ps.getBookmarkCount();
                default -> throw new CustomException(ErrorCode.INTERNAL_ERROR); // 사실상 불가
            };
        }

        Slice<Posts> slice = switch (sort) {
            case LATEST -> postsRepository.findFeedLatestWithFilter(
                    PostStatus.PUBLISHED,
                    code,
                    tagId,
                    kw,
                    cursorId,
                    PageRequest.of(0, limit)
            );

            case RECOMMENDED -> postsRepository.findFeedRecommended(
                    PostStatus.PUBLISHED,
                    code,
                    tagId,
                    kw,
                    cv,        // cursorValue(hotScore)
                    cursorId,  // cursorId(postId)
                    PageRequest.of(0, limit)
            );

            case LIKE -> postsRepository.findFeedLikeDesc(
                    PostStatus.PUBLISHED,
                    code,
                    tagId,
                    kw,
                    cv,        // cursorValue(likeCount)
                    cursorId,
                    PageRequest.of(0, limit)
            );

            case BOOKMARK -> postsRepository.findFeedBookmarkDesc(
                    PostStatus.PUBLISHED,
                    code,
                    tagId,
                    kw,
                    cv,        // cursorValue(bookmarkCount)
                    cursorId,
                    PageRequest.of(0, limit)
            );
        };

        return mapToListResponse(slice, sort);
    }

    @Override
    public PostListResponse getPostsByTag(Long tagId, Long cursorValue, Long cursorId, int size) {
        int limit = Math.min(Math.max(size, 1), 50);

        Slice<Posts> slice = postsRepository.findFeedRecommended(
                PostStatus.PUBLISHED,
                null,          // board filter 없음
                tagId,
                null,          // keyword 없음
                cursorValue,
                cursorId,
                PageRequest.of(0, limit)
        );

        return mapToListResponse(slice, Sort.RECOMMENDED);
    }

    @Override
    public PostListResponse getWaitingQuestions(int size) {
        Slice<Posts> slice = postsRepository.findWaitingQuestions(
                PostStatus.PUBLISHED,
                BoardCode.QUESTION,
                PageRequest.of(0, size)
        );
        return mapToListResponse(slice, Sort.LATEST);
    }

    private PostListResponse mapToListResponse(Slice<Posts> slice, Sort sort) {
        int MAX_CONTENT = 80;

        List<Posts> posts = slice.getContent();
        if (posts.isEmpty()) return PostListResponse.of(List.of(), slice.hasNext(), null);

        List<Long> postIds = posts.stream().map(Posts::getId).toList();

        // stats bulk
        Map<Long, PostStats> statsMap = new HashMap<>();
        for (PostStats ps : postStatsRepository.findByPost_IdIn(postIds)) {
            statsMap.put(ps.getPost().getId(), ps);
        }

        // tags bulk
        Map<Long, List<String>> tagsMap = new HashMap<>();
        for (PostTags pt : postTagsRepository.findAllByPostIdsWithTag(postIds)) {
            Long postId = pt.getPost().getId();
            tagsMap.computeIfAbsent(postId, k -> new ArrayList<>()).add(pt.getTag().getName());
        }

        // accepted bulk
        Set<Long> acceptedPostIds = new HashSet<>(acceptedCommentsRepository.findAcceptedPostIds(postIds));

        List<PostSummaryResponse> items = new ArrayList<>(posts.size());
        for (Posts p : posts) {
            PostStats ps = statsMap.get(p.getId());

            long likeCount = ps == null ? 0 : ps.getLikeCount();
            long commentCount = ps == null ? 0 : ps.getCommentCount();
            long answerCount = ps == null ? 0 : ps.getRootCommentCount();     // 질문 탭 "답변"
            long bookmarkCount = ps == null ? 0 : ps.getBookmarkCount();

            String preview = makePreview(p.getContent(), MAX_CONTENT);

            items.add(new PostSummaryResponse(
                    p.getId(),
                    p.getBoard().getCode(),
                    p.getTitle(),
                    preview,
                    p.getCreatedAt(),
                    likeCount,
                    answerCount,
                    commentCount,
                    bookmarkCount,
                    acceptedPostIds.contains(p.getId()),
                    tagsMap.getOrDefault(p.getId(), List.of()),
                    null // thumbnailUrl: 첨부 썸네일 필요해지면 PostAttachmentsRepository로 보강
            ));
        }

        Posts last = posts.getLast();
        PostStats lastStats = statsMap.get(last.getId());

        Long nextCursorValue = switch (sort) {
            case LATEST -> null;
            case RECOMMENDED -> (lastStats == null) ? 0L : lastStats.getHotScore();
            case LIKE -> (lastStats == null) ? 0L : lastStats.getLikeCount();
            case BOOKMARK -> (lastStats == null) ? 0L : lastStats.getBookmarkCount();
        };

        return PostListResponse.of(items, slice.hasNext(), nextCursorValue);
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null) return null;
        String t = keyword.trim();
        return t.isBlank() ? null : t;
    }

    private static String makePreview(String content, int max) {
        if (content == null) return "";
        String t = content.trim();
        if (t.length() <= max) return t;
        return t.substring(0, max) + "...";
    }

    private static BoardCode toBoardCode(Tab tab) {
        return switch (tab) {
            case ALL -> null;
            case INFO -> BoardCode.INFO;
            case QUESTION -> BoardCode.QUESTION;
        };
    }
}
