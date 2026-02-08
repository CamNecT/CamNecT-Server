package CamNecT.CamNecT_Server.domain.gifticon.repository;

import CamNecT.CamNecT_Server.domain.gifticon.model.GifticonBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GifticonBookmarkRepository extends JpaRepository<GifticonBookmark, Long> {

    Optional<GifticonBookmark> findByUser_UserIdAndProduct_Id(Long userId, Long productId);

    boolean existsByUser_UserIdAndProduct_Id(Long userId, Long productId);

    List<GifticonBookmark> findAllByUser_UserId(Long userId);
}