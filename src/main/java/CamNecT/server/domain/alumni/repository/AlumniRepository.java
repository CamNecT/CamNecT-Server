package CamNecT.server.domain.alumni.repository;

import CamNecT.server.domain.users.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlumniRepository extends JpaRepository<Users, Long>, AlumniRepositoryCustom {

    @Query(value = """
        SELECT u.user_id FROM Users u
        JOIN User_Profile up ON u.user_id = up.user_id
        WHERE u.user_id != :myId
        AND (:name IS NULL OR u.name LIKE CONCAT('%', :name, '%'))
        AND (:tagCount = 0 OR u.user_id IN (
            SELECT utm.user_id FROM user_tag_map utm
            WHERE utm.tag_id IN :tagIdList
            GROUP BY utm.user_id
            HAVING COUNT(DISTINCT utm.tag_id) = :tagCount
        ))
        ORDER BY (
            SELECT COUNT(*) FROM user_tag_map utm2
            WHERE utm2.user_id = u.user_id
            AND utm2.tag_id IN (
                SELECT utm3.tag_id FROM user_tag_map utm3 WHERE utm3.user_id = :myId
            )
        ) DESC, u.created_at DESC
        """, nativeQuery = true)
    List<Long> findAlumniIdsByConditions(
            @Param("myId") Long myId,
            @Param("name") String name,
            @Param("tagIdList") List<Long> tagIdList,
            @Param("tagCount") int tagCount
    );
}