package CamNecT.CamNecT_Server.domain.community.service;

import CamNecT.CamNecT_Server.domain.community.dto.AuthorDto;
import CamNecT.CamNecT_Server.domain.community.dto.response.PostListResponse;
import CamNecT.CamNecT_Server.domain.community.dto.response.PostSummaryResponse;
import CamNecT.CamNecT_Server.domain.community.model.Posts.PostAttachments;
import CamNecT.CamNecT_Server.domain.community.model.Posts.PostStats;
import CamNecT.CamNecT_Server.domain.community.model.Posts.PostTags;
import CamNecT.CamNecT_Server.domain.community.model.Posts.Posts;
import CamNecT.CamNecT_Server.domain.community.model.enums.BoardCode;
import CamNecT.CamNecT_Server.domain.community.model.enums.PostAccessType;
import CamNecT.CamNecT_Server.domain.community.model.enums.PostStatus;
import CamNecT.CamNecT_Server.domain.community.repository.Comments.AcceptedCommentsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostAttachmentsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostStatsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostTagsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostsRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.ErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.CommunityErrorCode;
import CamNecT.CamNecT_Server.global.storage.service.PublicUrlIssuer;
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
    private static final Set<String> THUMB_EXT = Set.of(".jpg", ".jpeg", ".png", ".webp");

    private final PostsRepository postsRepository;
    private final PostStatsRepository postStatsRepository;
    private final PostAttachmentsRepository postAttachmentsRepository;
    private final PostTagsRepository postTagsRepository;
    private final AcceptedCommentsRepository acceptedCommentsRepository;
    private final PublicUrlIssuer  publicUrlIssuer;
    private final AuthorAssembler  authorAssembler;

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
                    PostStatus.PUBLISHED, code, tagId, kw, cursorId, PageRequest.of(0, limit)
            );
            case RECOMMENDED -> postsRepository.findFeedRecommended(
                    PostStatus.PUBLISHED, code, tagId, kw, cv, cursorId, PageRequest.of(0, limit)
            );
            case LIKE -> postsRepository.findFeedLikeDesc(
                    PostStatus.PUBLISHED, code, tagId, kw, cv, cursorId, PageRequest.of(0, limit)
            );
            case BOOKMARK -> postsRepository.findFeedBookmarkDesc(
                    PostStatus.PUBLISHED, code, tagId, kw, cv, cursorId, PageRequest.of(0, limit)
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

        // thumbnail bulk (sortOrder=0)
        Map<Long, String> thumbKeyMap = new HashMap<>();
        for (PostAttachments a : postAttachmentsRepository.findThumbCandidates(postIds)) {
            Long pid = a.getPost().getId();
            if (!thumbKeyMap.containsKey(pid) && hasText(a.getFileKey())) {
                thumbKeyMap.put(pid, a.getFileKey());
            }
        }
        List<PostSummaryResponse> items = new ArrayList<>(posts.size());

        List<Long> authorIds = posts.stream()
                .map(p -> p.getUser().getUserId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, AuthorDto> authorMap = authorAssembler.buildAuthorMap(authorIds);

        for (Posts p : posts) {
            PostStats ps = statsMap.get(p.getId());

            long likeCount = ps == null ? 0 : ps.getLikeCount();
            long commentCount = ps == null ? 0 : ps.getCommentCount();
            long answerCount = ps == null ? 0 : ps.getRootCommentCount();
            long bookmarkCount = ps == null ? 0 : ps.getBookmarkCount();

            String preview = makePreview(p.getContent(), MAX_CONTENT);

            String thumbUrl = null;
            String thumbKey = thumbKeyMap.get(p.getId());
            // paywall 글은 CDN 썸네일 null (보안)
            if (p.getAccessType() != PostAccessType.POINT_REQUIRED) {
                thumbUrl = thumbnailUrlOrNull(thumbKey);
            }
            AuthorDto author = authorMap.get(p.getUser().getUserId());

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
                    author,
                    thumbUrl,
                    p.getAccessType()
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

    private String thumbnailUrlOrNull(String key) {
        if (!hasText(key)) return null;

        String lower = key.toLowerCase(Locale.ROOT);
        boolean isImage = THUMB_EXT.stream().anyMatch(lower::endsWith);
        if (!isImage) return null; // 0번이 pdf면 썸네일 없음

        return publicUrlIssuer.issuePublicUrl(key); // CDN 대상 prefix 아니면 null 가능
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

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
