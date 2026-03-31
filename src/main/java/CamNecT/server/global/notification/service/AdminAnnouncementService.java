package CamNecT.server.global.notification.service;

import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.notification.dto.request.AdminAnnouncementRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static CamNecT.server.global.notification.dto.request.AdminAnnouncementRequest.TargetType.USERS;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnnouncementService {

    private static final int BATCH_SIZE = 500;

    private final UserRepository userRepository;
    private final AdminAnnouncementBatchService adminAnnouncementBatchService;

    public long send(Long adminUserId, AdminAnnouncementRequest request) {
        validate(request);

        if (request.targetType() == USERS) {
            List<Long> receiverIds = resolveSelectedUsers(request.targetUserIds());
            if (receiverIds.isEmpty()) return 0L;
            return adminAnnouncementBatchService.dispatch(adminUserId, request, receiverIds);
        }

        long total = 0L;
        int page = 0;

        while (true) {
            Slice<Long> result = userRepository.findUserIdsByStatus(UserStatus.ACTIVE, PageRequest.of(page++, BATCH_SIZE));
            if (result.isEmpty()) break;

            total += adminAnnouncementBatchService.dispatch(
                    adminUserId,
                    request,
                    result.getContent()
            );

            if (!result.hasNext()) break;
        }

        log.info("[admin-announcement] sent by admin={}, total={}", adminUserId, total);
        return total;
    }

    private void validate(AdminAnnouncementRequest request) {
        if (request.targetType() == USERS &&
                (request.targetUserIds() == null || request.targetUserIds().isEmpty())) {
            throw new IllegalArgumentException("targetUserIds is required when targetType=USERS");
            // 프로젝트 스타일대로 가려면 ErrorCode 하나 추가해서 CustomException으로 바꾸면 됨
        }
    }

    private List<Long> resolveSelectedUsers(List<Long> targetUserIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>(targetUserIds);

        return userRepository.findAllById(uniqueIds).stream()
                .map(Users::getUserId)
                .toList();
    }
}