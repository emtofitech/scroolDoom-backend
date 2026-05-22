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
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BreachService {

    private final BreachEventRepository breachEventRepository;
    private final PartnershipRepository partnershipRepository;
    private final UserRepository userRepository;
    private final PartnershipService partnershipService;
    private final NotificationService notificationService;

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
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date todayStart = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        Date todayEnd = cal.getTime();

        boolean alreadyExists = breachEventRepository
                .existsByUserIdAndPackageNameAndBreachedAtBetween(
                        userId, req.getPackageName(), todayStart, todayEnd);

        if (alreadyExists) {
            BreachEvent existing = breachEventRepository
                    .findByUserIdOrderByBreachedAtDesc(userId)
                    .stream()
                    .filter(e -> e.getPackageName().equals(req.getPackageName()))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                return mapToResponse(existing);
            }
        }

        ObjectId partnershipId = null;
        try {
            Partnership partnership = partnershipRepository.findActivePartnership(userId)
                    .orElse(null);
            if (partnership != null) {
                partnershipId = partnership.getId();
            }
        } catch (Exception e) {
            // No active partnership — proceed without notification
        }

        BreachEvent breach = BreachEvent.builder()
                .userId(userId)
                .partnershipId(partnershipId)
                .packageName(req.getPackageName())
                .appLabel(req.getAppLabel())
                .limitMinutes(req.getLimitMinutes())
                .actualMinutes(req.getActualMinutes())
                .partnerNotified(false)
                .breachedAt(new Date())
                .build();

        BreachEvent saved = breachEventRepository.save(breach);

        if (partnershipId != null) {
            try {
                String partnerFcmToken = partnershipService.getPartnerFcmToken(partnershipId, userId);
                User currentUser = userRepository.findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                notificationService.sendBreachNotification(
                        partnerFcmToken,
                        req.getAppLabel(),
                        req.getActualMinutes(),
                        req.getLimitMinutes(),
                        currentUser.getDisplayName());

                saved.setPartnerNotified(true);
                saved = breachEventRepository.save(saved);
            } catch (Exception e) {
                // Notification failure must not break the response
            }
        }

        return mapToResponse(saved);
    }

    public List<BreachEventResponse> getMyBreaches(ObjectId userId) {
        return breachEventRepository.findByUserIdOrderByBreachedAtDesc(userId)
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

    private BreachEventResponse mapToResponse(BreachEvent event) {
        return BreachEventResponse.builder()
                .id(event.getId().toHexString())
                .packageName(event.getPackageName())
                .appLabel(event.getAppLabel())
                .limitMinutes(event.getLimitMinutes())
                .actualMinutes(event.getActualMinutes())
                .partnerNotified(event.isPartnerNotified())
                .breachedAt(event.getBreachedAt())
                .build();
    }
}
