package CamNecT.server.global.notification.repository;

import CamNecT.server.global.notification.model.PushDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {

    Optional<PushDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<PushDevice> findAllByUserIdAndEnabledTrue(Long userId);

    List<PushDevice> findAllByFcmTokenIn(Collection<String> tokens);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PushDevice p set p.enabled = false where p.fcmToken = :token and p.userId <> :userId")
    int disableTokenForOtherUsers(@Param("token") String token, @Param("userId") Long userId);
}
