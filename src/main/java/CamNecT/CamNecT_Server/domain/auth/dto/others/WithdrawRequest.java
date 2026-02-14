package CamNecT.CamNecT_Server.domain.auth.dto.others;

import jakarta.validation.constraints.NotBlank;

public record WithdrawRequest(
        @NotBlank String password
) {}