package CamNecT.server.global.jwt.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserRefreshToken is a Querydsl query type for UserRefreshToken
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserRefreshToken extends EntityPathBase<UserRefreshToken> {

    private static final long serialVersionUID = -1959225012L;

    public static final QUserRefreshToken userRefreshToken = new QUserRefreshToken("userRefreshToken");

    public final DateTimePath<java.time.Instant> expiresAt = createDateTime("expiresAt", java.time.Instant.class);

    public final StringPath refreshTokenHash = createString("refreshTokenHash");

    public final DateTimePath<java.time.Instant> updatedAt = createDateTime("updatedAt", java.time.Instant.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUserRefreshToken(String variable) {
        super(UserRefreshToken.class, forVariable(variable));
    }

    public QUserRefreshToken(Path<? extends UserRefreshToken> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserRefreshToken(PathMetadata metadata) {
        super(UserRefreshToken.class, metadata);
    }

}

