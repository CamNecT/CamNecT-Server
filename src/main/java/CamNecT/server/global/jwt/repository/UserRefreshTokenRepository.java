package CamNecT.server.global.jwt.repository;

import CamNecT.server.global.jwt.model.UserRefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from UserRefreshToken t where t.userId = :userId")
    Optional<UserRefreshToken> findByIdForUpdate(@Param("userId") Long userId);
}