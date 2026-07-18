package CamNecT.server.domain.users.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUsers is a Querydsl query type for Users
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUsers extends EntityPathBase<Users> {

    private static final long serialVersionUID = 2012922951L;

    public static final QUsers users = new QUsers("users");

    public final StringPath banReason = createString("banReason");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final BooleanPath emailVerified = createBoolean("emailVerified");

    public final BooleanPath isPermanentlyBanned = createBoolean("isPermanentlyBanned");

    public final StringPath name = createString("name");

    public final StringPath passwordHash = createString("passwordHash");

    public final StringPath phoneNum = createString("phoneNum");

    public final NumberPath<Integer> reportCount = createNumber("reportCount", Integer.class);

    public final EnumPath<UserRole> role = createEnum("role", UserRole.class);

    public final EnumPath<UserStatus> status = createEnum("status", UserStatus.class);

    public final DateTimePath<java.time.LocalDateTime> suspensionEndDate = createDateTime("suspensionEndDate", java.time.LocalDateTime.class);

    public final BooleanPath termsPrivacyAgreed = createBoolean("termsPrivacyAgreed");

    public final BooleanPath termsServiceAgreed = createBoolean("termsServiceAgreed");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final StringPath username = createString("username");

    public final BooleanPath verificationCompletePending = createBoolean("verificationCompletePending");

    public QUsers(String variable) {
        super(Users.class, forVariable(variable));
    }

    public QUsers(Path<? extends Users> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUsers(PathMetadata metadata) {
        super(Users.class, metadata);
    }

}

