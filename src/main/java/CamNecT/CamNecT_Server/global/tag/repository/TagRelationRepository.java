package CamNecT.CamNecT_Server.global.tag.repository;

import CamNecT.CamNecT_Server.global.tag.model.TagRelation;
import CamNecT.CamNecT_Server.global.tag.model.enums.TagRelationContext;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRelationRepository extends JpaRepository<TagRelation, TagRelation.TagRelationId> {

    List<TagRelation> findTop15ById_ContextAndId_FromTagIdOrderByScoreDesc(
            TagRelationContext context,
            Long fromTagId
    );

    // topK를 가변으로 쓰고 싶으면 아래처럼 pageable로 받는 버전도 같이 두면 좋습니다.
    List<TagRelation> findById_ContextAndId_FromTagIdOrderByScoreDesc(TagRelationContext context, Long fromTagId, Pageable pageable);
}