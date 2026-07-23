package CamNecT.server.domain.profile.components.experience.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QExperience is a Querydsl query type for Experience
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QExperience extends EntityPathBase<Experience> {

    private static final long serialVersionUID = -117369226L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QExperience experience = new QExperience("experience");

    public final StringPath companyName = createString("companyName");

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final NumberPath<Long> experienceId = createNumber("experienceId", Long.class);

    public final BooleanPath isCurrent = createBoolean("isCurrent");

    public final ListPath<String, StringPath> responsibilities = this.<String, StringPath>createList("responsibilities", String.class, StringPath.class, PathInits.DIRECT2);

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final CamNecT.server.domain.users.model.QUsers user;

    public QExperience(String variable) {
        this(Experience.class, forVariable(variable), INITS);
    }

    public QExperience(Path<? extends Experience> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QExperience(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QExperience(PathMetadata metadata, PathInits inits) {
        this(Experience.class, metadata, inits);
    }

    public QExperience(Class<? extends Experience> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new CamNecT.server.domain.users.model.QUsers(forProperty("user"), inits.get("user")) : null;
    }

}

