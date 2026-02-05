package CamNecT.CamNecT_Server.global.tag.repository;

import CamNecT.CamNecT_Server.global.tag.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    @Query("SELECT utm.userId, t FROM UserTagMap utm " +
            "JOIN Tag t ON utm.tagId = t.id " +
            "WHERE utm.userId IN :userIds AND t.active = true")
    List<Object[]> findTagsWithUserIdByUserIdIn(@Param("userIds") List<Long> userIds);
}

