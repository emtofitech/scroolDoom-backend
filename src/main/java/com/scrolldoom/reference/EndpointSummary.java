package com.scrolldoom.reference;

/**
 * ========================================================================
 * SCROLLDOOM API — ENDPOINT SUMMARY
 * ========================================================================
 *
 * Base URL: /api/v1
 * Auth:     Bearer token in Authorization header (Firebase ID token)
 *
 * ========================================================================
 * AUTH & USERS
 * ========================================================================
 *
 * POST /api/v1/auth/register
 *   Auth:    none
 *   Body:    { firebaseUid, displayName, email, fcmToken? }
 *   Returns: 201 Created
 *   Response: UserResponse
 *   Logic:   Idempotent — if firebaseUid exists, return existing user.
 *            Otherwise create new User with createdAt=now, lastActiveAt=now.
 *
 * GET /api/v1/users/me
 *   Auth:    JWT
 *   Returns: 200 OK
 *   Response: UserResponse
 *   Logic:   Resolve user from SecurityContext firebaseUid.
 *            Update lastActiveAt on every call.
 *            Throw 404 if firebaseUid not found in DB.
 *
 * PATCH /api/v1/users/me/fcm
 *   Auth:    JWT
 *   Body:    { fcmToken }
 *   Returns: 204 No Content
 *   Logic:   Overwrite fcmToken field. Called on every app launch.
 *            Throw 404 if user not found.
 *
 * ========================================================================
 * APP LIMITS
 * ========================================================================
 *
 * GET /api/v1/limits
 *   Auth:    JWT
 *   Returns: 200 OK
 *   Response: [ AppLimitResponse, ... ]
 *   Logic:   Return all AppLimit docs where userId == current user.
 *
 * POST /api/v1/limits
 *   Auth:    JWT
 *   Body:    { packageName, appLabel, dailyLimitMinutes (1-1440) }
 *   Returns: 201 Created
 *   Response: AppLimitResponse
 *   Logic:   Check findByUserIdAndPackageName. Throw 409 if exists.
 *            Create with updatedAt=now.
 *
 * PUT /api/v1/limits/{id}
 *   Auth:    JWT
 *   Body:    { dailyLimitMinutes (1-1440) }
 *   Returns: 200 OK
 *   Response: AppLimitResponse
 *   Logic:   Throw 404 if limit not found.
 *            Throw 403 if limit.userId != current user's ObjectId.
 *            Update dailyLimitMinutes + updatedAt.
 *
 * DELETE /api/v1/limits/{id}
 *   Auth:    JWT
 *   Returns: 204 No Content
 *   Logic:   Throw 404 if limit not found.
 *            Throw 403 if limit.userId != current user's ObjectId.
 *            Hard delete from MongoDB.
 *
 * GET /api/v1/limits/status
 *   Auth:    JWT
 *   Returns: 200 OK
 *   Response: [ LimitStatusResponse, ... ]
 *   Logic:   Get all AppLimit docs for current user.
 *            Query today's BreachEvent docs for SCREEN_TIME_EXCEEDED type.
 *            For each limit: if a breach exists for that packageName today,
 *            set exceeded=true with actualMinutes from the breach; else
 *            exceeded=false with actualMinutes=null.
 *            Return remainingMinutes = dailyLimitMinutes - actualMinutes.
 *
 * LimitStatusResponse {
 *   id:                String,
 *   packageName:       String,
 *   appLabel:          String,
 *   dailyLimitMinutes: int,
 *   exceeded:          boolean,
 *   actualMinutes:     Integer | null,
 *   remainingMinutes:  int
 * }
 *
 * ========================================================================
 * PARTNERSHIPS
 * ========================================================================
 *
 * POST /api/v1/partnerships/invite
 *   Auth:    JWT
 *   Returns: 201 Created
 *   Response: PartnershipResponse (partner field null)
 *   Logic:   Throw 409 if user already has active partnership.
 *            Check for existing pending invite from this user:
 *              if expired -> delete it silently
 *              if still valid -> throw 409
 *            Generate 6-char uppercase alphanumeric code.
 *            inviteExpiresAt = now + 24h, status = "pending".
 *
 * POST /api/v1/partnerships/accept
 *   Auth:    JWT
 *   Body:    { inviteCode }
 *   Returns: 200 OK
 *   Response: PartnershipResponse (partner = sender's UserResponse)
 *   Logic:   Throw 404 if code not found.
 *            Throw 409 if status != "pending".
 *            Throw 409 if inviteExpiresAt < now ("Invite expired").
 *            Throw 409 if senderUserId == receiverUserId ("own invite").
 *            Throw 409 if receiver already has active partnership.
 *            Set status="active", acceptedAt=now, receiverUserId=current user.
 *
 * GET /api/v1/partnerships/me
 *   Auth:    JWT
 *   Returns: 200 OK
 *   Response: PartnershipResponse (partner = other user's UserResponse)
 *   Logic:   Query $or:[{senderUserId},{receiverUserId}] + status="active".
 *            Throw 404 if none found.
 *            Determine which userId is the partner (opposite of current).
 *
 * DELETE /api/v1/partnerships/{id}
 *   Auth:    JWT
 *   Returns: 204 No Content
 *   Logic:   Throw 404 if partnership not found.
 *            Throw 403 if current user is not sender or receiver.
 *            Set status="dissolved" (soft delete).
 *
 * ========================================================================
 * BREACHES
 * ========================================================================
 *
 * POST /api/v1/breaches
 *   Auth:    JWT
 *   Body:    { packageName, appLabel, limitMinutes, actualMinutes }
 *   Returns: 201 Created
 *   Response: BreachEventResponse
 *   Logic:   1) Dedup: if breach for this packageName exists today
 *                (00:00:00 - 23:59:59), return existing record.
 *            2) Save BreachEvent with partnerNotified=false.
 *            3) If active partnership exists:
 *               - Get partner's fcmToken (via PartnershipService)
 *               - Get current user's displayName
 *               - Send FCM via FirebaseMessaging (catch + log errors)
 *               - If FCM success: update partnerNotified=true, re-save
 *            4) FCM failure MUST NOT propagate. Breach always saved.
 *
 * GET /api/v1/breaches/me
 *   Auth:    JWT
 *   Returns: 200 OK
 *   Response: [ BreachEventResponse, ... ]
 *   Logic:   findByUserIdOrderByBreachedAtDesc(currentUser).
 *
 * GET /api/v1/breaches/partner
 *   Auth:    JWT
 *   Returns: 200 OK
 *   Response: [ BreachEventResponse, ... ]
 *   Logic:   Throw 404 if no active partnership.
 *            Determine partner userId opposite of current.
 *            Return partner's breaches ordered by breachedAt DESC.
 *
 * ========================================================================
 * STREAKS
 * ========================================================================
 *
 * GET /api/v1/streaks/me
 *   Auth:    JWT
 *   Returns: 200 OK
 *   Response: StreakResponse
 *   Logic:   Lazy recalculation (skip if updatedAt is same day as today).
 *            Compute yesterdayStart (00:00:00) and yesterdayEnd (23:59:59).
 *            Query existsByUserIdAndBreachedAtBetween for yesterday.
 *            No breach yesterday (clean day):
 *              lastSuccessDate == dayBeforeYesterday -> currentStreak += 1
 *              otherwise -> currentStreak = 1
 *              lastSuccessDate = yesterday
 *              if currentStreak > longestStreak -> update longestStreak
 *            Had breach yesterday:
 *              currentStreak = 0
 *            Save with updatedAt = now.
 *
 * GET /api/v1/streaks/partner
 *   Auth:    JWT
 *   Returns: 200 OK
 *   Response: StreakResponse
 *   Logic:   Throw 404 if no active partnership.
 *            Resolve partner userId.
 *            Run getOrCalculateStreak for partner's userId.
 *
 * ========================================================================
 * HEALTH
 * ========================================================================
 *
 * GET /api/v1/health
 *   Auth:    none
 *   Returns: 200 OK
 *   Response: { "status": "UP" }
 *
 * ========================================================================
 * RESPONSE SHAPES
 * ========================================================================
 *
 * UserResponse {
 *   id:          String,          // MongoDB ObjectId as hex
 *   firebaseUid: String,
 *   displayName: String,
 *   email:       String,
 *   avatarUrl:   String | null,
 *   createdAt:   Date
 * }
 *
 * AppLimitResponse {
 *   id:                String,
 *   packageName:       String,
 *   appLabel:          String,
 *   dailyLimitMinutes: int,
 *   updatedAt:         Date
 * }
 *
 * PartnershipResponse {
 *   id:          String,
 *   status:      String,          // "pending" | "active" | "dissolved"
 *   inviteCode:  String,
 *   createdAt:   Date,
 *   acceptedAt:  Date | null,
 *   partner:     UserResponse | null
 * }
 *
 * BreachEventResponse {
 *   id:               String,
 *   packageName:      String,
 *   appLabel:         String,
 *   limitMinutes:     int,
 *   actualMinutes:    int,
 *   partnerNotified:  boolean,
 *   breachedAt:       Date
 * }
 *
 * StreakResponse {
 *   currentStreak:    int,
 *   longestStreak:    int,
 *   lastSuccessDate:  LocalDate | null,
 *   updatedAt:        Date
 * }
 *
 * ErrorResponse (404 / 409 / 403 / 401 / 500) {
 *   error:   String,     // short code: "Not Found", "Conflict", etc.
 *   message: String      // human-readable detail
 * }
 *
 * ErrorResponse (400 validation) {
 *   error:  String,                      // "Validation Failed"
 *   fields: { fieldName: "message", ... }
 * }
 *
 * ========================================================================
 * HTTP STATUS CODES USED
 * ========================================================================
 *
 *   200 OK          — successful GET, PUT, PATCH
 *   201 Created     — successful POST
 *   204 No Content  — successful DELETE, PATCH (fcm)
 *   400 Bad Request — validation failure (@Valid)
 *   401 Unauthorized — missing/invalid JWT (from JwtAuthFilter)
 *   403 Forbidden   — operation on resource owned by another user
 *   404 Not Found   — resource not found by ID / code / firebaseUid
 *   409 Conflict    — duplicate resource, expired invite, own invite, etc.
 *   500 Internal Server Error — unexpected exception
 *
 * ========================================================================
 */
public class EndpointSummary {
    private EndpointSummary() {}
}
