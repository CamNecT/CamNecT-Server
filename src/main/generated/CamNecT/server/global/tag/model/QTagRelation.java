package CamNecT.server.global.tag.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTagRelation is a Querydsl query type for TagRelation
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTagRelation extends EntityPathBase<TagRelation> {

    private static final long serialVersionUID = -402671642L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTagRelation tagRelation = new QTagRelation("tagRelation");

    public final NumberPath<Integer> evidenceCount = createNumber("evidenceCount", Integer.class);

    public final QTagRelation_TagRelationId id;

    public final NumberPath<Double> score = createNumber("score", Double.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QTagRelation(String variable) {
        this(TagRelation.class, forVariable(variable), INITS);
    }

    public QTagRelation(Path<? extends TagRelation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTagRelation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTagRelation(PathMetadata metadata, PathInits inits) {
        this(TagRelation.class, metadata, inits);
    }

    public QTagRelation(Class<? extends TagRelation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QTagRelation_TagRelationId(forProperty("id")) : null;
    }

}

