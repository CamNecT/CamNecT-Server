package CamNecT.server.global.notification.repository;

import CamNecT.server.global.notification.model.Notification;
import CamNecT.server.global.notification.model.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Slice<Notification> findByReceiverUserIdAndReadFalseAndTypeNotOrderByIdDesc(
            Long receiverUserId,
            NotificationType type,
            Pageable pageable
    );

    Slice<Notification> findByReceiverUserIdAndReadFalseAndTypeNotAndIdLessThanOrderByIdDesc(
            Long receiverUserId,
            NotificationType type,
            Long cursorId,
            Pageable pageable
    );

    long countByReceiverUserIdAndReadFalseAndTypeNot(Long receiverUserId, NotificationType type);

    boolean existsByIdAndReceiverUserId(Long id, Long receiverUserId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
           set n.read = true
         where n.id = :notificationId
           and n.receiverUserId = :receiverUserId
           and n.read = false
    """)
    int markRead(
            @Param("receiverUserId") Long receiverUserId,
            @Param("notificationId") Long notificationId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
           set n.read = true
         where n.receiverUserId = :receiverUserId
           and n.read = false
    """)
    int markAllRead(@Param("receiverUserId") Long receiverUserId);
}
