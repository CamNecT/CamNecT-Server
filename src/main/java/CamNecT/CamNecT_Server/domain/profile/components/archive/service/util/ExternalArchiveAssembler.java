package CamNecT.CamNecT_Server.domain.profile.components.archive.service.util;

import CamNecT.CamNecT_Server.domain.activity.model.enums.ActivityCategory;
import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.CamNecT_Server.domain.activity.repository.external_activity.ExternalActivityTagRepository;
import CamNecT.CamNecT_Server.domain.profile.components.archive.dto.response.MyArchiveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExternalArchiveAssembler {

    private static final int MAX_CONTENT = 80;

    private final ExternalActivityTagRepository externalActivityTagRepository;
    private final ArchiveUtils archiveUtils;

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
            long bookmarkCount = archiveUtils.safeLong(r[1]);

            String contextDisplay = (a.getCategory() == ActivityCategory.RECRUITMENT)
                    ? a.getContextTitle() // RECRUITMENT일 때만 호출
                    : a.getContext();

            items.add(new MyArchiveResponse.ExternalActivityItem(
                    a.getActivityId(),
                    a.getTitle(),
                    contextDisplay,
                    a.getThumbnailUrl(),
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
        long lastBookmarkCnt = archiveUtils.safeLong(lastRow[1]); // recommended cursorValue용

        return new ExternalAssembleResult(items, lastAct.getActivityId(), lastBookmarkCnt);
    }

    private static Integer computeDDayIfContest(ExternalActivity a) {
        if (a == null) return null;
        if (a.getCategory() != ActivityCategory.EXTERNAL) return null;
        if (a.getApplyEndDate() == null) return null;
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), a.getApplyEndDate());
    }

    public record ExternalAssembleResult(
            List<MyArchiveResponse.Item> items,
            Long lastId,
            Long lastCursorValue
    ) {}
}