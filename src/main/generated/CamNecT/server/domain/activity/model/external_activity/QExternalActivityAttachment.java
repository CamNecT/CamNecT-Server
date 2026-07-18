package CamNecT.server.domain.activity.model.external_activity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QExternalActivityAttachment is a Querydsl query type for ExternalActivityAttachment
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QExternalActivityAttachment extends EntityPathBase<ExternalActivityAttachment> {

    private static final long serialVersionUID = 723265622L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QExternalActivityAttachment externalActivityAttachment = new QExternalActivityAttachment("externalActivityAttachment");

    public final QExternalActivity activity;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath fileKey = createString("fileKey");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public QExternalActivityAttachment(String variable) {
        this(ExternalActivityAttachment.class, forVariable(variable), INITS);
    }

    public QExternalActivityAttachment(Path<? extends ExternalActivityAttachment> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QExternalActivityAttachment(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QExternalActivityAttachment(PathMetadata metadata, PathInits inits) {
        this(ExternalActivityAttachment.class, metadata, inits);
    }

    public QExternalActivityAttachment(Class<? extends ExternalActivityAttachment> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.activity = inits.isInitialized("activity") ? new QExternalActivity(forProperty("activity"), inits.get("activity")) : null;
    }

}

