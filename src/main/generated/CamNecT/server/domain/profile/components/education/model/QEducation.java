package CamNecT.server.domain.profile.components.education.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEducation is a Querydsl query type for Education
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEducation extends EntityPathBase<Education> {

    private static final long serialVersionUID = 42133120L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEducation education = new QEducation("education");

    public final StringPath degree = createString("degree");

    public final StringPath description = createString("description");

    public final NumberPath<Long> educationId = createNumber("educationId", Long.class);

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    public final CamNecT.server.domain.profile.components.institutions.model.QInstitutions institution;

    public final CamNecT.server.domain.profile.components.majors.model.QMajors major;

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final EnumPath<EducationStatus> status = createEnum("status", EducationStatus.class);

    public final CamNecT.server.domain.users.model.QUsers user;

    public QEducation(String variable) {
        this(Education.class, forVariable(variable), INITS);
    }

    public QEducation(Path<? extends Education> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEducation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEducation(PathMetadata metadata, PathInits inits) {
        this(Education.class, metadata, inits);
    }

    public QEducation(Class<? extends Education> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.institution = inits.isInitialized("institution") ? new CamNecT.server.domain.profile.components.institutions.model.QInstitutions(forProperty("institution")) : null;
        this.major = inits.isInitialized("major") ? new CamNecT.server.domain.profile.components.majors.model.QMajors(forProperty("major"), inits.get("major")) : null;
        this.user = inits.isInitialized("user") ? new CamNecT.server.domain.users.model.QUsers(forProperty("user"), inits.get("user")) : null;
    }

}

