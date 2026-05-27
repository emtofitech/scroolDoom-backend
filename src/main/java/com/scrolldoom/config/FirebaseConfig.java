package com.scrolldoom.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("!test")
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            String credentialsJson = System.getenv("FIREBASE_CREDENTIALS_JSON");
            String credentialsPath = System.getenv("FIREBASE_CREDENTIALS_PATH");

            try {
                GoogleCredentials credentials;

                if (credentialsJson != null && !credentialsJson.isBlank()) {
                    credentials = GoogleCredentials.fromStream(
                            new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));
                } else if (credentialsPath != null && !credentialsPath.isBlank()) {
                    credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
                } else {
                    throw new IllegalStateException(
                            "Set FIREBASE_CREDENTIALS_JSON (raw JSON) or FIREBASE_CREDENTIALS_PATH (file path)");
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize Firebase: " + e.getMessage(), e);
            }
        }
    }
}
