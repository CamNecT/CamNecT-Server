package CamNecT.server.domain.community.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBoards is a Querydsl query type for Boards
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBoards extends EntityPathBase<Boards> {

    private static final long serialVersionUID = -1293989715L;

    public static final QBoards boards = new QBoards("boards");

    public final EnumPath<CamNecT.server.domain.community.model.enums.BoardCode> code = createEnum("code", CamNecT.server.domain.community.model.enums.BoardCode.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public QBoards(String variable) {
        super(Boards.class, forVariable(variable));
    }

    public QBoards(Path<? extends Boards> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBoards(PathMetadata metadata) {
        super(Boards.class, metadata);
    }

}

