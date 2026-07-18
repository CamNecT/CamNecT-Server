package CamNecT.server.domain.users.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserTagMap is a Querydsl query type for UserTagMap
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserTagMap extends EntityPathBase<UserTagMap> {

    private static final long serialVersionUID = 870874158L;

    public static final QUserTagMap userTagMap = new QUserTagMap("userTagMap");

    public final NumberPath<Long> tagId = createNumber("tagId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final NumberPath<Long> userTagId = createNumber("userTagId", Long.class);

    public QUserTagMap(String variable) {
        super(UserTagMap.class, forVariable(variable));
    }

    public QUserTagMap(Path<? extends UserTagMap> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserTagMap(PathMetadata metadata) {
        super(UserTagMap.class, metadata);
    }

}

