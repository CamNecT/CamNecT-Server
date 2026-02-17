package CamNecT.server.global.jwt.repository;

import CamNecT.server.global.jwt.model.UserRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {
}