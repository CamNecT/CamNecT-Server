package CamNecT.server.global.tag.model;

import java.util.List;
import java.util.Objects;

public final class TagIds {
    private TagIds() {}

    public static long canonical(long id) {
        return id == 111L ? 53L : id;
    }

    public static List<Long> distinctCanonicalIds(List<Long> ids) {
        if (ids == null) return List.of();
        return ids.stream().filter(Objects::nonNull).map(TagIds::canonical).distinct().toList();
    }
}
