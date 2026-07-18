package CamNecT.server.domain.profile.components.institutions.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QInstitutions is a Querydsl query type for Institutions
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInstitutions extends EntityPathBase<Institutions> {

    private static final long serialVersionUID = 1877529366L;

    public static final QInstitutions institutions = new QInstitutions("institutions");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath institutionCode = createString("institutionCode");

    public final NumberPath<Long> institutionId = createNumber("institutionId", Long.class);

    public final StringPath institutionNameEng = createString("institutionNameEng");

    public final StringPath institutionNameKor = createString("institutionNameKor");

    public final BooleanPath isActive = createBoolean("isActive");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QInstitutions(String variable) {
        super(Institutions.class, forVariable(variable));
    }

    public QInstitutions(Path<? extends Institutions> path) {
        super(path.getType(), path.getMetadata());
    }

    public QInstitutions(PathMetadata metadata) {
        super(Institutions.class, metadata);
    }

}

