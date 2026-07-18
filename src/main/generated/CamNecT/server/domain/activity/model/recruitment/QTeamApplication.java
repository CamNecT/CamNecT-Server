package CamNecT.server.domain.activity.model.recruitment;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTeamApplication is a Querydsl query type for TeamApplication
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTeamApplication extends EntityPathBase<TeamApplication> {

    private static final long serialVersionUID = -1788707629L;

    public static final QTeamApplication teamApplication = new QTeamApplication("teamApplication");

    public final NumberPath<Long> applicationId = createNumber("applicationId", Long.class);

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> recruitId = createNumber("recruitId", Long.class);

    public final EnumPath<CamNecT.server.domain.activity.model.enums.ApplicationStatus> status = createEnum("status", CamNecT.server.domain.activity.model.enums.ApplicationStatus.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QTeamApplication(String variable) {
        super(TeamApplication.class, forVariable(variable));
    }

    public QTeamApplication(Path<? extends TeamApplication> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTeamApplication(PathMetadata metadata) {
        super(TeamApplication.class, metadata);
    }

}

