package CamNecT.CamNecT_Server.domain.community.service;

import CamNecT.CamNecT_Server.domain.community.dto.request.CreateCommentRequest;
import CamNecT.CamNecT_Server.domain.community.dto.request.UpdateCommentRequest;
import CamNecT.CamNecT_Server.domain.community.dto.response.CreateCommentResponse;
import CamNecT.CamNecT_Server.domain.community.dto.response.ToggleCommentLikeResponse;
import CamNecT.CamNecT_Server.domain.community.model.Comments.CommentLikes;
import CamNecT.CamNecT_Server.domain.community.model.Comments.Comments;
import CamNecT.CamNecT_Server.domain.community.model.Posts.PostStats;
import CamNecT.CamNecT_Server.domain.community.model.Posts.Posts;
import CamNecT.CamNecT_Server.domain.community.model.enums.CommentStatus;
import CamNecT.CamNecT_Server.domain.community.repository.Comments.CommentLikesRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Comments.CommentsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostStatsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.PostsRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.CommunityErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final PostsRepository postsRepository;
    private final CommentsRepository commentsRepository;
    private final PostStatsRepository postStatsRepository;
    private final CommentLikesRepository commentLikesRepository;

    @Transactional
    @Override
    public CreateCommentResponse create(Long userId, Long postId, CreateCommentRequest req) {
        if (userId == null) throw new CustomException(AuthErrorCode.USER_NOT_FOUND);

        Posts post = postsRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.POST_NOT_FOUND));

        Comments parent = null;
        if (req.parentCommentId() != null) {
            parent = commentsRepository.findById(req.parentCommentId())
                    .orElseThrow(() -> new CustomException(CommunityErrorCode.PARENT_COMMENT_NOT_FOUND));

            if (!Objects.equals(parent.getPost().getId(), postId)) {
                throw new CustomException(CommunityErrorCode.PARENT_COMMENT_NOT_IN_POST);
            }

            // 부모가 삭제/숨김이면 자식 댓글 추가 불가
            if (parent.getStatus() == CommentStatus.DELETED || parent.getStatus() == CommentStatus.HIDDEN) {
                throw new CustomException(CommunityErrorCode.CANNOT_REPLY_TO_DELETED_OR_HIDDEN);
            }

            // 2뎁스 제한: 부모가 이미 대댓글이면 금지
            if (parent.getParent() != null) {
                throw new CustomException(CommunityErrorCode.COMMENT_MAX_DEPTH_EXCEEDED);
            }
        }

        Comments saved = commentsRepository.save(Comments.create(post, userId, parent, req.content()));

        PostStats stats = postStatsRepository.findByPost_Id(postId)
                .orElseGet(() -> postStatsRepository.save(PostStats.init(post)));

        stats.incComment();               // 전체 댓글 수 +1
        if (parent == null) {
            stats.incRootComment();       // 루트 댓글 수(=답변 수) +1
        }
        stats.touch();

        return new CreateCommentResponse(saved.getId());
    }

    @Transactional
    @Override
    public void update(Long userId, Long commentId, UpdateCommentRequest req) {
        if (userId == null) throw new CustomException(AuthErrorCode.USER_NOT_FOUND);

        Comments comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.COMMENT_NOT_FOUND));

        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new CustomException(CommunityErrorCode.COMMENT_FORBIDDEN);
        }

        comment.update(req.content());
        postStatsRepository.findByPost_Id(comment.getPost().getId()).ifPresent(PostStats::touch);
    }

    @Transactional
    @Override
    public void delete(Long userId, Long commentId) {
        if (userId == null) throw new CustomException(AuthErrorCode.USER_NOT_FOUND);

        Comments comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.COMMENT_NOT_FOUND));

        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new CustomException(CommunityErrorCode.COMMENT_FORBIDDEN);
        }

        // 중복 삭제 방지 (카운트 두 번 깎이는 거 방지)
        if (comment.getStatus() == CommentStatus.DELETED) return;

        boolean isRoot = (comment.getParent() == null);

        comment.deleteSoft();

        PostStats stats = postStatsRepository.findByPost_Id(comment.getPost().getId())
                .orElseGet(() -> postStatsRepository.save(PostStats.init(comment.getPost())));

        stats.decComment();
        if (isRoot) stats.decRootComment();
        stats.touch();
    }

    @Transactional
    @Override
    public ToggleCommentLikeResponse toggleLike(Long userId, Long commentId) {
        if (userId == null) throw new CustomException(AuthErrorCode.USER_NOT_FOUND);

        Comments comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.COMMENT_NOT_FOUND));

        boolean liked;
        if (commentLikesRepository.existsByComment_IdAndUserId(commentId, userId)) {
            commentLikesRepository.deleteByComment_IdAndUserId(commentId, userId);
            liked = false;
        } else {
            commentLikesRepository.save(CommentLikes.of(comment, userId));
            liked = true;
        }

        long likeCount = commentLikesRepository.countByComment_Id(commentId);

        // (선택) 댓글에 추천이 찍히면 게시글도 “활동”으로 취급하고 싶을 때
        postStatsRepository.findByPost_Id(comment.getPost().getId()).ifPresent(PostStats::touch);

        return new ToggleCommentLikeResponse(liked, likeCount);
    }

    @Transactional(readOnly = true)
    @Override
    public List<CommentRow> list(Long postId, int size) {
        if (!postsRepository.existsById(postId)) {
            throw new CustomException(CommunityErrorCode.POST_NOT_FOUND);
        }

        int limit = Math.min(Math.max(size, 1), 50);
        var pageable = PageRequest.of(0, limit);

        List<Comments> roots = new ArrayList<>();
        roots.addAll(commentsRepository.findByPost_IdAndParentIsNullAndStatusOrderByCreatedAtDesc(postId, CommentStatus.PUBLISHED, pageable));
        roots.addAll(commentsRepository.findByPost_IdAndParentIsNullAndStatusOrderByCreatedAtDesc(postId, CommentStatus.DELETED, pageable));

        // created_at desc로 정렬 후 limit
        roots.sort(Comparator.comparing(Comments::getCreatedAt).reversed());
        if (roots.size() > limit) roots = roots.subList(0, limit);

        List<Long> rootIds = roots.stream().map(Comments::getId).toList();

        // 자식 댓글: 부모 아래 created_at asc
        List<Comments> children = rootIds.isEmpty() ? List.of() : mergeChildren(postId, rootIds);

        // 좋아요 수 배치 집계 (옵션 쿼리 사용)
        Map<Long, Long> likeMap = buildLikeCountMap(roots, children);

        // flat list 구성: 루트 -> 자식 순서로 내려줌
        List<CommentRow> out = new ArrayList<>();

        // parentId -> children list
        Map<Long, List<Comments>> childMap = new LinkedHashMap<>();
        for (Comments ch : children) {
            childMap.computeIfAbsent(ch.getParent().getId(), k -> new ArrayList<>()).add(ch);
        }

        for (Comments r : roots) {
            out.add(toRow(r, likeMap.getOrDefault(r.getId(), 0L)));

            List<Comments> kids = childMap.getOrDefault(r.getId(), List.of());
            for (Comments k : kids) {
                out.add(toRow(k, likeMap.getOrDefault(k.getId(), 0L)));
            }
        }

        return out;
    }

    private List<Comments> mergeChildren(Long postId, List<Long> rootIds) {
        List<Comments> children = new ArrayList<>();
        children.addAll(commentsRepository.findByPost_IdAndParent_IdInAndStatusOrderByParent_IdAscCreatedAtAsc(
                postId, rootIds, CommentStatus.PUBLISHED
        ));
        children.addAll(commentsRepository.findByPost_IdAndParent_IdInAndStatusOrderByParent_IdAscCreatedAtAsc(
                postId, rootIds, CommentStatus.DELETED
        ));
        // parent_id asc, created_at asc 유지되도록 한 번 더 안정 정렬
        children.sort(Comparator
                .comparing((Comments c) -> c.getParent().getId())
                .thenComparing(Comments::getCreatedAt)
                .thenComparing(Comments::getId));
        return children;
    }

    private Map<Long, Long> buildLikeCountMap(List<Comments> roots, List<Comments> children) {
        List<Long> ids = new ArrayList<>(roots.size() + children.size());
        for (Comments c : roots) ids.add(c.getId());
        for (Comments c : children) ids.add(c.getId());
        if (ids.isEmpty()) return Map.of();

        Map<Long, Long> map = new HashMap<>();
        for (CommentLikesRepository.LikeCountRow row : commentLikesRepository.countByCommentIds(ids)) {
            map.put(row.getCommentId(), row.getCnt());
        }
        return map;
    }

    private CommentRow toRow(Comments c, long likeCount) {
        String content = (c.getStatus() == CommentStatus.DELETED) ? "삭제된 댓글입니다." : c.getContent();
        Long parentId = (c.getParent() == null) ? null : c.getParent().getId();

        return new CommentRow(
                c.getId(),
                c.getUserId(),
                parentId,
                content,
                likeCount
        );
    }
}
