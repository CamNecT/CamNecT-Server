package CamNecT.server.global.jwt.util;

import CamNecT.server.domain.users.model.Users;

public interface JwtFacade {
    String createAccessToken(Users user);
    String createRefreshToken(Users user);
}
