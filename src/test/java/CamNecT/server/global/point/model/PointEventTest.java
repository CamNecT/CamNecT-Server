package CamNecT.server.global.point.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PointEventTest {

    @Test
    void coffeeRewardKeyIsStableAcrossRequestsButScopedToRecipientAndOpponent() {
        var first = PointEvent.coffeeChatAccepted(1L, 2L, 10L);
        assertThat(PointEvent.coffeeChatAccepted(1L, 2L, 11L).eventKey()).isEqualTo(first.eventKey());
        assertThat(PointEvent.coffeeChatAccepted(2L, 1L, 12L).eventKey()).isNotEqualTo(first.eventKey());
        assertThat(PointEvent.coffeeChatAccepted(1L, 3L, 13L).eventKey()).isNotEqualTo(first.eventKey());
        assertThat(first.requestId()).isEqualTo(10L);
        assertThat(PointEvent.coffeeChatAccepted(Long.MAX_VALUE, Long.MAX_VALUE, 1L).eventKey()).hasSizeLessThanOrEqualTo(64);
    }

    @Test
    void gifticonPurchaseKeyUsesThePersistedPurchaseIdAndFitsTheDatabaseColumn() {
        PointEvent event = PointEvent.gifticonPurchase(Long.MAX_VALUE, Long.MAX_VALUE);

        assertThat(event.eventKey())
                .isEqualTo("GIFTICON_PURCHASE:" + Long.MAX_VALUE + ":" + Long.MAX_VALUE)
                .hasSizeLessThanOrEqualTo(64);
        assertThat(event.requestId()).isEqualTo(Long.MAX_VALUE);
    }
}
