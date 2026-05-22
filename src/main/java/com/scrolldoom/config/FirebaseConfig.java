package com.scrolldoom.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@Profile("!test")
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            String credentialsPath = System.getenv("FIREBASE_CREDENTIALS_PATH");
            if (credentialsPath == null || credentialsPath.isBlank()) {
                throw new IllegalStateException("FIREBASE_CREDENTIALS_PATH environment variable is not set");
            }

            try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize Firebase: " + e.getMessage(), e);
            }
        }
    }
}
