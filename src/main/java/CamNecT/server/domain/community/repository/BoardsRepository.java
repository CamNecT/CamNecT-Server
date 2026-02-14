package CamNecT.server.domain.community.repository;

import CamNecT.server.domain.community.model.Boards;
import CamNecT.server.domain.community.model.enums.BoardCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardsRepository extends JpaRepository<Boards, Long> {
    Optional<Boards> findByCode(BoardCode code);
    boolean existsByCode(BoardCode code);
}