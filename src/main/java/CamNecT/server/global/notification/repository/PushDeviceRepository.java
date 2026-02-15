package CamNecT.server.global.notification.repository;

import CamNecT.server.global.notification.model.PushDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {

    Optional<PushDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<PushDevice> findAllByUserIdAndEnabledTrue(Long userId);

    List<PushDevice> findAllByFcmTokenIn(Collection<String> tokens);
}

