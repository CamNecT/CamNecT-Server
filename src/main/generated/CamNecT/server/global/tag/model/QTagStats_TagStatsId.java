package CamNecT.server.global.tag.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTagStats_TagStatsId is a Querydsl query type for TagStatsId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QTagStats_TagStatsId extends BeanPath<TagStats.TagStatsId> {

    private static final long serialVersionUID = -661538343L;

    public static final QTagStats_TagStatsId tagStatsId = new QTagStats_TagStatsId("tagStatsId");

    public final EnumPath<CamNecT.server.global.tag.model.enums.TagRelationContext> context = createEnum("context", CamNecT.server.global.tag.model.enums.TagRelationContext.class);

    public final NumberPath<Long> tagId = createNumber("tagId", Long.class);

    public QTagStats_TagStatsId(String variable) {
        super(TagStats.TagStatsId.class, forVariable(variable));
    }

    public QTagStats_TagStatsId(Path<? extends TagStats.TagStatsId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTagStats_TagStatsId(PathMetadata metadata) {
        super(TagStats.TagStatsId.class, metadata);
    }

}

