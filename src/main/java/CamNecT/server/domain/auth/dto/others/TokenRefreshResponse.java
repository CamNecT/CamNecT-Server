package CamNecT.server.domain.auth.dto.others;

public record TokenRefreshResponse(
        String tokenType,   // "Bearer"
        String accessToken,
        long accessTokenExpiresInMs,
        String refreshToken,
        long refreshTokenExpiresInMs
) {}