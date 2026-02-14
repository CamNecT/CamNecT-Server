package CamNecT.server.domain.community.repository.Posts;

import CamNecT.server.domain.community.model.Posts.PostTags;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostTagsRepository extends JpaRepository<PostTags, Long> {

    List<PostTags> findByPost_Id(Long postId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PostTags pt where pt.post.id = :postId")
    void deleteByPost_Id(Long postId);

    boolean existsByPost_IdAndTag_Id(Long postId, Long tagId);

    // 피드용: postIds의 태그를 한 번에 가져오기 (N+1 방지 + tag까지 fetch)
    @Query("""
        select pt from PostTags pt
        join fetch pt.tag t
        where pt.post.id in :postIds
    """)
    List<PostTags> findAllByPostIdsWithTag(@Param("postIds") Collection<Long> postIds);
}
