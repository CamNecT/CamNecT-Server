package CamNecT.server.global.notification.service;

import CamNecT.server.domain.users.repository.UserProfileRepository;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.server.global.notification.model.Notification;
import CamNecT.server.global.notification.model.NotificationType;
import CamNecT.server.global.notification.repository.NotificationRepository;
import CamNecT.server.global.storage.service.PublicUrlIssuer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
    private final PublicUrlIssuer publicUrlIssuer = mock(PublicUrlIssuer.class);

    private final NotificationService notificationService = new NotificationService(
            notificationRepository,
            userRepository,
            userProfileRepository,
            publicUrlIssuer
    );

    @Test
    void markReadTreatsAlreadyReadNotificationAsSuccess() {
        when(notificationRepository.existsByIdAndReceiverUserId(10L, 1L)).thenReturn(true);
        when(notificationRepository.markRead(1L, 10L)).thenReturn(0);

        assertDoesNotThrow(() -> notificationService.markRead(1L, 10L));

        verify(notificationRepository).markRead(1L, 10L);
    }

    @Test
    void markReadRejectsMissingOrOtherUsersNotification() {
        when(notificationRepository.existsByIdAndReceiverUserId(10L, 1L)).thenReturn(false);

        CustomException ex = assertThrows(
                CustomException.class,
                () -> notificationService.markRead(1L, 10L)
        );

        assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.NOTIFICATION_NOT_FOUND);
        verify(notificationRepository, never()).markRead(anyLong(), anyLong());
    }

    @Test
    void listNormalizesInvalidPageSizeToDefault() {
        when(notificationRepository.findByReceiverUserIdAndReadFalseAndTypeNotOrderByIdDesc(
                eq(1L),
                eq(NotificationType.CHAT_MESSAGE_RECEIVED),
                any(Pageable.class)
        )).thenReturn(new SliceImpl<>(List.of()));

        notificationService.list(1L, null, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findByReceiverUserIdAndReadFalseAndTypeNotOrderByIdDesc(
                eq(1L),
                eq(NotificationType.CHAT_MESSAGE_RECEIVED),
                captor.capture()
        );
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
    }
}
