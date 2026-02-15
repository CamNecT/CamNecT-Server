package CamNecT.server.domain.home.service;

import CamNecT.server.domain.activity.service.ActivityService;
import CamNecT.server.domain.alumni.service.AlumniService;
import CamNecT.server.domain.chat.service.ChatService;
import CamNecT.server.domain.home.dto.HomeResponse;
import CamNecT.server.domain.point.model.PointWallet;
import CamNecT.server.domain.point.repository.PointWalletRepository;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.CustomException;
import CamNecT.server.global.common.response.errorcode.bydomains.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final UserRepository userRepository;
    private final ChatService chatService;
    private final PointWalletRepository pointWalletRepository;
    private final AlumniService alumniService;
    private final ActivityService contestService;

    private static final int HOME_COFFEECHAT_PREVIEW_SIZE = 2;
    private static final int HOME_ALUMNI_PREVIEW_SIZE = 2;
    private static final int HOME_CONTEST_PREVIEW_SIZE = 4;

    public HomeResponse getHome(Long userId) {
        Users user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        HomeResponse.CoffeeChatSection coffeeChat =
                chatService.getHomeInbox(userId, HOME_COFFEECHAT_PREVIEW_SIZE);

        int balance = pointWalletRepository.findByUserId(userId)
                .map(PointWallet::getBalance)
                .orElse(0);

        HomeResponse.AlumniSection alumni =
                alumniService.getHomePreview(userId, HOME_ALUMNI_PREVIEW_SIZE);

        HomeResponse.ContestSection contests =
                contestService.getHomeContests(HOME_CONTEST_PREVIEW_SIZE);

        return new HomeResponse(
                new HomeResponse.UserSection(user.getName()),
                coffeeChat,
                new HomeResponse.PointSection(balance),
                alumni,
                contests
        );
    }
}