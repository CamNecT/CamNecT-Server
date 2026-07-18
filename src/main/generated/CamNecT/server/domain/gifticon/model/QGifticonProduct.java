package CamNecT.server.domain.gifticon.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGifticonProduct is a Querydsl query type for GifticonProduct
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGifticonProduct extends EntityPathBase<GifticonProduct> {

    private static final long serialVersionUID = 343881618L;

    public static final QGifticonProduct gifticonProduct = new QGifticonProduct("gifticonProduct");

    public final StringPath brandName = createString("brandName");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final BooleanPath isActive = createBoolean("isActive");

    public final DateTimePath<java.time.LocalDateTime> lastSyncedAt = createDateTime("lastSyncedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> pricePoints = createNumber("pricePoints", Integer.class);

    public final StringPath productName = createString("productName");

    public final NumberPath<Integer> sortScore = createNumber("sortScore", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final StringPath vendorProductCode = createString("vendorProductCode");

    public QGifticonProduct(String variable) {
        super(GifticonProduct.class, forVariable(variable));
    }

    public QGifticonProduct(Path<? extends GifticonProduct> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGifticonProduct(PathMetadata metadata) {
        super(GifticonProduct.class, metadata);
    }

}

