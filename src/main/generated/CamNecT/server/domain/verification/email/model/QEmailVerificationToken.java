package CamNecT.server.domain.verification.email.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEmailVerificationToken is a Querydsl query type for EmailVerificationToken
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEmailVerificationToken extends EntityPathBase<EmailVerificationToken> {

    private static final long serialVersionUID = 859168118L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEmailVerificationToken emailVerificationToken = new QEmailVerificationToken("emailVerificationToken");

    public final NumberPath<Integer> attemptCount = createNumber("attemptCount", Integer.class);

    public final StringPath codeHash = createString("codeHash");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> usedAt = createDateTime("usedAt", java.time.LocalDateTime.class);

    public final CamNecT.server.domain.users.model.QUsers user;

    public QEmailVerificationToken(String variable) {
        this(EmailVerificationToken.class, forVariable(variable), INITS);
    }

    public QEmailVerificationToken(Path<? extends EmailVerificationToken> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEmailVerificationToken(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEmailVerificationToken(PathMetadata metadata, PathInits inits) {
        this(EmailVerificationToken.class, metadata, inits);
    }

    public QEmailVerificationToken(Class<? extends EmailVerificationToken> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new CamNecT.server.domain.users.model.QUsers(forProperty("user")) : null;
    }

}

