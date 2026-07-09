package CamNecT.server.domain.auth.service;

import CamNecT.server.domain.auth.dto.account.FindUsernameRequest;
import CamNecT.server.domain.auth.dto.account.FindUsernameResponse;
import CamNecT.server.domain.users.model.Users;
import CamNecT.server.domain.users.repository.UserRepository;
import CamNecT.server.global.common.exception.InvalidPropertiesException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountRecoveryService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public FindUsernameResponse findUsername(FindUsernameRequest req) {
        Users user = findUserForUsernameRecovery(req.name(), req.email());
        return new FindUsernameResponse(user.getUsername());
    }

    @Transactional(readOnly = true)
    public Users findUserForUsernameRecovery(String rawName, String rawEmail) {
        String name = normalize(rawName);
        String email = normalize(rawEmail);

        List<String> invalidProperties = new ArrayList<>();
        if (name.isBlank() || !userRepository.existsByName(name)) {
            invalidProperties.add("name");
        }
        if (email.isBlank() || !userRepository.existsByEmail(email)) {
            invalidProperties.add("email");
        }
        if (!invalidProperties.isEmpty()) {
            throw new InvalidPropertiesException(invalidProperties);
        }

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidPropertiesException(List.of("email")));

        if (!name.equals(user.getName())) {
            throw new InvalidPropertiesException(List.of("name"));
        }

        return user;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
