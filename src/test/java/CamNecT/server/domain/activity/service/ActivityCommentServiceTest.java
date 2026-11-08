package CamNecT.server.domain.activity.service;

import CamNecT.server.domain.activity.model.comment.ActivityComment;
import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.activity.repository.comment.ActivityCommentLikeRepository;
import CamNecT.server.domain.activity.repository.comment.ActivityCommentRepository;
import CamNecT.server.domain.activity.repository.external_activity.ExternalActivityRepository;
import CamNecT.server.domain.community.dto.request.CreateCommentRequest;
import CamNecT.server.domain.community.model.enums.CommentStatus;
import CamNecT.server.domain.community.service.AuthorAssembler;
import CamNecT.server.global.common.auth.AccountAccessGuard;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.ActivityErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.CommunityErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityCommentServiceTest {
    @Mock ExternalActivityRepository activityRepository;
    @Mock ActivityCommentRepository commentsRepository;
    @Mock ActivityCommentLikeRepository likesRepository;
    @Mock AccountAccessGuard accountAccessGuard;
    @Mock AuthorAssembler authorAssembler;
    @InjectMocks ActivityCommentService service;

    @ParameterizedTest
    @EnumSource(value = ActivityCategory.class, names = {"EXTERNAL", "RECRUITMENT"})
    void rejectsUnsupportedCategory(ActivityCategory category) {
        when(activityRepository.findByIdForRead(10L)).thenReturn(Optional.of(activity(10L, category)));
        CustomException error = assertThrows(CustomException.class,
                () -> service.create(1L, 10L, new CreateCommentRequest("댓글", null)));
        assertThat(error.getErrorCode()).isEqualTo(ActivityErrorCode.INVALID_ACTIVITY_CATEGORY);
        verify(commentsRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = ActivityCategory.class, names = {"CLUB", "STUDY"})
    void createsCommentsForSupportedCategories(ActivityCategory category) {
        when(activityRepository.findByIdForRead(10L)).thenReturn(Optional.of(activity(10L, category)));
        when(commentsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.create(1L, 10L, new CreateCommentRequest("댓글", null));
        verify(commentsRepository).save(argThat(c -> c.getActivity().getCategory() == category && c.getStatus() == CommentStatus.PUBLISHED));
    }

    @Test
    void rejectsParentFromAnotherActivity() {
        when(activityRepository.findByIdForRead(10L)).thenReturn(Optional.of(activity(10L, ActivityCategory.CLUB)));
        ActivityComment parent = ActivityComment.create(activity(11L, ActivityCategory.CLUB), 2L, null, "부모");
        when(commentsRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(parent));
        CustomException error = assertThrows(CustomException.class,
                () -> service.create(1L, 10L, new CreateCommentRequest("답글", 2L)));
        assertThat(error.getErrorCode()).isEqualTo(CommunityErrorCode.PARENT_COMMENT_NOT_IN_POST);
        verify(commentsRepository, never()).save(any());
    }

    @Test
    void rejectsReplyToReply() {
        ExternalActivity activity = activity(10L, ActivityCategory.STUDY);
        when(activityRepository.findByIdForRead(10L)).thenReturn(Optional.of(activity));
        ActivityComment root = ActivityComment.create(activity, 1L, null, "부모");
        ActivityComment reply = ActivityComment.create(activity, 2L, root, "답글");
        when(commentsRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(reply));
        CustomException error = assertThrows(CustomException.class,
                () -> service.create(1L, 10L, new CreateCommentRequest("답글", 2L)));
        assertThat(error.getErrorCode()).isEqualTo(CommunityErrorCode.COMMENT_MAX_DEPTH_EXCEEDED);
    }

    private ExternalActivity activity(Long id, ActivityCategory category) {
        return ExternalActivity.builder().activityId(id).category(category).build();
    }
}
