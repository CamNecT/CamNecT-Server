package CamNecT.CamNecT_Server.domain.community.service;


import CamNecT.CamNecT_Server.domain.community.dto.AuthorDto;
import CamNecT.CamNecT_Server.domain.community.dto.request.CreateCommentRequest;
import CamNecT.CamNecT_Server.domain.community.dto.request.UpdateCommentRequest;
import CamNecT.CamNecT_Server.domain.community.dto.response.CreateCommentResponse;
import CamNecT.CamNecT_Server.domain.community.dto.response.ToggleCommentLikeResponse;

import java.util.List;

public interface CommentService {

    CreateCommentResponse create(Long userId, Long postId, CreateCommentRequest req);

    void update(Long userId, Long commentId, UpdateCommentRequest req);

    void delete(Long userId, Long commentId);

    ToggleCommentLikeResponse toggleLike(Long userId, Long commentId);

    List<CommentRow> list(Long postId, int size);

    record CommentRow(
            Long commentId,
            Long userId,
            Long parentCommentId,
            String content,
            long likeCount,
            AuthorDto author
    ) {}
}
