package CamNecT.server.domain.auth.dto.account;

public record FindUsernameRequest(
        String name,
        String email
) {}
