package CamNecT.server.global.notification.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPushDevice is a Querydsl query type for PushDevice
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPushDevice extends EntityPathBase<PushDevice> {

    private static final long serialVersionUID = 1004507297L;

    public static final QPushDevice pushDevice = new QPushDevice("pushDevice");

    public final StringPath deviceId = createString("deviceId");

    public final BooleanPath enabled = createBoolean("enabled");

    public final StringPath fcmToken = createString("fcmToken");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastSeenAt = createDateTime("lastSeenAt", java.time.LocalDateTime.class);

    public final EnumPath<Platform> platform = createEnum("platform", Platform.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QPushDevice(String variable) {
        super(PushDevice.class, forVariable(variable));
    }

    public QPushDevice(Path<? extends PushDevice> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPushDevice(PathMetadata metadata) {
        super(PushDevice.class, metadata);
    }

}

