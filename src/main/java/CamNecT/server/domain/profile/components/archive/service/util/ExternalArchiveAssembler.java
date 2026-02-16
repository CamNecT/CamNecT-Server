package CamNecT.server.domain.profile.components.archive.service.util;

import CamNecT.server.domain.activity.model.enums.ActivityCategory;
import CamNecT.server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.server.domain.activity.repository.external_activity.ExternalActivityTagRepository;
import CamNecT.server.domain.profile.components.archive.dto.response.MyArchiveResponse;
import CamNecT.server.global.storage.service.PublicUrlIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalArchiveAssembler {

    private final ExternalActivityTagRepository externalActivityTagRepository;
    private final PublicUrlIssuer publicUrlIssuer;

    public ExternalAssembleResult assemble(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ExternalAssembleResult(List.of(), null, null);
        }

        List<ExternalActivity> acts = rows.stream()
                .map(r -> (ExternalActivity) r[0])
                .toList();

        List<Long> actIds = acts.stream().map(ExternalActivity::getActivityId).distinct().toList();

        Map<Long, List<String>> tagsMap = new HashMap<>();
        externalActivityTagRepository.findAllByActivity_ActivityIdIn(actIds).forEach(at -> {
            Long aid = at.getActivity().getActivityId();
            tagsMap.computeIfAbsent(aid, k -> new ArrayList<>()).add(at.getTag().getName());
        });

        List<MyArchiveResponse.Item> items = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            ExternalActivity a = (ExternalActivity) r[0];
            long bookmarkCount = safeLong(r[1]);

            String contextDisplay = (a.getCategory() == ActivityCategory.RECRUITMENT)
                    ? a.getContextTitle() // RECRUITMENT일 때만 호출
                    : a.getContext();

            items.add(new MyArchiveResponse.ExternalActivityItem(
                    a.getActivityId(),
                    a.getTitle(),
                    contextDisplay,
                    publicUrlIssuer.issueImagePublicUrl(a.getThumbnailKey()),
                    tagsMap.getOrDefault(a.getActivityId(), List.of()),
                    bookmarkCount,
                    a.getOrganizer(),
                    a.getApplyEndDate(),
                    a.getStatus(),
                    a.getCreatedAt(),
                    a.getCategory()
            ));
        }

        Object[] lastRow = rows.getLast();
        ExternalActivity lastAct = (ExternalActivity) lastRow[0];
        long lastBookmarkCnt = safeLong(lastRow[1]); // recommended cursorValue용

        return new ExternalAssembleResult(items, lastAct.getActivityId(), lastBookmarkCnt);
    }

    public record ExternalAssembleResult(
            List<MyArchiveResponse.Item> items,
            Long lastId,
            Long lastCursorValue
    ) {}

    public long safeLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }
}