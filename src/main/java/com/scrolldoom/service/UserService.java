package com.scrolldoom.service;

import com.scrolldoom.dto.RegisterRequest;
import com.scrolldoom.dto.UserResponse;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.User;
import com.scrolldoom.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
                .createdAt(new Date())
                .lastActiveAt(new Date())
                .build();

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

    public static String getCurrentFirebaseUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new ResourceNotFoundException("User not found");
        }
        return (String) auth.getPrincipal();
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
