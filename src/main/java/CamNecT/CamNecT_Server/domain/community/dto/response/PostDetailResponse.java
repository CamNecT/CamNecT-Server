package CamNecT.CamNecT_Server.domain.community.dto.response;

import CamNecT.CamNecT_Server.domain.community.dto.AuthorDto;
import CamNecT.CamNecT_Server.domain.community.model.enums.BoardCode;
import CamNecT.CamNecT_Server.domain.community.model.enums.ContentAccessStatus;

import java.util.List;

public record PostDetailResponse(
        Long postId,
        BoardCode boardCode,
        String title,
        String content,
        boolean anonymous,
        long viewCount,
        long likeCount,
        boolean likedByMe,
        Long acceptedCommentId,
        List<Long> tagIds,
        List<PostAttachmentItemResponse> attachments,
        ContentAccessStatus accessStatus,
        boolean bookmarked,
        Integer requiredPoints,
        Integer myPoints,
        AuthorDto author
) {}
