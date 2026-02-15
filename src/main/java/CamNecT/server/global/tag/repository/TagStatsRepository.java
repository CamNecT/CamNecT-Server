package CamNecT.server.global.tag.repository;

import CamNecT.server.global.tag.model.TagStats;
import CamNecT.server.global.tag.model.enums.TagRelationContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagStatsRepository extends JpaRepository<TagStats, TagStats.TagStatsId> {

    List<TagStats> findAllById_Context(TagRelationContext context);
}