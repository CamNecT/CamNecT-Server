package CamNecT.server.domain.verification.document.dto;

public record DocumentVerificationFileDto(
        String originalFilename,
        String contentType,
        long size,
        String storageKey
) {}