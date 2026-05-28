package com.scrolldoom.service;

import com.scrolldoom.dto.BreachEventResponse;
import com.scrolldoom.dto.CreateBreachRequest;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.BreachEvent;
import com.scrolldoom.model.Partnership;
import com.scrolldoom.model.User;
import com.scrolldoom.repository.BreachEventRepository;
import com.scrolldoom.repository.PartnershipRepository;
import com.scrolldoom.repository.UserRepository;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BreachService {

    private static final Logger log = LoggerFactory.getLogger(BreachService.class);

    private final BreachEventRepository breachEventRepository;
    private final PartnershipRepository partnershipRepository;
    private final UserRepository userRepository;
    private final PartnershipService partnershipService;
    private final NotificationService notificationService;

    private static final String SEVERITY_LOW = "LOW";
    private static final String SEVERITY_MEDIUM = "MEDIUM";
    private static final String SEVERITY_HIGH = "HIGH";

    public BreachService(BreachEventRepository breachEventRepository,
                         PartnershipRepository partnershipRepository,
                         UserRepository userRepository,
                         PartnershipService partnershipService,
                         NotificationService notificationService) {
        this.breachEventRepository = breachEventRepository;
        this.partnershipRepository = partnershipRepository;
        this.userRepository = userRepository;
        this.partnershipService = partnershipService;
        this.notificationService = notificationService;
    }

    public BreachEventResponse reportBreach(ObjectId userId, CreateBreachRequest req) {
        Date todayStart = getStartOfDay(new Date());
        Date todayEnd = getEndOfDay(new Date());

        log.info("Reporting screen-time breach for userId={}, packageName={}, limit={}, actual={}",
                userId, req.getPackageName(), req.getLimitMinutes(), req.getActualMinutes());

        boolean alreadyExists = breachEventRepository
                .existsByUserIdAndPackageNameAndBreachedAtBetween(
                        userId, req.getPackageName(), todayStart, todayEnd);

        if (alreadyExists) {
            List<BreachEvent> allBreaches = breachEventRepository
                    .findByUserIdOrderByBreachedAtDesc(userId);
            for (BreachEvent e : allBreaches) {
                if (req.getPackageName().equals(e.getPackageName())
                        && BreachEvent.BREACH_SCREEN_TIME.equals(e.getBreachType())) {
                    log.info("Found existing screen-time breach for today, returning it");
                    return mapToResponse(e);
                }
            }
            log.info("No matching screen-time breach found despite exists=true, creating new");
        }

        ObjectId partnershipId = getActivePartnershipId(userId);

        BreachEvent breach = BreachEvent.builder()
                .userId(userId)
                .partnershipId(partnershipId)
                .breachType(BreachEvent.BREACH_SCREEN_TIME)
                .packageName(req.getPackageName())
                .appLabel(req.getAppLabel())
                .limitMinutes(req.getLimitMinutes())
                .actualMinutes(req.getActualMinutes())
                .severity(calculateSeverity(req.getActualMinutes(), req.getLimitMinutes()))
                .partnerNotified(false)
                .acknowledged(false)
                .breachedAt(new Date())
                .build();

        BreachEvent saved = breachEventRepository.save(breach);
        log.info("Saved screen-time breach with id={}", saved.getId());

        try {
            notifyPartner(saved, userId, req.getAppLabel(), req.getActualMinutes(), req.getLimitMinutes(), null);
        } catch (Exception e) {
            log.error("Failed to notify partner for new breach: {}", e.getMessage());
        }

        return mapToResponse(saved);
    }

    public BreachEvent reportStreakBroken(ObjectId userId, String streakName, int missedDays) {
        Date todayStart = getStartOfDay(new Date());
        Date todayEnd = getEndOfDay(new Date());

        boolean alreadyExists = breachEventRepository
                .existsByUserIdAndStreakNameAndBreachedAtBetween(
                        userId, streakName, todayStart, todayEnd);

        if (alreadyExists) {
            return breachEventRepository
                    .findByUserIdAndBreachTypeOrderByBreachedAtDesc(userId, BreachEvent.BREACH_STREAK)
                    .stream()
                    .filter(e -> streakName.equals(e.getStreakName()))
                    .findFirst()
                    .orElse(null);
        }

        ObjectId partnershipId = getActivePartnershipId(userId);

        BreachEvent breach = BreachEvent.builder()
                .userId(userId)
                .partnershipId(partnershipId)
                .breachType(BreachEvent.BREACH_STREAK)
                .streakName(streakName)
                .missedDays(missedDays)
                .severity(calculateStreakSeverity(missedDays))
                .partnerNotified(false)
                .acknowledged(false)
                .breachedAt(new Date())
                .build();

        BreachEvent saved = breachEventRepository.save(breach);

        notifyPartner(saved, userId, null, 0, 0, streakName);

        return saved;
    }

    public BreachEvent reportBlockedAppOpened(ObjectId userId, String packageName, String appLabel) {
        Date todayStart = getStartOfDay(new Date());
        Date todayEnd = getEndOfDay(new Date());

        boolean alreadyExists = breachEventRepository
                .existsByUserIdAndPackageNameAndBreachedAtBetween(
                        userId, packageName, todayStart, todayEnd);

        if (alreadyExists) {
            return breachEventRepository
                    .findByUserIdOrderByBreachedAtDesc(userId)
                    .stream()
                    .filter(e -> packageName.equals(e.getPackageName())
                            && BreachEvent.BREACH_BLOCKED_APP.equals(e.getBreachType()))
                    .findFirst()
                    .orElse(null);
        }

        ObjectId partnershipId = getActivePartnershipId(userId);

        BreachEvent breach = BreachEvent.builder()
                .userId(userId)
                .partnershipId(partnershipId)
                .breachType(BreachEvent.BREACH_BLOCKED_APP)
                .packageName(packageName)
                .appLabel(appLabel)
                .severity(SEVERITY_HIGH)
                .partnerNotified(false)
                .acknowledged(false)
                .breachedAt(new Date())
                .build();

        BreachEvent saved = breachEventRepository.save(breach);

        notifyPartner(saved, userId, appLabel, 0, 0, null);

        return saved;
    }

    public List<BreachEventResponse> getMyBreaches(ObjectId userId) {
        return breachEventRepository.findByUserIdOrderByBreachedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BreachEventResponse> getMyBreachesByType(ObjectId userId, String breachType) {
        return breachEventRepository.findByUserIdAndBreachTypeOrderByBreachedAtDesc(userId, breachType)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<BreachEventResponse> getPartnerBreaches(ObjectId userId) {
        Partnership partnership = partnershipRepository.findActivePartnership(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active partnership"));

        ObjectId partnerId = partnership.getSenderUserId().equals(userId)
                ? partnership.getReceiverUserId()
                : partnership.getSenderUserId();

        return breachEventRepository.findByUserIdOrderByBreachedAtDesc(partnerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<BreachEventResponse> getPartnerBreaches(ObjectId userId, Pageable pageable) {
        ObjectId partnerId = resolvePartnerId(userId);
        return breachEventRepository.findByUserIdOrderByBreachedAtDesc(partnerId, pageable)
                .map(this::mapToResponse);
    }

    public Page<BreachEventResponse> getPartnerBreaches(ObjectId userId, Boolean acknowledged, Pageable pageable) {
        ObjectId partnerId = resolvePartnerId(userId);
        if (acknowledged != null) {
            return breachEventRepository.findByUserIdAndAcknowledgedOrderByBreachedAtDesc(partnerId, acknowledged, pageable)
                    .map(this::mapToResponse);
        }
        return breachEventRepository.findByUserIdOrderByBreachedAtDesc(partnerId, pageable)
                .map(this::mapToResponse);
    }

    public Page<BreachEventResponse> getPartnerBreachesByType(ObjectId userId, String breachType, Pageable pageable) {
        ObjectId partnerId = resolvePartnerId(userId);
        return breachEventRepository.findByUserIdAndBreachTypeOrderByBreachedAtDesc(partnerId, breachType, pageable)
                .map(this::mapToResponse);
    }

    public Page<BreachEventResponse> getPartnerBreachesByType(ObjectId userId, String breachType, Boolean acknowledged, Pageable pageable) {
        ObjectId partnerId = resolvePartnerId(userId);
        if (acknowledged != null) {
            return breachEventRepository.findByUserIdAndBreachTypeAndAcknowledgedOrderByBreachedAtDesc(partnerId, breachType, acknowledged, pageable)
                    .map(this::mapToResponse);
        }
        return breachEventRepository.findByUserIdAndBreachTypeOrderByBreachedAtDesc(partnerId, breachType, pageable)
                .map(this::mapToResponse);
    }

    private ObjectId resolvePartnerId(ObjectId userId) {
        Partnership partnership = partnershipRepository.findActivePartnership(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active partnership"));
        return partnership.getSenderUserId().equals(userId)
                ? partnership.getReceiverUserId()
                : partnership.getSenderUserId();
    }

    public BreachEvent acknowledgeBreach(ObjectId breachId, ObjectId userId) {
        BreachEvent breach = breachEventRepository.findById(breachId)
                .orElseThrow(() -> new ResourceNotFoundException("Breach not found"));

        if (!breach.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Breach not found");
        }

        breach.setAcknowledged(true);
        breach.setAcknowledgedAt(new Date());
        return breachEventRepository.save(breach);
    }

    private ObjectId getActivePartnershipId(ObjectId userId) {
        try {
            Partnership partnership = partnershipRepository.findActivePartnership(userId)
                    .orElse(null);
            return partnership != null ? partnership.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void notifyPartner(BreachEvent breach, ObjectId userId, String appLabel,
                               int actualMinutes, int limitMinutes, String streakName) {
        if (breach.getPartnershipId() == null) {
            return;
        }

        try {
            Partnership partnership = partnershipRepository.findById(breach.getPartnershipId())
                    .orElse(null);
            if (partnership == null) return;

            ObjectId partnerId = partnership.getSenderUserId().equals(userId)
                    ? partnership.getReceiverUserId()
                    : partnership.getSenderUserId();

            User partner = userRepository.findById(partnerId).orElse(null);
            if (partner == null) return;

            String partnerFcmToken = partner.getFcmToken();
            if (partnerFcmToken == null || partnerFcmToken.isBlank()) return;

            User currentUser = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            String title;
            String body;

            switch (breach.getBreachType()) {
                case BreachEvent.BREACH_STREAK:
                    title = "Streak Broken";
                    body = String.format("%s broke their '%s' streak (missed %d day(s))",
                            currentUser.getDisplayName(), streakName, breach.getMissedDays());
                    break;
                case BreachEvent.BREACH_BLOCKED_APP:
                    title = "Blocked App Accessed";
                    body = String.format("%s accessed a blocked app: %s",
                            currentUser.getDisplayName(), appLabel);
                    break;
                default:
                    title = "Screen Time Breach";
                    body = String.format("%s exceeded their %s limit (%d min used / %d min limit)",
                            currentUser.getDisplayName(), appLabel, actualMinutes, limitMinutes);
                    break;
            }

            notificationService.sendBreachNotification(partnerFcmToken, partner.getFirebaseUid(), title, body);

            breach.setPartnerNotified(true);
            breachEventRepository.save(breach);
        } catch (Exception e) {
            log.error("Failed to notify partner for breach {}: {}", breach.getId(), e.getMessage(), e);
        }
    }

    private String calculateSeverity(int actualMinutes, int limitMinutes) {
        double ratio = (double) actualMinutes / limitMinutes;
        if (ratio >= 2.0) return SEVERITY_HIGH;
        if (ratio >= 1.5) return SEVERITY_MEDIUM;
        return SEVERITY_LOW;
    }

    private String calculateStreakSeverity(int missedDays) {
        if (missedDays >= 7) return SEVERITY_HIGH;
        if (missedDays >= 3) return SEVERITY_MEDIUM;
        return SEVERITY_LOW;
    }

    private Date getStartOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date getEndOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    private BreachEventResponse mapToResponse(BreachEvent event) {
        return BreachEventResponse.builder()
                .id(event.getId().toHexString())
                .breachType(event.getBreachType())
                .packageName(event.getPackageName())
                .appLabel(event.getAppLabel())
                .limitMinutes(event.getLimitMinutes())
                .actualMinutes(event.getActualMinutes())
                .streakName(event.getStreakName())
                .missedDays(event.getMissedDays())
                .severity(event.getSeverity())
                .partnerNotified(event.isPartnerNotified())
                .acknowledged(event.isAcknowledged())
                .breachedAt(event.getBreachedAt())
                .build();
    }
}
