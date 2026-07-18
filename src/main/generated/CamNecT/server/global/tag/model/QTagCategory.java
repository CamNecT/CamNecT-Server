package CamNecT.server.global.tag.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTagCategory is a Querydsl query type for TagCategory
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTagCategory extends EntityPathBase<TagCategory> {

    private static final long serialVersionUID = 202275560L;

    public static final QTagCategory tagCategory = new QTagCategory("tagCategory");

    public final BooleanPath active = createBoolean("active");

    public final StringPath code = createString("code");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> sortOrder = createNumber("sortOrder", Integer.class);

    public QTagCategory(String variable) {
        super(TagCategory.class, forVariable(variable));
    }

    public QTagCategory(Path<? extends TagCategory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTagCategory(PathMetadata metadata) {
        super(TagCategory.class, metadata);
    }

}

