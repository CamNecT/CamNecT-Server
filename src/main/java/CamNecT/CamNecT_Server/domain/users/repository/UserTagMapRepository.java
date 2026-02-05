package CamNecT.CamNecT_Server.domain.users.repository;

import CamNecT.CamNecT_Server.domain.users.model.UserTagMap;
import CamNecT.CamNecT_Server.global.tag.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserTagMapRepository extends JpaRepository<UserTagMap, Long> {

    @Modifying
    @Query("DELETE FROM UserTagMap utm WHERE utm.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM Tag t " +
            "JOIN UserTagMap utm ON t.id = utm.tagId " +
            "WHERE utm.userId = :userId")
    List<Tag> findAllTagsByUserId(@Param("userId") Long userId);

    //유저들의 모든 태그 매핑 정보를 한번에 가져오기
    List<UserTagMap> findAllByUserIdIn(List<Long> userIds);
    long countByUserId(Long userId);

}
