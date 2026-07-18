package CamNecT.server.domain.users.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserFollow is a Querydsl query type for UserFollow
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserFollow extends EntityPathBase<UserFollow> {

    private static final long serialVersionUID = 483174525L;

    public static final QUserFollow userFollow = new QUserFollow("userFollow");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> followerId = createNumber("followerId", Long.class);

    public final NumberPath<Long> followId = createNumber("followId", Long.class);

    public final NumberPath<Long> followingId = createNumber("followingId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QUserFollow(String variable) {
        super(UserFollow.class, forVariable(variable));
    }

    public QUserFollow(Path<? extends UserFollow> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserFollow(PathMetadata metadata) {
        super(UserFollow.class, metadata);
    }

}

