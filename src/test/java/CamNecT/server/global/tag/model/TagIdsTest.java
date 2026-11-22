package CamNecT.server.global.tag.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TagIdsTest {
    @Test
    void filtersAndRequestsDropNullsAndKeepFirstOccurrenceAfterMapping() {
        assertThat(TagIds.distinctCanonicalIds(null)).isEmpty();
        assertThat(TagIds.distinctCanonicalIds(Arrays.asList(4L, 111L, null, 53L, 5L, 4L)))
                .containsExactly(4L, 53L, 5L);
    }
}
