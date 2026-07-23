package CamNecT.server.global.common.auth;

import CamNecT.server.domain.users.model.UserStatus;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountAccessGuard {

    private final UserRepository userRepository;

    public Users requireAccessible(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.INVALID_TOKEN));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(AuthErrorCode.USER_SUSPENDED);
        }
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(AuthErrorCode.USER_WITHDRAWN);
        }
        return user;
    }

    public void requireActive(Long userId) {
        Users user = requireAccessible(userId);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new CustomException(AuthErrorCode.ACTIVE_ACCOUNT_REQUIRED);
        }
    }
}
