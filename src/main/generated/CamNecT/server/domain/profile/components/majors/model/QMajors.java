package CamNecT.server.domain.profile.components.majors.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMajors is a Querydsl query type for Majors
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMajors extends EntityPathBase<Majors> {

    private static final long serialVersionUID = -376149002L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMajors majors = new QMajors("majors");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final CamNecT.server.domain.profile.components.institutions.model.QInstitutions institution;

    public final BooleanPath isActive = createBoolean("isActive");

    public final StringPath majorCode = createString("majorCode");

    public final NumberPath<Long> majorId = createNumber("majorId", Long.class);

    public final StringPath majorNameEng = createString("majorNameEng");

    public final StringPath majorNameKor = createString("majorNameKor");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QMajors(String variable) {
        this(Majors.class, forVariable(variable), INITS);
    }

    public QMajors(Path<? extends Majors> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMajors(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMajors(PathMetadata metadata, PathInits inits) {
        this(Majors.class, metadata, inits);
    }

    public QMajors(Class<? extends Majors> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.institution = inits.isInitialized("institution") ? new CamNecT.server.domain.profile.components.institutions.model.QInstitutions(forProperty("institution")) : null;
    }

}

