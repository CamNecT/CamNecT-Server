package CamNecT.server.domain.community.model.Posts;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPostTags is a Querydsl query type for PostTags
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPostTags extends EntityPathBase<PostTags> {

    private static final long serialVersionUID = 1855152916L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPostTags postTags = new QPostTags("postTags");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QPosts post;

    public final CamNecT.server.global.tag.model.QTag tag;

    public QPostTags(String variable) {
        this(PostTags.class, forVariable(variable), INITS);
    }

    public QPostTags(Path<? extends PostTags> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPostTags(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPostTags(PathMetadata metadata, PathInits inits) {
        this(PostTags.class, metadata, inits);
    }

    public QPostTags(Class<? extends PostTags> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.post = inits.isInitialized("post") ? new QPosts(forProperty("post"), inits.get("post")) : null;
        this.tag = inits.isInitialized("tag") ? new CamNecT.server.global.tag.model.QTag(forProperty("tag"), inits.get("tag")) : null;
    }

}

