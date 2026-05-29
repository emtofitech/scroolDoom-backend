package com.scrolldoom.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.scrolldoom.dto.LoginWithPasswordResponse;
import com.scrolldoom.dto.RefreshTokenResponse;
import com.scrolldoom.dto.RegisterRequest;
import com.scrolldoom.dto.UserResponse;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.exception.UnauthorizedException;
import com.scrolldoom.model.RefreshToken;
import com.scrolldoom.model.User;
import com.scrolldoom.repository.RefreshTokenRepository;
import com.scrolldoom.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public UserResponse registerUser(RegisterRequest req) {
        User existing = userRepository.findByFirebaseUid(req.getFirebaseUid()).orElse(null);
        if (existing != null) {
            return mapToResponse(existing);
        }

        User user = User.builder()
                .firebaseUid(req.getFirebaseUid())
                .displayName(req.getDisplayName())
                .email(req.getEmail())
                .fcmToken(req.getFcmToken())
                .password(req.getPassword() != null ? passwordEncoder.encode(req.getPassword()) : null)
                .createdAt(new Date())
                .lastActiveAt(new Date())
                .build();

        return mapToResponse(userRepository.save(user));
    }

    public LoginWithPasswordResponse loginWithPassword(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        user.setLastActiveAt(new Date());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getFirebaseUid(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getFirebaseUid(), user.getEmail());

        refreshTokenRepository.save(RefreshToken.builder()
                .firebaseUid(user.getFirebaseUid())
                .token(refreshToken)
                .expiresAt(new Date(System.currentTimeMillis() + refreshExpiration))
                .createdAt(new Date())
                .revoked(false)
                .build());

        return LoginWithPasswordResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .firebaseUid(user.getFirebaseUid())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }

    public UserResponse verifyPassword(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        user.setLastActiveAt(new Date());
        return mapToResponse(userRepository.save(user));
    }

    public UserResponse getCurrentUserProfile(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setLastActiveAt(new Date());
        return mapToResponse(userRepository.save(user));
    }

    public void updateFcmToken(String firebaseUid, String fcmToken) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    public RefreshTokenResponse refreshAccessToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found or revoked"));

        String firebaseUid = jwtService.extractFirebaseUid(refreshToken);
        String email = jwtService.extractEmail(refreshToken);

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        String newAccessToken = jwtService.generateToken(firebaseUid, email);
        String newRefreshToken = jwtService.generateRefreshToken(firebaseUid, email);

        refreshTokenRepository.save(RefreshToken.builder()
                .firebaseUid(firebaseUid)
                .token(newRefreshToken)
                .expiresAt(new Date(System.currentTimeMillis() + refreshExpiration))
                .createdAt(new Date())
                .revoked(false)
                .build());

        return RefreshTokenResponse.builder()
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .firebaseUid(firebaseUid)
                .email(email)
                .build();
    }

    public RefreshTokenResponse slidingRefresh(String token) {
        if (!jwtService.isTokenValid(token)) {
            throw new UnauthorizedException("Token is expired or invalid");
        }

        String firebaseUid = jwtService.extractFirebaseUid(token);
        String email = jwtService.extractEmail(token);

        String newAccessToken = jwtService.generateToken(firebaseUid, email);

        return RefreshTokenResponse.builder()
                .token(newAccessToken)
                .firebaseUid(firebaseUid)
                .email(email)
                .build();
    }

    public RefreshTokenResponse firebaseRefresh(String firebaseIdToken) {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new ResourceNotFoundException("Firebase not configured");
        }

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(firebaseIdToken);
            String firebaseUid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = (String) decodedToken.getClaims().get("name");

            User user = userRepository.findByFirebaseUid(firebaseUid).orElse(null);
            
            if (user == null) {
                // Auto-register user if they don't exist
                user = User.builder()
                        .firebaseUid(firebaseUid)
                        .displayName(name != null ? name : email)
                        .email(email)
                        .createdAt(new Date())
                        .lastActiveAt(new Date())
                        .build();
                user = userRepository.save(user);
            } else {
                user.setLastActiveAt(new Date());
                userRepository.save(user);
            }

            String newAccessToken = jwtService.generateToken(firebaseUid, email);
            String newRefreshToken = jwtService.generateRefreshToken(firebaseUid, email);

            refreshTokenRepository.save(RefreshToken.builder()
                    .firebaseUid(firebaseUid)
                    .token(newRefreshToken)
                    .expiresAt(new Date(System.currentTimeMillis() + refreshExpiration))
                    .createdAt(new Date())
                    .revoked(false)
                    .build());

            return RefreshTokenResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .firebaseUid(firebaseUid)
                    .email(email)
                    .build();
        } catch (FirebaseAuthException e) {
            throw new UnauthorizedException("Invalid Firebase token: " + e.getMessage());
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredRefreshTokens() {
        refreshTokenRepository.deleteByExpiresAtBefore(new Date());
    }

    public static String getCurrentFirebaseUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("User not found");
        }
        return (String) auth.getPrincipal();
    }

    public ObjectId getCurrentUserId() {
        String firebaseUid = getCurrentFirebaseUid();
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId().toHexString())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .firebaseUid(user.getFirebaseUid())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
