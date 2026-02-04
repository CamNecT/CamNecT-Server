package CamNecT.CamNecT_Server.domain.chat.repository;

import CamNecT.CamNecT_Server.domain.chat.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
/*    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.requester.userId = :userId OR cr.receiver.userId = :userId " +
            "ORDER BY cr.lastMessageAt DESC")
    List<ChatRoom> findMyChatRooms(@Param("userId") Long userId);

    @Query("SELECT r FROM ChatRoom r " +
            "JOIN FETCH r.request req " +
            "JOIN FETCH req.requestInterests " +
            "WHERE r.requester.userId = :userId OR r.receiver.userId = :userId")
    List<ChatRoom> findAllByUserIdWithRequest(@Param("userId") Long userId);*/

    @Query("SELECT DISTINCT r FROM ChatRoom r " +
            "JOIN FETCH r.request req " +
            "LEFT JOIN FETCH req.requestInterests " +
            "JOIN FETCH r.requester " +
            "JOIN FETCH r.receiver " +
            "WHERE r.requester.userId = :userId OR r.receiver.userId = :userId")
    List<ChatRoom> findAllByUserIdWithBasicInfo(@Param("userId") Long userId);


    @Query("SELECT r FROM ChatRoom r " +
            "JOIN FETCH r.requester " +
            "JOIN FETCH r.receiver " +
            "JOIN FETCH r.request req " +
            "JOIN FETCH req.requestInterests " +
            "WHERE r.id = :roomId")
    Optional<ChatRoom> findByUserIdWithDetails(@Param("roomId") Long roomId);


}