package CamNecT.CamNecT_Server.domain.chat.repository;

import CamNecT.CamNecT_Server.domain.chat.model.ChatRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRequestRepository extends JpaRepository<ChatRequest, Long> {
//    // 받은 요청 목록
//    List<ChatRequest> findAllByReceiverIdOrderByCreatedAtDesc(Long receiverId);
//
//    // 보낸 요청 목록
//    List<ChatRequest> findAllByRequesterIdOrderByCreatedAtDesc(Long requesterId);
//
//    // 중복 신청 방지
//    boolean existsByRequesterIdAndReceiverIdAndStatus(Long requesterId, Long receiverId, ChatRequest.RequestStatus status);


    @Query("SELECT cr FROM ChatRequest cr " +
            "JOIN FETCH cr.requester " +
            "JOIN FETCH cr.requestInterests " +
            "WHERE cr.receiver.userId = :userId AND cr.status = :status")
    List<ChatRequest> findAllByReceiver_UserIdAndStatusOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("status") ChatRequest.RequestStatus status
    );


    //    List<ChatRequest> findAllByReceiver_UserIdAndStatusOrderByCreatedAtDesc(Long userId, ChatRequest.RequestStatus status);

    // 내가 보낸 요청 목록
    List<ChatRequest> findAllByRequester_UserIdOrderByCreatedAtDesc(Long userId);

    // 중복 신청 방지
    boolean existsByRequester_UserIdAndReceiver_UserIdAndStatus(Long requesterId, Long receiverId, ChatRequest.RequestStatus status);

    long countByReceiver_UserIdAndStatus(Long userId, ChatRequest.RequestStatus status);

    @Query("""
                select cr
                from ChatRequest cr
                join fetch cr.requester r
                where cr.receiver.userId = :userId
                  and cr.status = :status
                order by cr.createdAt desc
            """)
    List<ChatRequest> findLatestReceivedRequests(
            @Param("userId") Long userId,
            @Param("status") ChatRequest.RequestStatus status,
            Pageable pageable
    );

    @Query("SELECT cr FROM ChatRequest cr " +
            "JOIN FETCH cr.requester " +
            "WHERE cr.receiver.userId = :userId AND cr.type = :type AND cr.status = :status " +
            "ORDER BY cr.createdAt DESC")
    List<ChatRequest> findAllByReceiver_UserIdAndTypeAndStatusOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("type") ChatRequest.RequestType type,
            @Param("status") ChatRequest.RequestStatus status
    );

    @Query("SELECT cr FROM ChatRequest cr " +
            "JOIN FETCH cr.requester " +
            "WHERE cr.receiver.userId = :userId " +
            "AND cr.type = :type " +
            "AND cr.status = :status")
    List<ChatRequest> findAllByReceiver_UserIdAndTypeAndStatus(
            @Param("userId") Long userId,
            @Param("type") ChatRequest.RequestType type,
            @Param("status") ChatRequest.RequestStatus status
    );

    @Query("SELECT cr FROM ChatRequest cr " +
            "JOIN FETCH cr.requester " +
            "WHERE cr.receiver.userId = :userId " +
            "AND cr.type = :type " +
            "AND cr.recruitmentId = :recruitmentId " +
            "AND cr.status = :status")
    List<ChatRequest> findAllByReceiver_UserIdAndTypeAndRecruitmentIdAndStatus(
            @Param("userId") Long userId,
            @Param("type") ChatRequest.RequestType type,
            @Param("recruitmentId") Long recruitmentId,
            @Param("status") ChatRequest.RequestStatus status
    );
}


