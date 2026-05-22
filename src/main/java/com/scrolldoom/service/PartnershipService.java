package com.scrolldoom.service;

import com.scrolldoom.dto.PartnershipResponse;
import com.scrolldoom.dto.UserResponse;
import com.scrolldoom.exception.ConflictException;
import com.scrolldoom.exception.ForbiddenException;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.Partnership;
import com.scrolldoom.model.User;
import com.scrolldoom.repository.PartnershipRepository;
import com.scrolldoom.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Random;

@Service
public class PartnershipService {

    private final PartnershipRepository partnershipRepository;
    private final UserRepository userRepository;
    private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final long INVITE_TTL_MS = 24 * 60 * 60 * 1000;
    private static final Random RANDOM = new Random();

    public PartnershipService(PartnershipRepository partnershipRepository,
                              UserRepository userRepository) {
        this.partnershipRepository = partnershipRepository;
        this.userRepository = userRepository;
    }

    public PartnershipResponse generateInvite(ObjectId userId) {
        partnershipRepository.findActivePartnership(userId)
                .ifPresent(p -> { throw new ConflictException("User already has an active partnership"); });

        partnershipRepository.findBySenderUserIdAndStatus(userId, "pending")
                .ifPresent(pending -> {
                    if (pending.getInviteExpiresAt().before(new Date())) {
                        partnershipRepository.delete(pending);
                    } else {
                        throw new ConflictException("User already has a pending invite");
                    }
                });

        String inviteCode = generateCode();

        Partnership partnership = Partnership.builder()
                .senderUserId(userId)
                .status("pending")
                .inviteCode(inviteCode)
                .inviteExpiresAt(new Date(System.currentTimeMillis() + INVITE_TTL_MS))
                .createdAt(new Date())
                .build();

        Partnership saved = partnershipRepository.save(partnership);

        return PartnershipResponse.builder()
                .id(saved.getId().toHexString())
                .status(saved.getStatus())
                .inviteCode(saved.getInviteCode())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public PartnershipResponse acceptInvite(ObjectId receiverUserId, String inviteCode) {
        Partnership partnership = partnershipRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

        if (!"pending".equals(partnership.getStatus())) {
            throw new ConflictException("Invite is no longer pending");
        }

        if (partnership.getInviteExpiresAt().before(new Date())) {
            throw new ConflictException("Invite has expired");
        }

        if (partnership.getSenderUserId().equals(receiverUserId)) {
            throw new ConflictException("You cannot accept your own invite");
        }

        partnershipRepository.findActivePartnership(receiverUserId)
                .ifPresent(p -> { throw new ConflictException("You already have an active partnership"); });

        partnership.setReceiverUserId(receiverUserId);
        partnership.setStatus("active");
        partnership.setAcceptedAt(new Date());

        Partnership saved = partnershipRepository.save(partnership);

        User partnerUser = userRepository.findById(partnership.getSenderUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Partner user not found"));

        return buildResponse(saved, partnerUser);
    }

    public PartnershipResponse getActivePartnership(ObjectId userId) {
        Partnership partnership = partnershipRepository.findActivePartnership(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No active partnership found"));

        ObjectId partnerId = partnership.getSenderUserId().equals(userId)
                ? partnership.getReceiverUserId()
                : partnership.getSenderUserId();

        User partnerUser = userRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner user not found"));

        return buildResponse(partnership, partnerUser);
    }

    public void dissolvePartnership(ObjectId userId, String partnershipId) {
        Partnership partnership = partnershipRepository.findById(new ObjectId(partnershipId))
                .orElseThrow(() -> new ResourceNotFoundException("Partnership not found"));

        boolean isParticipant = partnership.getSenderUserId().equals(userId)
                || (partnership.getReceiverUserId() != null
                    && partnership.getReceiverUserId().equals(userId));

        if (!isParticipant) {
            throw new ForbiddenException("You are not a participant in this partnership");
        }

        partnership.setStatus("dissolved");
        partnershipRepository.save(partnership);
    }

    public String getPartnerFcmToken(ObjectId partnershipId, ObjectId requestingUserId) {
        Partnership partnership = partnershipRepository.findById(partnershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Partnership not found"));

        ObjectId partnerId = partnership.getSenderUserId().equals(requestingUserId)
                ? partnership.getReceiverUserId()
                : partnership.getSenderUserId();

        User partner = userRepository.findById(partnerId)
                .orElseThrow(() -> new ResourceNotFoundException("Partner user not found"));

        return partner.getFcmToken();
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return code.toString();
    }

    private PartnershipResponse buildResponse(Partnership partnership, User partnerUser) {
        UserResponse partnerResponse = UserResponse.builder()
                .id(partnerUser.getId().toHexString())
                .displayName(partnerUser.getDisplayName())
                .email(partnerUser.getEmail())
                .avatarUrl(partnerUser.getAvatarUrl())
                .firebaseUid(partnerUser.getFirebaseUid())
                .createdAt(partnerUser.getCreatedAt())
                .build();

        return PartnershipResponse.builder()
                .id(partnership.getId().toHexString())
                .status(partnership.getStatus())
                .inviteCode(partnership.getInviteCode())
                .createdAt(partnership.getCreatedAt())
                .acceptedAt(partnership.getAcceptedAt())
                .partner(partnerResponse)
                .build();
    }
}
