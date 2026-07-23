package CamNecT.server.domain.users.repository;

import CamNecT.server.domain.users.model.UserSuspensionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSuspensionRecordRepository extends JpaRepository<UserSuspensionRecord, Long> {
    Optional<UserSuspensionRecord> findByUser_UserId(Long userId);
}
