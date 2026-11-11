package CamNecT.server.domain.preview;

import CamNecT.server.domain.activity.model.enums.RecruitStatus;
import CamNecT.server.domain.community.model.Posts.Posts;
import CamNecT.server.domain.community.model.enums.BoardCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface GuestPreviewRepository extends Repository<Posts, Long> {
    interface PostSummary {
        Long getId();
        String getTitle();
        long getLikeCount();
        long getCommentCount();
    }

    interface RecruitmentSummary {
        Long getId();
        String getTitle();
        RecruitStatus getStatus();
    }

    @Query("""
            select p.id as id, p.title as title, coalesce(s.likeCount, 0) as likeCount,
                   coalesce(s.commentCount, 0) as commentCount
            from Posts p left join p.stats s
            where p.status = CamNecT.server.domain.community.model.enums.PostStatus.PUBLISHED
              and (:board is null or p.board.code = :board)
            order by p.id desc
            """)
    Slice<PostSummary> posts(BoardCode board, Pageable page);

    @Query("""
            select r.recruitId as id, r.title as title, r.recruitStatus as status
            from TeamRecruitment r where (:activityId is null or r.activityId = :activityId)
            order by r.recruitId desc
            """)
    Slice<RecruitmentSummary> recruitments(Long activityId, Pageable page);

    @Query("""
            select count(p) from UserProfile p
            where p.user.status = CamNecT.server.domain.users.model.UserStatus.ACTIVE
            """)
    long alumniCount();
}
