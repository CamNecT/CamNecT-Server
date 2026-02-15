package CamNecT.server.domain.community.service;

import CamNecT.server.domain.community.dto.response.CommunityHomeResponse;

public interface CommunityHomeService {
    CommunityHomeResponse getHome(Long tagId);
}
