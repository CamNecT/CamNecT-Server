package CamNecT.server.domain.community.model.Comments;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAcceptedComments is a Querydsl query type for AcceptedComments
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAcceptedComments extends EntityPathBase<AcceptedComments> {

    private static final long serialVersionUID = 844989749L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAcceptedComments acceptedComments = new QAcceptedComments("acceptedComments");

    public final QComments comment;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final CamNecT.server.domain.community.model.Posts.QPosts post;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QAcceptedComments(String variable) {
        this(AcceptedComments.class, forVariable(variable), INITS);
    }

    public QAcceptedComments(Path<? extends AcceptedComments> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAcceptedComments(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAcceptedComments(PathMetadata metadata, PathInits inits) {
        this(AcceptedComments.class, metadata, inits);
    }

    public QAcceptedComments(Class<? extends AcceptedComments> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.comment = inits.isInitialized("comment") ? new QComments(forProperty("comment"), inits.get("comment")) : null;
        this.post = inits.isInitialized("post") ? new CamNecT.server.domain.community.model.Posts.QPosts(forProperty("post"), inits.get("post")) : null;
    }

}

