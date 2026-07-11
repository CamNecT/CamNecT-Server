package CamNecT.server.global.notification.service;

import CamNecT.server.domain.users.model.UserRole;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.ErrorCode;
import CamNecT.server.global.common.response.errorcode.bydomains.UserErrorCode;
import CamNecT.server.global.notification.dto.request.AdminAnnouncementRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
        validateAdmin(adminUserId);
        validate(request);

        if (request.targetType() == USERS) {
            List<Long> receiverIds = resolveSelectedUsers(request.targetUserIds());
            return adminAnnouncementBatchService.dispatch(adminUserId, request, receiverIds);
        }

        long total = 0L;
        int page = 0;

        while (true) {
            // TODO: 추후에는 findAll(Pageable) 대신 "활성 사용자만" 조회하는 쿼리로 교체
            Page<Users> result = userRepository.findAll(PageRequest.of(page++, BATCH_SIZE));
            if (result.isEmpty()) {
                break;
            }

            List<Long> receiverIds = result.getContent().stream()
                    .map(Users::getUserId)
                    .toList();

            total += adminAnnouncementBatchService.dispatch(adminUserId, request, receiverIds);

            if (!result.hasNext()) {
                break;
            }
        }

        log.info("[admin-announcement] sent by admin={}, total={}", adminUserId, total);
        return total;
    }

    private void validate(AdminAnnouncementRequest request) {
        if (request.targetType() == USERS &&
                (request.targetUserIds() == null || request.targetUserIds().isEmpty())) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validateAdmin(Long adminUserId) {
        if (!userRepository.existsByUserIdAndRole(adminUserId, UserRole.ADMIN)) {
            throw new CustomException(UserErrorCode.USER_NOT_ADMIN);
        }
    }

    private List<Long> resolveSelectedUsers(List<Long> targetUserIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>(targetUserIds);

        return userRepository.findAllById(uniqueIds).stream()
                .map(Users::getUserId)
                .toList();
    }
}
