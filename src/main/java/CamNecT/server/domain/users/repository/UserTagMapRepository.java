package CamNecT.server.domain.users.repository;

import CamNecT.server.domain.users.model.UserTagMap;
import CamNecT.server.global.tag.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserTagMapRepository extends JpaRepository<UserTagMap, Long> {

    @Query("""
    SELECT utm.userId, t.name
    FROM UserTagMap utm
    JOIN Users u ON u.userId = utm.userId
    JOIN Tag t ON utm.tagId = t.id
    WHERE utm.userId IN :userIds
      AND t.active = true
""")
    List<Object[]> findTagNamesWithUserIdByUserIdIn(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query("DELETE FROM UserTagMap utm WHERE utm.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("""
    SELECT DISTINCT t
    FROM Tag t
    JOIN FETCH t.category c
    JOIN UserTagMap utm ON t.id = utm.tagId
    JOIN Users u ON u.userId = utm.userId
    WHERE utm.userId = :userId
      AND t.active = true
""")
    List<Tag> findAllTagsByUserId(@Param("userId") Long userId);

}
