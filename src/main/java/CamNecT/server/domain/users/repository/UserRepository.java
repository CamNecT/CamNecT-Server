package CamNecT.server.domain.users.repository;

import CamNecT.server.domain.users.model.UserRole;
import CamNecT.server.domain.users.model.Users;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    Optional<Users> findByUserId(Long userId);
    Optional<Users> findByEmail(String email);
    Optional<Users> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByPhoneNum(String phoneNum);
    boolean existsByUserIdAndRole(Long userId, UserRole role);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u.userId from Users u where u.userId = :userId")
    void lockUserRow(@Param("userId") Long userId);

    @Query("select u.name from Users u where u.userId = :userId")
    Optional<String> findNameByUserId(@Param("userId") Long userId);
}
