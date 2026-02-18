package CamNecT.server.domain.chat.repository;

import CamNecT.server.domain.chat.model.Chat;
import CamNecT.server.domain.chat.model.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {


    @Query("SELECT c FROM Chat c " +
            "JOIN FETCH c.sender " +
            "JOIN FETCH c.receiver " +
            "WHERE c.room.id = :roomId " +
            "ORDER BY c.id DESC")
    List<Chat> findTop1000ByRoomId(@Param("roomId") Long roomId, Pageable pageable);

    // 내가 받은 안 읽은 메시지
    @Query("""
                select c
                from Chat c
                where c.room.id = :roomId
                  and c.receiver.userId = :userId
                  and c.isRead = false
            """)
    List<Chat> findUnreadMessages(@Param("roomId") Long roomId, @Param("userId") Long userId);

    long countByRoom_IdAndReceiver_UserIdAndIsReadFalse(Long roomId, Long receiverId);

    long countByReceiver_UserIdAndIsReadFalse(Long receiverId);

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