package CamNecT.server.domain.community.model.Posts;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostAttachments is a Querydsl query type for PostAttachments
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostAttachments extends EntityPathBase<PostAttachments> {

    private static final long serialVersionUID = 1120787541L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostAttachments postAttachments = new QPostAttachments("postAttachments");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath fileKey = createString("fileKey");

    public final NumberPath<Long> fileSize = createNumber("fileSize", Long.class);

    public final NumberPath<Integer> height = createNumber("height", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QPosts post;

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public final BooleanPath status = createBoolean("status");

    public final NumberPath<Integer> width = createNumber("width", Integer.class);

    public QPostAttachments(String variable) {
        this(PostAttachments.class, forVariable(variable), INITS);
    }

    public QPostAttachments(Path<? extends PostAttachments> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostAttachments(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostAttachments(PathMetadata metadata, PathInits inits) {
        this(PostAttachments.class, metadata, inits);
    }

    public QPostAttachments(Class<? extends PostAttachments> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new QPosts(forProperty("post"), inits.get("post")) : null;
    }

}

