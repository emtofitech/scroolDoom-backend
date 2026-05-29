package com.scrolldoom.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("!test")
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @PostConstruct
    public void initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            String credentialsJson = System.getenv("FIREBASE_CREDENTIALS_JSON");
            String credentialsPath = System.getenv("FIREBASE_CREDENTIALS_PATH");

            try {
                GoogleCredentials credentials = null;

                if (credentialsJson != null && !credentialsJson.isBlank()) {
                    log.info("Initializing Firebase with credentials from FIREBASE_CREDENTIALS_JSON");
                    credentials = GoogleCredentials.fromStream(
                            new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));
                } else if (credentialsPath != null && !credentialsPath.isBlank()) {
                    log.info("Initializing Firebase with credentials from path: {}", credentialsPath);
                    credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath));
                } else {
                    // Try default location
                    java.io.File defaultFile = new java.io.File("firebase-service-account.json");
                    if (defaultFile.exists()) {
                        log.info("Initializing Firebase with credentials from default file: firebase-service-account.json");
                        credentials = GoogleCredentials.fromStream(new FileInputStream(defaultFile));
                    } else {
                        // Try classpath
                        java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
                        if (is != null) {
                            log.info("Initializing Firebase with credentials from classpath: firebase-service-account.json");
                            credentials = GoogleCredentials.fromStream(is);
                        } else {
                            try {
                                log.info("Attempting to initialize Firebase with Application Default Credentials");
                                credentials = GoogleCredentials.getApplicationDefault();
                            } catch (IOException e) {
                                log.warn("No Firebase credentials found and Application Default Credentials not available. Firebase auth disabled. " +
                                        "Set FIREBASE_CREDENTIALS_JSON, FIREBASE_CREDENTIALS_PATH, or provide firebase-service-account.json to enable.");
                                return;
                            }
                        }
                    }
                }

                if (credentials != null) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .build();

                    FirebaseApp.initializeApp(options);
                    log.info("Firebase initialized successfully");
                }
            } catch (IOException e) {
                log.error("Failed to initialize Firebase: {}", e.getMessage());
                // Don't throw exception to allow the app to start without Firebase
            }
        }
    }
}
