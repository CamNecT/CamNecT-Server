package CamNecT.server.domain.activity.model.recruitment;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRecruitmentBookmark is a Querydsl query type for RecruitmentBookmark
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRecruitmentBookmark extends EntityPathBase<RecruitmentBookmark> {

    private static final long serialVersionUID = 1452648722L;

    public static final QRecruitmentBookmark recruitmentBookmark = new QRecruitmentBookmark("recruitmentBookmark");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> recruitId = createNumber("recruitId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QRecruitmentBookmark(String variable) {
        super(RecruitmentBookmark.class, forVariable(variable));
    }

    public QRecruitmentBookmark(Path<? extends RecruitmentBookmark> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRecruitmentBookmark(PathMetadata metadata) {
        super(RecruitmentBookmark.class, metadata);
    }

}

