package CamNecT.CamNecT_Server.domain.community.service;

import CamNecT.CamNecT_Server.domain.community.dto.response.CommunityHomeResponse;

public interface CommunityHomeService {
    CommunityHomeResponse getHome(Long tagId);
}
