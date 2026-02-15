package CamNecT.server.domain.chat.repository;

import CamNecT.server.domain.chat.model.ChatRequest;
import CamNecT.server.domain.chat.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 전체 조회
    @Query("SELECT r FROM ChatRoom r " +
            "JOIN FETCH r.request " +
            "JOIN FETCH r.requester " +
            "JOIN FETCH r.receiver " +
            "WHERE (r.requester.userId = :userId AND r.requesterExited = false) " +
            "OR (r.receiver.userId = :userId AND r.receiverExited = false) " +
            "ORDER BY r.lastMessageAt DESC")
    List<ChatRoom> findAllByUserIdWithBasicInfo(@Param("userId") Long userId);

    // 타입별 조회
    @Query("SELECT r FROM ChatRoom r " +
            "JOIN FETCH r.request req " +
            "JOIN FETCH r.requester " +
            "JOIN FETCH r.receiver " +
            "WHERE ((r.requester.userId = :userId AND r.requesterExited = false) " +
            "OR (r.receiver.userId = :userId AND r.receiverExited = false)) " +
            "AND req.type = :type " +
            "ORDER BY r.lastMessageAt DESC")
    List<ChatRoom> findAllByUserIdAndType(@Param("userId") Long userId,
                                          @Param("type") ChatRequest.RequestType type);

    // 상세 조회
    @Query("SELECT r FROM ChatRoom r " +
            "JOIN FETCH r.requester " +
            "JOIN FETCH r.receiver " +
            "JOIN FETCH r.request req " +
            "JOIN FETCH req.requestInterests " +
            "WHERE r.id = :roomId " +
            "AND ((r.requester.userId = :userId AND r.requesterExited = false) " +
            "OR (r.receiver.userId = :userId AND r.receiverExited = false))")
    Optional<ChatRoom> findByUserIdWithDetails(@Param("roomId") Long roomId, @Param("userId") Long userId);

}
