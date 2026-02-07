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

        String base = trimTrailingSlash(cdnProps.baseUrl());
        if (!StringUtils.hasText(base)) return null; // CDN 미설정이면 null 처리(개발/로컬)

        String key = storageKey.replaceAll("^/+", "");
        return base + "/" + key;
    }

    private String trimTrailingSlash(String s) {
        return (s == null) ? "" : s.replaceAll("/+$", "");
    }
}