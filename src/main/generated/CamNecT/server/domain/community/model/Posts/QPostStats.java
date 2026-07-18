package CamNecT.server.domain.community.model.Posts;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostStats is a Querydsl query type for PostStats
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostStats extends EntityPathBase<PostStats> {

    private static final long serialVersionUID = 1674802436L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostStats postStats = new QPostStats("postStats");

    public final NumberPath<Long> bookmarkCount = createNumber("bookmarkCount", Long.class);

    public final NumberPath<Long> commentCount = createNumber("commentCount", Long.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> hotScore = createNumber("hotScore", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastActivityAt = createDateTime("lastActivityAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> likeCount = createNumber("likeCount", Long.class);

    public final BooleanPath likeRewarded3 = createBoolean("likeRewarded3");

    public final QPosts post;

    public final NumberPath<Long> rootCommentCount = createNumber("rootCommentCount", Long.class);

    public final NumberPath<Long> viewCount = createNumber("viewCount", Long.class);

    public QPostStats(String variable) {
        this(PostStats.class, forVariable(variable), INITS);
    }

    public QPostStats(Path<? extends PostStats> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostStats(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostStats(PathMetadata metadata, PathInits inits) {
        this(PostStats.class, metadata, inits);
    }

    public QPostStats(Class<? extends PostStats> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new QPosts(forProperty("post"), inits.get("post")) : null;
    }

}

