package CamNecT.server.global.notification.service;

import CamNecT.server.global.notification.model.Platform;
import CamNecT.server.global.notification.model.PushDevice;
import CamNecT.server.global.notification.repository.PushDeviceRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PushDeviceServiceTest {

    private final PushDeviceRepository pushDeviceRepository = mock(PushDeviceRepository.class);
    private final PushDeviceService pushDeviceService = new PushDeviceService(pushDeviceRepository);

    @Test
    void findEnabledTokensRemovesDuplicateTokens() {
        when(pushDeviceRepository.findAllByUserIdAndEnabledTrue(1L)).thenReturn(List.of(
                device("token-a"),
                device("token-a"),
                device("token-b")
        ));

        assertThat(pushDeviceService.findEnabledTokens(1L))
                .containsExactly("token-a", "token-b");
    }

    private PushDevice device(String token) {
        return PushDevice.builder()
                .userId(1L)
                .deviceId("device-" + token)
                .platform(Platform.WEB)
                .fcmToken(token)
                .enabled(true)
                .build();
    }
}
