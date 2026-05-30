package com.scrolldoom.service;

import com.scrolldoom.dto.AppLimitResponse;
import com.scrolldoom.dto.CreateLimitRequest;
import com.scrolldoom.dto.LimitStatusResponse;
import com.scrolldoom.dto.UpdateLimitRequest;
import com.scrolldoom.exception.ConflictException;
import com.scrolldoom.exception.ForbiddenException;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.AppLimit;
import com.scrolldoom.model.BreachEvent;
import com.scrolldoom.repository.AppLimitRepository;
import com.scrolldoom.repository.BreachEventRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AppLimitService {

    private final AppLimitRepository appLimitRepository;
    private final BreachEventRepository breachEventRepository;

    public AppLimitService(AppLimitRepository appLimitRepository, BreachEventRepository breachEventRepository) {
        this.appLimitRepository = appLimitRepository;
        this.breachEventRepository = breachEventRepository;
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

    public List<LimitStatusResponse> getLimitStatuses(ObjectId userId) {
        List<AppLimit> limits = appLimitRepository.findByUserId(userId);

        Date todayStart = getStartOfDay(new Date());
        Date todayEnd = getEndOfDay(new Date());

        List<BreachEvent> todaysScreenTimeBreaches = breachEventRepository
                .findByUserIdAndBreachedAtBetween(userId, todayStart, todayEnd)
                .stream()
                .filter(e -> BreachEvent.BREACH_SCREEN_TIME.equals(e.getBreachType()))
                .toList();

        Map<String, BreachEvent> breachByPackage = todaysScreenTimeBreaches.stream()
                .collect(Collectors.toMap(BreachEvent::getPackageName, Function.identity(), (a, b) -> b));

        return limits.stream()
                .map(limit -> {
                    BreachEvent breach = breachByPackage.get(limit.getPackageName());
                    boolean exceeded = breach != null;
                    int actualMinutes = breach != null ? breach.getActualMinutes() : 0;
                    int remainingMinutes = limit.getDailyLimitMinutes() - actualMinutes;
                    return LimitStatusResponse.builder()
                            .id(limit.getId().toHexString())
                            .packageName(limit.getPackageName())
                            .appLabel(limit.getAppLabel())
                            .dailyLimitMinutes(limit.getDailyLimitMinutes())
                            .exceeded(exceeded)
                            .actualMinutes(exceeded ? actualMinutes : null)
                            .remainingMinutes(remainingMinutes)
                            .build();
                })
                .toList();
    }

    private static Date getStartOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private static Date getEndOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
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
