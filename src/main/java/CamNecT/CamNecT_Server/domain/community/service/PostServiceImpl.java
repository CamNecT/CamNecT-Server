package CamNecT.CamNecT_Server.domain.community.service;

import CamNecT.CamNecT_Server.domain.community.dto.AuthorDto;
import CamNecT.CamNecT_Server.domain.community.dto.request.CreatePostRequest;
import CamNecT.CamNecT_Server.domain.community.dto.request.UpdatePostRequest;
import CamNecT.CamNecT_Server.domain.community.dto.response.*;
import CamNecT.CamNecT_Server.domain.community.event.CommentAcceptedEvent;
import CamNecT.CamNecT_Server.domain.community.model.*;
import CamNecT.CamNecT_Server.domain.community.model.Comments.AcceptedComments;
import CamNecT.CamNecT_Server.domain.community.model.Comments.Comments;
import CamNecT.CamNecT_Server.domain.community.model.Posts.*;
import CamNecT.CamNecT_Server.domain.community.model.enums.*;
import CamNecT.CamNecT_Server.domain.community.repository.*;
import CamNecT.CamNecT_Server.domain.community.repository.Comments.AcceptedCommentsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Comments.CommentLikesRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Comments.CommentsRepository;
import CamNecT.CamNecT_Server.domain.community.repository.Posts.*;
import CamNecT.CamNecT_Server.domain.point.model.PointEvent;
import CamNecT.CamNecT_Server.domain.point.model.TransactionType;
import CamNecT.CamNecT_Server.domain.point.service.PointService;
import CamNecT.CamNecT_Server.domain.users.model.UserRole;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import CamNecT.CamNecT_Server.global.common.exception.CustomException;
import CamNecT.CamNecT_Server.global.common.response.errorcode.ErrorCode;
import CamNecT.CamNecT_Server.global.common.response.errorcode.bydomains.CommunityErrorCode;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import CamNecT.CamNecT_Server.global.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    @Value("${app.point.reward.comment-selection:200}")
    private int rewardAcceptedComment;
    @Value("${app.point.reward.first-three-likes:100}")
    private int rewardFirstThreeLikes;


    private final BoardsRepository boardsRepository;
    private final PostsRepository postsRepository;
    private final PostStatsRepository postStatsRepository;
    private final PostTagsRepository postTagsRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    private final PostLikesRepository postLikesRepository;
    private final AcceptedCommentsRepository acceptedCommentsRepository;
    private final CommentsRepository commentsRepository;
    private final CommentLikesRepository commentLikesRepository;

    private final PostBookmarksRepository postBookmarksRepository;
    private final PostAccessRepository postAccessRepository;

    private final PostAttachmentsService postAttachmentsService;
    private final PointService pointService;

    private final ApplicationEventPublisher eventPublisher;

    private final AuthorAssembler  authorAssembler;

    @Transactional
    @Override
    public CreatePostResponse create(Long userId, CreatePostRequest req) {
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.USER_NOT_FOUND));

        Boards board = boardsRepository.findByCode(req.boardCode())
                .orElseThrow(() -> new CustomException(CommunityErrorCode.BOARD_NOT_FOUND));

        PostAccessType accessType = (req.accessType() == null) ? PostAccessType.FREE : req.accessType();
        Integer requiredPoints = req.requiredPoints();

        if (accessType == PostAccessType.POINT_REQUIRED) {
            if (requiredPoints == null || requiredPoints <= 0) {
                throw new CustomException(CommunityErrorCode.INVALID_REQUIRED_POINTS);
            }
        } else {
            requiredPoints = null;
        }

        Posts post = Posts.create(board, user, req.title(), req.content(), Boolean.TRUE.equals(req.anonymous()));
        post.applyAccess(accessType, requiredPoints);

        Posts saved = postsRepository.save(post);
        postStatsRepository.save(PostStats.init(saved));

        replaceTags(saved, req.tagIds());

        postAttachmentsService.replace(saved, userId, req.attachments());

        return new CreatePostResponse(saved.getId());
    }

    @Transactional
    @Override
    public void update(Long userId, Long postId, UpdatePostRequest req) {
        if(userId == null) throw new CustomException(CommunityErrorCode.USER_NOT_FOUND);

        Posts post = postsRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.POST_NOT_FOUND));

        if (!Objects.equals(post.getUser().getUserId(), userId)) {
            throw new CustomException(CommunityErrorCode.POST_FORBIDDEN);
        }

        post.update(req.title(), req.content(), req.anonymous());

        if (req.tagIds() != null) {
            postTagsRepository.deleteByPost_Id(postId);
            replaceTags(post, req.tagIds());
        }

        if (req.attachments() != null) {
            postAttachmentsService.replace(post, userId, req.attachments());
        }

        touchStats(postId);
    }

    @Transactional
    @Override
    public void delete(Long userId, Long postId) {
        if (userId == null) throw new CustomException(CommunityErrorCode.USER_NOT_FOUND);

        Posts post = postsRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.POST_NOT_FOUND));

        boolean isAdmin = userRepository.existsByUserIdAndRole(userId, UserRole.ADMIN);
        boolean isOwner = Objects.equals(post.getUser().getUserId(), userId);

        if (!(isAdmin || isOwner)) {
            throw new CustomException(CommunityErrorCode.POST_FORBIDDEN);
        } //Admin이거나 작성자면은 스킵

        if (post.getBoard().getCode() == BoardCode.QUESTION
                && acceptedCommentsRepository.existsByPost_Id(postId)) {
            throw new CustomException(CommunityErrorCode.CANNOT_DELETE_ACCEPTED_QUESTION);
        }
        // 1) 댓글 좋아요 -> 댓글 하드 삭제 (FK 안전)
        commentLikesRepository.deleteByPostId(postId);
        commentsRepository.deleteByPostId(postId);

        // 2) 게시글 좋아요/북마크/구매권한 정리
        postLikesRepository.deleteByPostId(postId);
        postBookmarksRepository.deleteByPostId(postId);
        postAccessRepository.deleteByPostId(postId);
        postStatsRepository.deleteByPostId(postId);


        // 3) 태그/채택 정리
        postTagsRepository.deleteByPost_Id(postId);
        acceptedCommentsRepository.deleteByPost_Id(postId);

        // 4) 첨부 정리 (S3 after-commit 삭제 포함)
        postAttachmentsService.purgeAllByPostId(postId);

        // 5) 마지막: 게시글 soft delete
        post.deleteSoft();
    }

    @Transactional
    @Override
    public ToggleLikeResponse toggleLike(Long userId, Long postId) {
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.USER_NOT_FOUND));

        Posts post = postsRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.POST_NOT_FOUND));

        PostStats stats = postStatsRepository.findByPostIdForUpdate(postId)
                .orElseGet(() -> postStatsRepository.save(PostStats.init(post)));

        //작성자면 본인글 좋아요 불가
        if(Objects.equals(userId, post.getUser().getUserId())) throw new CustomException(CommunityErrorCode.CANNOT_LIKE_OWN_POST);

        boolean liked;
        if (postLikesRepository.existsByPost_IdAndUser_UserId(postId, userId)) {
            postLikesRepository.deleteByPost_IdAndUser_UserId(postId, userId);
            stats.decLike();
            liked = false;
        } else {
            //좋아요 증가 이때 좋아요 개수 3개 이상시 포인트 제공 : 100P
            postLikesRepository.save(PostLikes.of(post, user));
            stats.incLike();
            liked = true;

            // 좋아요 3개 이상 “첫 1회” 보상 -> 정보글 한정
            if (stats.getLikeCount() >= 3 && stats.tryMarkLikeRewarded3()
                    && post.getBoard().getCode()==BoardCode.INFO) {
                // 작성자에게 지급 (본인 글이면 지급 안 줄지 정책 결정)
                Long authorId = post.getUser().getUserId();
                pointService.changePoint(authorId,rewardFirstThreeLikes,
                        TransactionType.EARN, PointEvent.threeLikeReward(authorId,postId));
            }
        }
        return new ToggleLikeResponse(liked, stats.getLikeCount());
    }

    @Transactional
    @Override
    public PostDetailResponse getDetail(Long userId, Long postId) {
        if (userId == null) throw new CustomException(CommunityErrorCode.USER_NOT_FOUND);

        Posts post = postsRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new CustomException(CommunityErrorCode.POST_NOT_PUBLISHED);
        }

        PostStats stats = postStatsRepository.findByPost_Id(post.getId())
                .orElseGet(() -> postStatsRepository.save(PostStats.init(post)));
        stats.incView();

        boolean likedByMe = postLikesRepository.existsByPost_IdAndUser_UserId(postId, userId);

        List<Long> tagIds = postTagsRepository.findByPost_Id(postId).stream()
                .map(pt -> pt.getTag().getId())
                .toList();

        Long acceptedCommentId = acceptedCommentsRepository.findByPost_Id(postId)
                .map(ac -> ac.getComment().getId())
                .orElse(null);

        ContentAccessStatus accessStatus;
        Integer requiredPoints = null;
        Integer myPoints = null;

        if (post.getAccessType() == PostAccessType.POINT_REQUIRED) {
            requiredPoints = post.getRequiredPoints();
            if (requiredPoints == null || requiredPoints <= 0) {
                throw new CustomException(ErrorCode.INTERNAL_ERROR);
            }

            if (userId.equals(post.getUser().getUserId())) {
                accessStatus = ContentAccessStatus.GRANTED;
            } else if (postAccessRepository.existsByPost_IdAndUser_UserId(postId, userId)) {
                accessStatus = ContentAccessStatus.GRANTED;
            } else {
                myPoints = pointService.getBalance(userId);
                accessStatus = (myPoints >= requiredPoints)
                        ? ContentAccessStatus.NEED_PURCHASE
                        : ContentAccessStatus.INSUFFICIENT_POINTS;
            }
        } else {
            accessStatus = ContentAccessStatus.GRANTED;
        }

        String content = (accessStatus == ContentAccessStatus.GRANTED) ? post.getContent() : null;

        AuthorDto author = authorAssembler
                .buildAuthorMap(List.of(post.getUser().getUserId()))
                .get(post.getUser().getUserId());

        return new PostDetailResponse(
                post.getId(),
                post.getBoard().getCode(),
                post.getTitle(),
                content,
                post.isAnonymous(),
                stats.getViewCount(),
                stats.getLikeCount(),
                likedByMe,
                acceptedCommentId,
                tagIds,
                accessStatus,
                requiredPoints,
                myPoints,
                author
        );
    }

    @Transactional
    @Override
    public void acceptComment(Long userId, Long postId, Long commentId) {
        if (userId == null) throw new CustomException(CommunityErrorCode.USER_NOT_FOUND);

        Posts post = postsRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.POST_NOT_FOUND));

        if (post.getBoard().getCode() != BoardCode.QUESTION) {
            throw new CustomException(CommunityErrorCode.ONLY_QUESTION_CAN_ACCEPT);
        }
        if (!Objects.equals(post.getUser().getUserId(), userId)) {
            throw new CustomException(CommunityErrorCode.POST_FORBIDDEN);
        }

        Comments comment = commentsRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.COMMENT_NOT_FOUND));

        if (!Objects.equals(comment.getPost().getId(), postId)) {
            throw new CustomException(CommunityErrorCode.COMMENT_NOT_IN_POST);
        }
        if (comment.getStatus() != CommentStatus.PUBLISHED) {
            throw new CustomException(CommunityErrorCode.CANNOT_ACCEPT_UNPUBLISHED_COMMENT);
        }


        try {
            acceptedCommentsRepository.save(AcceptedComments.of(post, comment, userId));
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(CommunityErrorCode.ALREADY_ACCEPTED, e);
        }

        touchStats(postId);

        Long receiverId = comment.getUserId();
        if (receiverId != null && !Objects.equals(receiverId, userId)) {
            pointService.earnPointByCommentSelection(receiverId, postId, commentId, rewardAcceptedComment);

            eventPublisher.publishEvent(new CommentAcceptedEvent(receiverId, postId, commentId, userId));
        }
    }

    @Transactional
    @Override
    public ToggleBookmarkResponse toggleBookmark(Long userId, Long postId) {
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.USER_NOT_FOUND));

        Posts post = postsRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.POST_NOT_FOUND));

        PostStats stats = postStatsRepository.findByPost_Id(postId)
                .orElseGet(() -> postStatsRepository.save(PostStats.init(post)));

        boolean exists = postBookmarksRepository.existsByPost_IdAndUser_UserId(postId, userId);

        if (exists) {
            postBookmarksRepository.deleteByPost_IdAndUser_UserId(postId, userId);
            stats.decBookmark();
        } else {
            postBookmarksRepository.save(PostBookmarks.create(post, user));
            stats.incBookmark();
        }

        postStatsRepository.save(stats);

        return new ToggleBookmarkResponse(postId, !exists, stats.getBookmarkCount());
    }


    @Transactional
    @Override
    public PurchasePostAccessResponse purchasePostAccess(Long userId, Long postId) {
        userRepository.lockUserRow(userId);

        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.USER_NOT_FOUND));

        Posts post = postsRepository.findById(postId)
                .orElseThrow(() -> new CustomException(CommunityErrorCode.POST_NOT_FOUND));

        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new CustomException(CommunityErrorCode.POST_NOT_PUBLISHED);
        }

        if (post.getAccessType() != PostAccessType.POINT_REQUIRED) {
            int bal = pointService.getBalance(user.getUserId());
            return new PurchasePostAccessResponse(postId, ContentAccessStatus.GRANTED, bal);
        }

        Integer cost = post.getRequiredPoints();
        if (cost == null || cost <= 0) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }

        if (userId.equals(post.getUser().getUserId())) {
            int bal = pointService.getBalance(user.getUserId());
            return new PurchasePostAccessResponse(postId, ContentAccessStatus.GRANTED, bal);
        }

        if (postAccessRepository.existsByPost_IdAndUser_UserId(postId, userId)) {
            int bal = pointService.getBalance(user.getUserId());
            return new PurchasePostAccessResponse(postId, ContentAccessStatus.GRANTED, bal);
        }

        pointService.spendPoint(userId, cost, PointEvent.postAccess(userId, postId));

        try {
            postAccessRepository.save(PostAccess.of(user, post, cost));
        } catch (DataIntegrityViolationException ignored) {
        }

        int remaining = pointService.getBalance(userId);
        return new PurchasePostAccessResponse(postId, ContentAccessStatus.GRANTED, remaining);
    }

    private void replaceTags(Posts post, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return;

        List<Long> ids = tagIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return;

        List<Tag> tags = tagRepository.findAllById(ids);
        if (tags.size() != ids.size()) {
            throw new CustomException(CommunityErrorCode.INVALID_TAG_IDS);
        }

        for (Tag t : tags) {
            if (!t.isActive()) throw new CustomException(CommunityErrorCode.INACTIVE_TAG);
            postTagsRepository.save(PostTags.link(post, t));
        }
    }

    private void touchStats(Long postId) {
        postStatsRepository.findByPost_Id(postId).ifPresent(PostStats::touch);
    }
}
