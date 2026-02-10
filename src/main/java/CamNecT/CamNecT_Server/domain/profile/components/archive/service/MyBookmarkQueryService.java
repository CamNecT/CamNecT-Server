package CamNecT.CamNecT_Server.domain.profile.components.archive.service;

import CamNecT.CamNecT_Server.domain.profile.components.archive.dto.response.MyArchiveResponse;

public interface MyBookmarkQueryService {
    MyArchiveResponse getBookmarks(
            Long userId,
            MyArchiveResponse.Tab tab,
            MyArchiveResponse.Sort sort,
            Long cursorId,
            Long cursorValue,
            int size
    );
}