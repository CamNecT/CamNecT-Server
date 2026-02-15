package CamNecT.server.domain.auth.dto.login;

public record VerificationCompleteResponse(
        String name,
        String studentNo,
        String institutionName,
        String majorName
) {}
