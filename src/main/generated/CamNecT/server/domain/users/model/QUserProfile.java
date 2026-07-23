package CamNecT.server.domain.users.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserProfile is a Querydsl query type for UserProfile
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserProfile extends EntityPathBase<UserProfile> {

    private static final long serialVersionUID = -1827883427L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserProfile userProfile = new QUserProfile("userProfile");

    public final StringPath bio = createString("bio");

    public final BooleanPath initialSetupCompleted = createBoolean("initialSetupCompleted");

    public final NumberPath<Long> institutionId = createNumber("institutionId", Long.class);

    public final BooleanPath isCertificateVisible = createBoolean("isCertificateVisible");

    public final BooleanPath isEducationVisible = createBoolean("isEducationVisible");

    public final BooleanPath isExperienceVisible = createBoolean("isExperienceVisible");

    public final BooleanPath isFollowerVisible = createBoolean("isFollowerVisible");

    public final CamNecT.server.domain.profile.components.majors.model.QMajors major;

    public final NumberPath<Long> majorId = createNumber("majorId", Long.class);

    public final BooleanPath openToCoffeeChat = createBoolean("openToCoffeeChat");

    public final StringPath profileImageKey = createString("profileImageKey");

    public final StringPath studentNo = createString("studentNo");

    public final QUsers user;

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<Integer> yearLevel = createNumber("yearLevel", Integer.class);

    public QUserProfile(String variable) {
        this(UserProfile.class, forVariable(variable), INITS);
    }

    public QUserProfile(Path<? extends UserProfile> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserProfile(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserProfile(PathMetadata metadata, PathInits inits) {
        this(UserProfile.class, metadata, inits);
    }

    public QUserProfile(Class<? extends UserProfile> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.major = inits.isInitialized("major") ? new CamNecT.server.domain.profile.components.majors.model.QMajors(forProperty("major"), inits.get("major")) : null;
        this.user = inits.isInitialized("user") ? new QUsers(forProperty("user"), inits.get("user")) : null;
    }

}

