package CamNecT.server.domain.gifticon.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QGifticonExportBatch is a Querydsl query type for GifticonExportBatch
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGifticonExportBatch extends EntityPathBase<GifticonExportBatch> {

    private static final long serialVersionUID = -1498913527L;

    public static final QGifticonExportBatch gifticonExportBatch = new QGifticonExportBatch("gifticonExportBatch");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> exportedAt = createDateTime("exportedAt", java.time.LocalDateTime.class);

    public final StringPath fileName = createString("fileName");

    public final StringPath filePath = createString("filePath");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> itemCount = createNumber("itemCount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QGifticonExportBatch(String variable) {
        super(GifticonExportBatch.class, forVariable(variable));
    }

    public QGifticonExportBatch(Path<? extends GifticonExportBatch> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGifticonExportBatch(PathMetadata metadata) {
        super(GifticonExportBatch.class, metadata);
    }

}

