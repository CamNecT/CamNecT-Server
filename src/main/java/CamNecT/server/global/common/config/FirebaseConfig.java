package CamNecT.server.global.common.config;

import com.google.firebase.FirebaseApp;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {
    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) return;
        FirebaseApp.initializeApp();
    }
}
