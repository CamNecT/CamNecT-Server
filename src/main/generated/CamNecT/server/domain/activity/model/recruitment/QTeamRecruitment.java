package CamNecT.server.domain.activity.model.recruitment;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTeamRecruitment is a Querydsl query type for TeamRecruitment
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTeamRecruitment extends EntityPathBase<TeamRecruitment> {

    private static final long serialVersionUID = 1732789535L;

    public static final QTeamRecruitment teamRecruitment = new QTeamRecruitment("teamRecruitment");

    public final NumberPath<Long> activityId = createNumber("activityId", Long.class);

    public final NumberPath<Integer> bookmarkCount = createNumber("bookmarkCount", Integer.class);

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> recruitCount = createNumber("recruitCount", Integer.class);

    public final DatePath<java.time.LocalDate> recruitDeadline = createDate("recruitDeadline", java.time.LocalDate.class);

    public final NumberPath<Long> recruitId = createNumber("recruitId", Long.class);

    public final EnumPath<CamNecT.server.domain.activity.model.enums.RecruitStatus> recruitStatus = createEnum("recruitStatus", CamNecT.server.domain.activity.model.enums.RecruitStatus.class);

    public final StringPath title = createString("title");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QTeamRecruitment(String variable) {
        super(TeamRecruitment.class, forVariable(variable));
    }

    public QTeamRecruitment(Path<? extends TeamRecruitment> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTeamRecruitment(PathMetadata metadata) {
        super(TeamRecruitment.class, metadata);
    }

}

