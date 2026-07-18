package CamNecT.server.domain.activity.model.external_activity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QExternalActivityTag is a Querydsl query type for ExternalActivityTag
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QExternalActivityTag extends EntityPathBase<ExternalActivityTag> {

    private static final long serialVersionUID = -1790749753L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QExternalActivityTag externalActivityTag = new QExternalActivityTag("externalActivityTag");

    public final QExternalActivity activity;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final CamNecT.server.global.tag.model.QTag tag;

    public QExternalActivityTag(String variable) {
        this(ExternalActivityTag.class, forVariable(variable), INITS);
    }

    public QExternalActivityTag(Path<? extends ExternalActivityTag> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QExternalActivityTag(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QExternalActivityTag(PathMetadata metadata, PathInits inits) {
        this(ExternalActivityTag.class, metadata, inits);
    }

    public QExternalActivityTag(Class<? extends ExternalActivityTag> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.activity = inits.isInitialized("activity") ? new QExternalActivity(forProperty("activity"), inits.get("activity")) : null;
        this.tag = inits.isInitialized("tag") ? new CamNecT.server.global.tag.model.QTag(forProperty("tag"), inits.get("tag")) : null;
    }

}

