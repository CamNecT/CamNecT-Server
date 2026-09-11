package CamNecT.server.global.common.auth;

import CamNecT.server.global.jwt.util.JwtUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class VerificationTokenGuardTest {

    private final VerificationTokenGuard guard = new VerificationTokenGuard(mock(JwtUtil.class));

    @ParameterizedTest
    @CsvSource({
            "GET,/api/tags,true",
            "POST,/api/profile/uploads/presign,true",
            "POST,/api/auth/onboarding,true",
            "POST,/api/verification/documents,true",
            "POST,/api/verification/documents/uploads/presign,true",
            "GET,/api/verification/documents/me,true",
            "GET,/api/verification/documents/10,true",
            "GET,/api/verification/documents/10/download-url,true",
            "DELETE,/api/verification/documents/10,true",
            "POST,/api/tags,false",
            "GET,/api/profile/uploads/presign,false",
            "PATCH,/api/profile/me/image,false",
            "PUT,/api/profile/tags,false",
            "GET,/api/auth/onboarding,false",
            "POST,/api/auth/onboarding/extra,false",
            "POST,/api/auth/refresh,false",
            "GET,/api/verification/documents/admin,false",
            "PATCH,/api/verification/documents/10,false",
            "GET,/api/admin/verification/documents,false"
    })
    void allowsOnlyTheSignupMethodsAndPaths(String method, String path, boolean allowed) {
        assertThat(guard.isAllowed(new MockHttpServletRequest(method, path))).isEqualTo(allowed);
    }
}
