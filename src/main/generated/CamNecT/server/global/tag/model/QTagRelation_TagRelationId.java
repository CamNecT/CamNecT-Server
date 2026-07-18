package CamNecT.server.global.tag.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTagRelation_TagRelationId is a Querydsl query type for TagRelationId
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QTagRelation_TagRelationId extends BeanPath<TagRelation.TagRelationId> {

    private static final long serialVersionUID = 632303433L;

    public static final QTagRelation_TagRelationId tagRelationId = new QTagRelation_TagRelationId("tagRelationId");

    public final EnumPath<CamNecT.server.global.tag.model.enums.TagRelationContext> context = createEnum("context", CamNecT.server.global.tag.model.enums.TagRelationContext.class);

    public final NumberPath<Long> fromTagId = createNumber("fromTagId", Long.class);

    public final NumberPath<Long> toTagId = createNumber("toTagId", Long.class);

    public QTagRelation_TagRelationId(String variable) {
        super(TagRelation.TagRelationId.class, forVariable(variable));
    }

    public QTagRelation_TagRelationId(Path<? extends TagRelation.TagRelationId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTagRelation_TagRelationId(PathMetadata metadata) {
        super(TagRelation.TagRelationId.class, metadata);
    }

}

