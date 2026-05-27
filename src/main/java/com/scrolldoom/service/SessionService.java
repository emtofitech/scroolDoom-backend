package com.scrolldoom.service;

import com.scrolldoom.dto.SessionResponse;
import com.scrolldoom.exception.ResourceNotFoundException;
import com.scrolldoom.model.Session;
import com.scrolldoom.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import com.mongodb.client.result.DeleteResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SessionService {

    private static final int DEFAULT_SESSION_HOURS = 24;
    private static final int REMEMBER_ME_DAYS = 30;
    private static final int MAX_SESSIONS = 10;

    private final SessionRepository sessionRepository;
    private final MongoTemplate mongoTemplate;

    public SessionService(SessionRepository sessionRepository, MongoTemplate mongoTemplate) {
        this.sessionRepository = sessionRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public SessionResponse createSession(String firebaseUid, String deviceInfo, String ipAddress,
                                         String fcmToken, boolean rememberMe) {
        int activeCount = sessionRepository.countByFirebaseUidAndRevokedFalse(firebaseUid);
        if (activeCount >= MAX_SESSIONS) {
            revokeOldestSession(firebaseUid);
        }

        Date now = new Date();
        Date expiresAt = calculateExpiry(rememberMe);

        Session session = Session.builder()
                .firebaseUid(firebaseUid)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .fcmToken(fcmToken)
                .rememberMe(rememberMe)
                .expiresAt(expiresAt)
                .createdAt(now)
                .lastActiveAt(now)
                .revoked(false)
                .build();

        Session saved = sessionRepository.save(session);
        log.info("Session created for user {} on device {}", firebaseUid, deviceInfo);
        return mapToResponse(saved);
    }

    public List<SessionResponse> getUserSessions(String firebaseUid) {
        return sessionRepository.findByFirebaseUidAndRevokedFalseOrderByLastActiveAtDesc(firebaseUid)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void revokeSession(String firebaseUid, String sessionId) {
        ObjectId oid = new ObjectId(sessionId);
        Session session = sessionRepository.findByFirebaseUidAndIdAndRevokedFalse(firebaseUid, oid)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        session.setRevoked(true);
        sessionRepository.save(session);
        log.info("Session {} revoked for user {}", sessionId, firebaseUid);
    }

    public void revokeAllSessions(String firebaseUid) {
        List<Session> sessions = sessionRepository.findByFirebaseUidAndRevokedFalseOrderByLastActiveAtDesc(firebaseUid);
        sessions.forEach(s -> s.setRevoked(true));
        sessionRepository.saveAll(sessions);
        log.info("All sessions revoked for user {}", firebaseUid);
    }

    public void updateLastActive(String firebaseUid, String sessionId) {
        ObjectId oid = new ObjectId(sessionId);
        sessionRepository.findByFirebaseUidAndIdAndRevokedFalse(firebaseUid, oid)
                .ifPresent(session -> {
                    session.setLastActiveAt(new Date());
                    sessionRepository.save(session);
                });
    }

    public boolean isSessionValid(String firebaseUid, String sessionId) {
        try {
            ObjectId oid = new ObjectId(sessionId);
            return sessionRepository.findByFirebaseUidAndIdAndRevokedFalse(firebaseUid, oid)
                    .map(s -> s.getExpiresAt().after(new Date()))
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredSessions() {
        Query query = new Query(Criteria.where("expiresAt").lt(new Date()));
        DeleteResult result = mongoTemplate.remove(query, Session.class);
        if (result.getDeletedCount() > 0) {
            log.info("Cleaned up {} expired sessions", result.getDeletedCount());
        }
    }

    private void revokeOldestSession(String firebaseUid) {
        List<Session> sessions = sessionRepository.findByFirebaseUidAndRevokedFalseOrderByLastActiveAtDesc(firebaseUid);
        if (!sessions.isEmpty()) {
            Session oldest = sessions.get(sessions.size() - 1);
            oldest.setRevoked(true);
            sessionRepository.save(oldest);
        }
    }

    private Date calculateExpiry(boolean rememberMe) {
        Calendar cal = Calendar.getInstance();
        if (rememberMe) {
            cal.add(Calendar.DAY_OF_MONTH, REMEMBER_ME_DAYS);
        } else {
            cal.add(Calendar.HOUR_OF_DAY, DEFAULT_SESSION_HOURS);
        }
        return cal.getTime();
    }

    private SessionResponse mapToResponse(Session session) {
        return SessionResponse.builder()
                .id(session.getId().toHexString())
                .deviceInfo(session.getDeviceInfo())
                .ipAddress(session.getIpAddress())
                .rememberMe(session.isRememberMe())
                .expiresAt(session.getExpiresAt())
                .createdAt(session.getCreatedAt())
                .lastActiveAt(session.getLastActiveAt())
                .build();
    }
}
