package CamNecT.CamNecT_Server.domain.profile.components.archive.service.util;

import CamNecT.CamNecT_Server.domain.activity.model.external_activity.ExternalActivity;
import CamNecT.CamNecT_Server.domain.activity.model.recruitment.TeamRecruitment;
import CamNecT.CamNecT_Server.domain.activity.repository.external_activity.ExternalActivityRepository;
import CamNecT.CamNecT_Server.domain.profile.components.archive.dto.response.MyArchiveResponse;
import CamNecT.CamNecT_Server.domain.users.model.Users;
import CamNecT.CamNecT_Server.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentArchiveAssembler {

    private final ExternalActivityRepository externalActivityRepository;
    private final UserRepository userRepository;
    private final ArchiveUtils archiveUtils;

    public RecruitmentAssembleResult assemble(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return new RecruitmentAssembleResult(List.of(), null, null);
        }

        List<TeamRecruitment> recruits = rows.stream()
                .map(r -> (TeamRecruitment) r[0])
                .toList();

        Set<Long> activityIds = recruits.stream()
                .map(TeamRecruitment::getActivityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> activityTitleMap = externalActivityRepository.findAllById(activityIds).stream()
                .collect(Collectors.toMap(ExternalActivity::getActivityId, ExternalActivity::getTitle));

        Set<Long> authorIds = recruits.stream()
                .map(TeamRecruitment::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> authorNameMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(Users::getUserId, Users::getName));

        List<MyArchiveResponse.Item> items = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            TeamRecruitment tr = (TeamRecruitment) r[0];
            long bookmarkCount = archiveUtils.safeLong(r[1]);

            items.add(new MyArchiveResponse.RecruitmentItem(
                    tr.getRecruitId(),
                    activityTitleMap.get(tr.getActivityId()),
                    authorNameMap.get(tr.getUserId()),
                    tr.getRecruitStatus().name(),
                    tr.getTitle(),
                    tr.getContent(),
                    tr.getRecruitDeadline(),
                    tr.getRecruitCount(),
                    bookmarkCount,
                    tr.getCreatedAt(),
                    tr.getUpdatedAt()
            ));
        }

        Object[] lastRow = rows.getLast();
        TeamRecruitment last = (TeamRecruitment) lastRow[0];
        long lastCursorVal = archiveUtils.safeLong(lastRow[1]);

        return new RecruitmentAssembleResult(items, last.getRecruitId(), lastCursorVal);
    }

    public record RecruitmentAssembleResult(
            List<MyArchiveResponse.Item> items,
            Long lastId,
            Long lastCursorValue
    ) {}
}