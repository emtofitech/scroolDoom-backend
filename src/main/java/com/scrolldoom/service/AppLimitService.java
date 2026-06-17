package com.scrolldoom.service;

import com.scrolldoom.dto.AppLimitResponse;
import com.scrolldoom.dto.BlockAppRequest;
import com.scrolldoom.dto.CreateLimitRequest;
import com.scrolldoom.dto.LimitStatusResponse;
import com.scrolldoom.dto.UpdateLimitRequest;
import com.scrolldoom.exception.ConflictException;
import com.scrolldoom.exception.ForbiddenException;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.AppLimit;
import com.scrolldoom.model.BlockedApp;
import com.scrolldoom.model.BreachEvent;
import com.scrolldoom.model.Partnership;
import com.scrolldoom.model.User;
import com.scrolldoom.repository.AppLimitRepository;
import com.scrolldoom.repository.BlockedAppRepository;
import com.scrolldoom.repository.BreachEventRepository;
import com.scrolldoom.repository.PartnershipRepository;
import com.scrolldoom.repository.UserRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AppLimitService {

    private static final Logger log = LoggerFactory.getLogger(AppLimitService.class);

    private final AppLimitRepository appLimitRepository;
    private final BreachEventRepository breachEventRepository;
    private final BlockedAppRepository blockedAppRepository;
    private final PartnershipRepository partnershipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AppLimitService(AppLimitRepository appLimitRepository,
                           BreachEventRepository breachEventRepository,
                           BlockedAppRepository blockedAppRepository,
                           PartnershipRepository partnershipRepository,
                           UserRepository userRepository,
                           NotificationService notificationService) {
        this.appLimitRepository = appLimitRepository;
        this.breachEventRepository = breachEventRepository;
        this.blockedAppRepository = blockedAppRepository;
        this.partnershipRepository = partnershipRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public List<AppLimitResponse> getLimits(ObjectId userId) {
        return appLimitRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public AppLimitResponse createLimit(ObjectId userId, CreateLimitRequest req) {
        appLimitRepository.findByUserIdAndPackageName(userId, req.getPackageName())
                .ifPresent(l -> { throw new ConflictException("Limit already exists for this app"); });

        AppLimit.AppLimitBuilder builder = AppLimit.builder()
                .userId(userId)
                .packageName(req.getPackageName())
                .appLabel(req.getAppLabel())
                .dailyLimitMinutes(req.getDailyLimitMinutes())
                .updatedAt(new Date());

        if (req.getBreachThreshold() != null) {
            builder.breachThreshold(req.getBreachThreshold());
        }

        return mapToResponse(appLimitRepository.save(builder.build()));
    }

    public AppLimitResponse updateLimit(ObjectId userId, String limitId, UpdateLimitRequest req) {
        AppLimit limit = appLimitRepository.findById(new ObjectId(limitId))
                .orElseThrow(() -> new ResourceNotFoundException("App limit not found"));

        if (!limit.getUserId().equals(userId)) {
            throw new ForbiddenException("You do not own this app limit");
        }

        limit.setDailyLimitMinutes(req.getDailyLimitMinutes());
        if (req.getBreachThreshold() != null) {
            limit.setBreachThreshold(req.getBreachThreshold());
        }
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

        Map<String, BlockedApp> blockedByPackage = blockedAppRepository.findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(BlockedApp::getPackageName, Function.identity(), (a, b) -> b));

        return limits.stream()
                .map(limit -> {
                    BreachEvent breach = breachByPackage.get(limit.getPackageName());
                    BlockedApp blocked = blockedByPackage.get(limit.getPackageName());
                    boolean exceeded = breach != null;
                    int actualMinutes = breach != null ? breach.getActualMinutes() : 0;
                    int remainingMinutes = limit.getDailyLimitMinutes() - actualMinutes;

                    boolean isBlocked = blocked != null;
                    String blockedBy = blocked != null ? blocked.getBlockedBy() : null;
                    Date lockedUntil = blocked != null ? blocked.getExpiresAt() : null;
                    long todayBreachCount = todaysScreenTimeBreaches.stream()
                            .filter(e -> e.getPackageName().equals(limit.getPackageName()))
                            .count();
                    int breachesRemaining = Math.max(0, limit.getBreachThreshold() - (int) todayBreachCount);

                    return LimitStatusResponse.builder()
                            .id(limit.getId().toHexString())
                            .packageName(limit.getPackageName())
                            .appLabel(limit.getAppLabel())
                            .dailyLimitMinutes(limit.getDailyLimitMinutes())
                            .exceeded(exceeded)
                            .actualMinutes(exceeded ? actualMinutes : null)
                            .remainingMinutes(remainingMinutes)
                            .breachThreshold(limit.getBreachThreshold())
                            .blocked(isBlocked)
                            .blockedBy(blockedBy)
                            .lockedUntil(lockedUntil)
                            .breachesRemaining(isBlocked ? 0 : breachesRemaining)
                            .build();
                })
                .toList();
    }

    public List<BlockedApp> listBlockedApps(ObjectId userId) {
        return blockedAppRepository.findByUserId(userId);
    }

    public BlockedApp lockApp(ObjectId userId, BlockAppRequest req, String blockedBy) {
        BlockedApp existing = blockedAppRepository.findByUserIdAndPackageName(userId, req.getPackageName())
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        BlockedApp blocked = BlockedApp.builder()
                .userId(userId)
                .packageName(req.getPackageName())
                .appLabel(req.getAppLabel())
                .blockedAt(new Date())
                .blockedBy(blockedBy)
                .build();

        return blockedAppRepository.save(blocked);
    }

    public BlockedApp lockPartnerApp(ObjectId userId, BlockAppRequest req) {
        Partnership partnership = partnershipRepository.findActivePartnership(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active partnership"));

        ObjectId partnerId = partnership.getSenderUserId().equals(userId)
                ? partnership.getReceiverUserId()
                : partnership.getSenderUserId();

        BlockedApp blocked = lockApp(partnerId, req, "partner");

        User partner = userRepository.findById(partnerId).orElse(null);
        User me = userRepository.findById(userId).orElse(null);
        if (partner != null && me != null
                && partner.getFcmToken() != null && !partner.getFcmToken().isBlank()) {
            notificationService.sendBreachNotification(
                    partner.getFcmToken(), partner.getFirebaseUid(),
                    "App Locked by Partner",
                    String.format("%s has locked your %s access",
                            me.getDisplayName(), req.getAppLabel()));
        }

        return blocked;
    }

    public void unlockApp(ObjectId userId, String packageName) {
        blockedAppRepository.deleteByUserIdAndPackageName(userId, packageName);

        AppLimit limit = appLimitRepository.findByUserIdAndPackageName(userId, packageName).orElse(null);
        if (limit != null) {
            limit.setLockedUntil(null);
            appLimitRepository.save(limit);
        }
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
                .breachThreshold(limit.getBreachThreshold())
                .updatedAt(limit.getUpdatedAt())
                .build();
    }
}
