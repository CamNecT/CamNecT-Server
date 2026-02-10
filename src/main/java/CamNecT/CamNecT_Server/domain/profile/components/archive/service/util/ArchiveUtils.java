package CamNecT.CamNecT_Server.domain.profile.components.archive.service.util;

import CamNecT.CamNecT_Server.global.storage.service.PublicUrlIssuer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveUtils {

    private final PublicUrlIssuer publicUrlIssuer;

    public String makePreview(String content, int max) {
        if (!StringUtils.hasText(content)) return null;
        String c = content.trim().replaceAll("\\s+", " ");
        if (c.length() <= max) return c;
        return c.substring(0, max) + "...";
    }

    public long safeLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }

    public String thumbnailUrlOrNull(String key) {
        if (!StringUtils.hasText(key)) return null;
        try {
            return publicUrlIssuer.issuePublicUrl(key);
        } catch (Exception e) {
            log.warn("issuePublicUrl failed. key={}", key, e);
            return null;
        }
    }
}