package com.scrolldoom.service;

import com.scrolldoom.dto.AppLimitResponse;
import com.scrolldoom.dto.CreateLimitRequest;
import com.scrolldoom.dto.UpdateLimitRequest;
import com.scrolldoom.exception.ConflictException;
import com.scrolldoom.exception.ForbiddenException;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.AppLimit;
import com.scrolldoom.repository.AppLimitRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppLimitService {

    private final AppLimitRepository appLimitRepository;

    public AppLimitService(AppLimitRepository appLimitRepository) {
        this.appLimitRepository = appLimitRepository;
    }

    public List<AppLimitResponse> getLimits(ObjectId userId) {
        return appLimitRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AppLimitResponse createLimit(ObjectId userId, CreateLimitRequest req) {
        boolean exists = appLimitRepository
                .findByUserIdAndPackageName(userId, req.getPackageName())
                .isPresent();

        if (exists) {
            throw new ConflictException("Limit already exists for this app");
        }

        AppLimit limit = AppLimit.builder()
                .userId(userId)
                .packageName(req.getPackageName())
                .appLabel(req.getAppLabel())
                .dailyLimitMinutes(req.getDailyLimitMinutes())
                .updatedAt(new Date())
                .build();

        return mapToResponse(appLimitRepository.save(limit));
    }

    public AppLimitResponse updateLimit(ObjectId userId, String limitId, UpdateLimitRequest req) {
        AppLimit limit = appLimitRepository.findById(new ObjectId(limitId))
                .orElseThrow(() -> new ResourceNotFoundException("App limit not found"));

        if (!limit.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own this app limit");
        }

        limit.setDailyLimitMinutes(req.getDailyLimitMinutes());
        limit.setUpdatedAt(new Date());

        return mapToResponse(appLimitRepository.save(limit));
    }

    public void deleteLimit(ObjectId userId, String limitId) {
        AppLimit limit = appLimitRepository.findById(new ObjectId(limitId))
                .orElseThrow(() -> new ResourceNotFoundException("App limit not found"));

        if (!limit.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own this app limit");
        }

        appLimitRepository.delete(limit);
    }

    private AppLimitResponse mapToResponse(AppLimit limit) {
        return AppLimitResponse.builder()
                .id(limit.getId().toHexString())
                .packageName(limit.getPackageName())
                .appLabel(limit.getAppLabel())
                .dailyLimitMinutes(limit.getDailyLimitMinutes())
                .updatedAt(limit.getUpdatedAt())
                .build();
    }
}
