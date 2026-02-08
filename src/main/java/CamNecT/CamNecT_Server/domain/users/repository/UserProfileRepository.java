package CamNecT.CamNecT_Server.domain.users.repository;

import CamNecT.CamNecT_Server.domain.users.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    //userId에 해당하는 프로필 조회
    Optional<UserProfile> findByUserId(Long userId);

    //List<Long> 형식으로 들어오는 유저 id에 해당하는 프로필들 반환
    List<UserProfile> findAllByUserIdIn(List<Long> userIds);

    boolean existsByUserId(Long userId);


    @Query("SELECT DISTINCT up FROM UserProfile up " +
            "JOIN UserTagMap utm ON up.userId = utm.userId " +
            "WHERE utm.tagId IN :tagIds")
    Page<UserProfile> findByAnyTagIds(@Param("tagIds") List<Long> tagIds, Pageable pageable);

    @Query("SELECT up FROM UserProfile up JOIN FETCH up.user WHERE up.userId = :userId")
    Optional<UserProfile> findByUserIdWithUser(Long userId);

    @Query("""
        SELECT up FROM UserProfile up
        WHERE EXISTS ( SELECT 1 FROM UserTagMap utm
        WHERE utm.userId = up.userId
        AND utm.tagId IN :tagIds GROUP BY utm.userId
        HAVING COUNT(DISTINCT utm.tagId) = :tagCount
        )
    """)
    Slice<UserProfile> findByAllTagIds(List<Long> tagIds, Long tagCount, Pageable pageable);

    boolean existsByUserIdAndOpenToCoffeeChatTrue(Long userId);

    @Query("""
    select up
    from UserProfile up
    join fetch up.user u
    where up.userId in :userIds
""")
    List<UserProfile> findAllByUserIdInWithUser(@Param("userIds") List<Long> userIds);
}