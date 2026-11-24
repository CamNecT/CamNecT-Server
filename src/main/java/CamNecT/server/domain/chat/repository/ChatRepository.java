package CamNecT.server.domain.chat.repository;

import CamNecT.server.domain.chat.model.Chat;
import CamNecT.server.domain.chat.model.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {


    @Query("SELECT c FROM Chat c " +
            "JOIN FETCH c.sender " +
            "JOIN FETCH c.receiver " +
            "WHERE c.room.id = :roomId " +
            "ORDER BY c.id DESC")
    List<Chat> findTop1000ByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    interface UnreadBoundary {
        long getFirstId();
        long getLastId();
        String getContent();
        LocalDateTime getCreatedAt();
    }

    @Query("""
            select (select min(m.id) from Chat m where m.room.id = :roomId
                    and m.receiver.userId = :userId and m.isRead = false) as firstId,
                   c.id as lastId, c.content as content, c.createdAt as createdAt
            from Chat c
            where c.id = (select max(m.id) from Chat m where m.room.id = :roomId
                          and m.receiver.userId = :userId and m.isRead = false)
            """)
    Optional<UnreadBoundary> findUnreadBoundary(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Chat c set c.isRead = true, c.readAt = :at, c.updatedAt = :at
            where c.room.id = :roomId and c.receiver.userId = :userId and c.isRead = false
              and c.id between :firstId and :lastId
            """)
    int markUnreadRange(@Param("roomId") Long roomId, @Param("userId") Long userId,
                        @Param("firstId") long firstId, @Param("lastId") long lastId, @Param("at") LocalDateTime at);

    long countByRoom_IdAndReceiver_UserIdAndIsReadFalse(Long roomId, Long receiverId);

    long countByReceiver_UserIdAndIsReadFalse(Long receiverId);

    @Query("""
            select c
            from Chat c
            join fetch c.room
            join fetch c.sender
            join fetch c.receiver
            where c.room.id = :roomId
              and c.sender.userId = :senderId
              and c.clientMessageId = :clientMessageId
            """)
    Optional<Chat> findByClientMessageId(
            @Param("roomId") Long roomId,
            @Param("senderId") Long senderId,
            @Param("clientMessageId") String clientMessageId
    );

    long countByRoom_IdAndSender_UserIdAndClientMessageId(
            Long roomId,
            Long senderId,
            String clientMessageId
    );

    @Query("""
            select count(c)
            from Chat c
            where c.receiver.userId = :userId
              and c.isRead = false
              and ((c.room.requester.userId = :userId and c.room.requesterExited = false)
                or (c.room.receiver.userId = :userId and c.room.receiverExited = false))
            """)
    long countVisibleUnreadByUserId(@Param("userId") Long userId);

    interface RecipientUnread {
        Long getUserId();
        long getRoomUnread();
        long getTotalUnread();
    }

    @Query("""
            select c.receiver.userId as userId,
                   sum(case when r.id = :roomId then 1 else 0 end) as roomUnread,
                   sum(case when (r.requester.userId = c.receiver.userId and r.requesterExited = false)
                              or (r.receiver.userId = c.receiver.userId and r.receiverExited = false)
                            then 1 else 0 end) as totalUnread
            from Chat c join c.room r
            where c.isRead = false and c.receiver.userId in :participants
            group by c.receiver.userId
            """)
    List<RecipientUnread> countForDelivery(@Param("roomId") Long roomId, @Param("participants") List<Long> participants);

    @Query("SELECT c.room.id, COUNT(c) " +
            "FROM Chat c " +
            "WHERE c.room IN :rooms " +
            "AND c.sender.userId != :myId " +
            "AND c.isRead = false " +
            "GROUP BY c.room.id")
        // 방 별로 묶어서 카운트
    List<Object[]> countUnreadMessagesByRooms(@Param("rooms") List<ChatRoom> rooms, @Param("myId") Long myId);

    @Query("SELECT c.room.id, c.content " +
            "FROM Chat c " +
            "WHERE c.id IN (" +
            "   SELECT MAX(c2.id) " +
            "   FROM Chat c2 " +
            "   WHERE c2.room IN :rooms " +
            "   GROUP BY c2.room" +
            ")")
    List<Object[]> findLastMessagesByRooms(@Param("rooms") List<ChatRoom> rooms);
}
