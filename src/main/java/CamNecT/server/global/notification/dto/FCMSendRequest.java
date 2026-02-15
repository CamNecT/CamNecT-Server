package CamNecT.server.global.notification.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record FCMSendRequest (
        @NotBlank String token,
        @NotBlank String title,
        @NotBlank String body,
        Map<String, String> data
){
}
