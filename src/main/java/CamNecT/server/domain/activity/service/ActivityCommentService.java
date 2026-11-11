package CamNecT.server.domain.activity.service;

import CamNecT.server.domain.activity.dto.response.ActivityCommentListResponse;
import CamNecT.server.domain.activity.model.comment.ActivityComment;
import CamNecT.server.domain.activity.model.comment.ActivityCommentLike;
import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.activity.repository.comment.ActivityCommentLikeRepository;
import CamNecT.server.domain.activity.repository.comment.ActivityCommentRepository;
import CamNecT.server.domain.activity.repository.external_activity.ExternalActivityRepository;
import CamNecT.server.domain.community.dto.AuthorDto;
import CamNecT.server.domain.community.dto.request.CreateCommentRequest;
import CamNecT.server.domain.community.dto.request.UpdateCommentRequest;
import CamNecT.server.domain.community.dto.response.*;
import CamNecT.server.domain.community.model.enums.CommentStatus;
import CamNecT.server.domain.community.service.AuthorAssembler;
import CamNecT.server.domain.users.model.UserRole;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.global.common.auth.AccountAccessGuard;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.ActivityErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.CommunityErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ActivityCommentService {
    private static final int REPLY_PREVIEW_SIZE = 20;
    private static final List<CommentStatus> VISIBLE_STATUSES = List.of(CommentStatus.PUBLISHED, CommentStatus.DELETED);
    private final ExternalActivityRepository activityRepository;
    private final ActivityCommentRepository commentsRepository;
    private final ActivityCommentLikeRepository likesRepository;
    private final AccountAccessGuard accountAccessGuard;
    private final AuthorAssembler authorAssembler;

    @Transactional
    public CreateCommentResponse create(Long userId, Long activityId, CreateCommentRequest req) {
        requireUser(userId);
        accountAccessGuard.requireAccessibleForUpdate(userId);
        ExternalActivity activity = requireActivity(activityId);
        ActivityComment parent = null;
        if (req.parentCommentId() != null) {
            parent = commentsRepository.findByIdForUpdate(req.parentCommentId())
                    .orElseThrow(() -> new CustomException(CommunityErrorCode.PARENT_COMMENT_NOT_FOUND));
            if (!Objects.equals(parent.getActivity().getActivityId(), activityId))
                throw new CustomException(CommunityErrorCode.PARENT_COMMENT_NOT_IN_POST);
            if (parent.getStatus() == CommentStatus.HIDDEN)
                throw new CustomException(CommunityErrorCode.CANNOT_REPLY_TO_HIDDEN_COMMENT);
            if (parent.getParent() != null)
                throw new CustomException(CommunityErrorCode.COMMENT_MAX_DEPTH_EXCEEDED);
        }
        return new CreateCommentResponse(commentsRepository.save(ActivityComment.create(activity, userId, parent, req.content())).getId());
    }

    @Transactional
    public void update(Long userId, Long commentId, UpdateCommentRequest req) {
        requireUser(userId);
        accountAccessGuard.requireAccessibleForUpdate(userId);
        ActivityComment comment = requireCommentForUpdate(commentId);
        requirePublished(comment);
        if (!Objects.equals(comment.getUserId(), userId)) throw new CustomException(CommunityErrorCode.COMMENT_FORBIDDEN);
        comment.update(req.content());
    }

    @Transactional
    public void delete(Long userId, Long commentId) {
        requireUser(userId);
        Users actor = accountAccessGuard.requireAccessibleForUpdate(userId);
        ActivityComment comment = requireCommentForUpdate(commentId);
        requirePublished(comment);
        if (!Objects.equals(comment.getUserId(), userId) && actor.getRole() != UserRole.ADMIN)
            throw new CustomException(CommunityErrorCode.COMMENT_FORBIDDEN);
        comment.deleteSoft();
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ToggleCommentLikeResponse toggleLike(Long userId, Long commentId) {
        requireUser(userId);
        accountAccessGuard.requireAccessibleForUpdate(userId);
        ActivityComment comment = requireCommentForUpdate(commentId);
        requirePublished(comment);
        boolean liked = !likesRepository.existsByComment_IdAndUserId(commentId, userId);
        if (liked) likesRepository.save(ActivityCommentLike.of(comment, userId));
        else likesRepository.deleteByComment_IdAndUserId(commentId, userId);
        return new ToggleCommentLikeResponse(liked, likesRepository.countByComment_Id(commentId));
    }

    @Transactional(readOnly = true)
    public ActivityCommentListResponse list(Long userId, Long activityId, Long cursorId, int size) {
        requireUser(userId);
        requireActivity(activityId);
        if (cursorId != null && cursorId <= 0) throw new CustomException(CommunityErrorCode.INVALID_CURSOR);
        int limit = Math.clamp(size, 1, 50);
        List<CommentStatus> statuses = VISIBLE_STATUSES;
        List<ActivityComment> fetched = commentsRepository.findRootPage(activityId, statuses, cursorId, PageRequest.of(0, limit + 1));
        boolean hasNext = fetched.size() > limit;
        List<ActivityComment> roots = hasNext ? fetched.subList(0, limit) : fetched;
        Long nextCursor = hasNext ? roots.getLast().getId() : null;
        List<Long> rootIds = roots.stream().map(ActivityComment::getId).toList();
        List<ActivityComment> children = rootIds.isEmpty() ? new ArrayList<>() : new ArrayList<>(commentsRepository
                .findReplyPreviews(activityId, rootIds, statuses.stream().map(Enum::name).toList(), REPLY_PREVIEW_SIZE + 1));
        children.sort(Comparator.comparing((ActivityComment c) -> c.getParent().getId())
                .thenComparing(ActivityComment::getCreatedAt).thenComparing(ActivityComment::getId));
        Map<Long, List<ActivityComment>> childrenByParent = new HashMap<>();
        children.forEach(c -> childrenByParent.computeIfAbsent(c.getParent().getId(), ignored -> new ArrayList<>()).add(c));
        List<ActivityComment> visibleChildren = childrenByParent.values().stream()
                .flatMap(replies -> replies.stream().limit(REPLY_PREVIEW_SIZE)).toList();
        List<ActivityComment> all = Stream.concat(roots.stream(), visibleChildren.stream()).toList();
        Map<Long, AuthorDto> authors = authorAssembler.buildAuthorMap(all.stream().map(ActivityComment::getUserId).distinct().toList());
        Map<Long, Long> counts = new HashMap<>();
        if (!all.isEmpty()) likesRepository.countByCommentIds(all.stream().map(ActivityComment::getId).toList())
                .forEach(row -> counts.put(row.getCommentId(), row.getCnt()));
        List<CommentItemResponse> items = new ArrayList<>();
        List<ActivityCommentListResponse.ReplyPage> replyPages = new ArrayList<>();
        for (ActivityComment root : roots) {
            items.add(toRow(root, counts, authors));
            List<ActivityComment> replies = childrenByParent.getOrDefault(root.getId(), List.of());
            boolean hasMoreReplies = replies.size() > REPLY_PREVIEW_SIZE;
            List<ActivityComment> preview = replies.subList(0, Math.min(replies.size(), REPLY_PREVIEW_SIZE));
            for (ActivityComment child : preview) items.add(toRow(child, counts, authors));
            replyPages.add(new ActivityCommentListResponse.ReplyPage(root.getId(),
                    hasMoreReplies ? preview.getLast().getId() : null, hasMoreReplies));
        }
        return new ActivityCommentListResponse(items, nextCursor, hasNext, replyPages);
    }

    @Transactional(readOnly = true)
    public CommentListResponse listReplies(Long userId, Long activityId, Long parentId, Long cursorId, int size) {
        requireUser(userId);
        requireActivity(activityId);
        ActivityComment parent = commentsRepository.findById(parentId)
                .filter(c -> c.getActivity().getActivityId().equals(activityId)
                        && c.getParent() == null && VISIBLE_STATUSES.contains(c.getStatus()))
                .orElseThrow(() -> new CustomException(CommunityErrorCode.COMMENT_NOT_FOUND));
        ActivityComment cursor = null;
        if (cursorId != null) {
            cursor = commentsRepository.findById(cursorId)
                    .filter(c -> c.getActivity().getActivityId().equals(activityId)
                            && c.getParent() != null && c.getParent().getId().equals(parent.getId()))
                    .orElseThrow(() -> new CustomException(CommunityErrorCode.INVALID_CURSOR));
        }
        int limit = Math.clamp(size, 1, 50);
        List<ActivityComment> fetched = commentsRepository.findReplyPage(activityId, parentId, VISIBLE_STATUSES,
                cursorId, cursor == null ? null : cursor.getCreatedAt(), PageRequest.of(0, limit + 1));
        boolean hasNext = fetched.size() > limit;
        List<ActivityComment> replies = hasNext ? fetched.subList(0, limit) : fetched;
        Map<Long, AuthorDto> authors = authorAssembler.buildAuthorMap(replies.stream().map(ActivityComment::getUserId).distinct().toList());
        Map<Long, Long> counts = new HashMap<>();
        if (!replies.isEmpty()) likesRepository.countByCommentIds(replies.stream().map(ActivityComment::getId).toList())
                .forEach(row -> counts.put(row.getCommentId(), row.getCnt()));
        return new CommentListResponse(replies.stream().map(c -> toRow(c, counts, authors)).toList(),
                hasNext ? replies.getLast().getId() : null, hasNext);
    }

    private CommentItemResponse toRow(ActivityComment c, Map<Long, Long> counts, Map<Long, AuthorDto> authors) {
        boolean deleted = c.getStatus().isDeleted();
        return new CommentItemResponse(c.getId(), c.getUserId(), c.getParent() == null ? null : c.getParent().getId(),
                deleted ? "삭제된 댓글입니다." : c.getContent(), counts.getOrDefault(c.getId(), 0L), c.getCreatedAt(),
                deleted ? null : authors.get(c.getUserId()));
    }

    private ExternalActivity requireActivity(Long activityId) {
        ExternalActivity activity = activityRepository.findByIdForRead(activityId)
                .orElseThrow(() -> new CustomException(ActivityErrorCode.ACTIVITY_NOT_FOUND));
        if (activity.getCategory() != ActivityCategory.CLUB && activity.getCategory() != ActivityCategory.STUDY)
            throw new CustomException(ActivityErrorCode.INVALID_ACTIVITY_CATEGORY);
        return activity;
    }

    private ActivityComment requireCommentForUpdate(Long commentId) {
        Long activityId = commentsRepository.findActivityIdByCommentId(commentId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.COMMENT_NOT_FOUND));
        requireActivity(activityId);
        return commentsRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.COMMENT_NOT_FOUND));
    }

    private void requirePublished(ActivityComment comment) {
        if (!comment.getStatus().isPublished()) throw new CustomException(CommunityErrorCode.COMMENT_NOT_PUBLISHED);
    }

    private void requireUser(Long userId) {
        if (userId == null) throw new CustomException(AuthErrorCode.INVALID_TOKEN);
    }
}
