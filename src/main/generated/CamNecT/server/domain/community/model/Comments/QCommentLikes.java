package CamNecT.server.domain.community.model.Comments;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCommentLikes is a Querydsl query type for CommentLikes
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCommentLikes extends EntityPathBase<CommentLikes> {

    private static final long serialVersionUID = -846786537L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCommentLikes commentLikes = new QCommentLikes("commentLikes");

    public final QComments comment;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QCommentLikes(String variable) {
        this(CommentLikes.class, forVariable(variable), INITS);
    }

    public QCommentLikes(Path<? extends CommentLikes> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCommentLikes(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCommentLikes(PathMetadata metadata, PathInits inits) {
        this(CommentLikes.class, metadata, inits);
    }

    public QCommentLikes(Class<? extends CommentLikes> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.comment = inits.isInitialized("comment") ? new QComments(forProperty("comment"), inits.get("comment")) : null;
    }

}

