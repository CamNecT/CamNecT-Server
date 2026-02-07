package CamNecT.CamNecT_Server.global.storage.service;

import CamNecT.CamNecT_Server.global.storage.config.CdnProps;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/*
썸네일, 프로필 이미지 등을 간단하게 내려주기 위한 구조체
 */
@Component
@RequiredArgsConstructor
public class PublicUrlIssuer {
    private final CdnProps cdnProps;

    public String issuePublicUrl(String storageKey) {
        if (!StringUtils.hasText(storageKey)) return null;

        String key = storageKey.replaceAll("^/+", "");

        // temp 노출 금지
        if (key.startsWith("temp/") || key.contains("/temp/")) return null;

        String base = trimTrailingSlash(cdnProps.baseUrl());
        if (!StringUtils.hasText(base)) return null;

        return base + "/" + key;
    }

    private String trimTrailingSlash(String s) {
        return (s == null) ? "" : s.replaceAll("/+$", "");
    }
}