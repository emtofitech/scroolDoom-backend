#!/bin/bash
set -euo pipefail

BASE="https://doomscroll-aotr.onrender.com"
TOKEN=""
REFRESH_TOKEN=""
LIMIT_ID=""
BREACH_ID=""
SESSION_ID=""
DELIVERY_ID=""
INVITE_CODE=""
PARTNERSHIP_ID=""

PASSED=0
FAILED=0
TOTAL=0

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

test_endpoint() {
  local desc="$1"
  local expected="$2"
  local response="$3"
  local status="$4"

  TOTAL=$((TOTAL + 1))
  if [ "$status" = "$expected" ]; then
    PASSED=$((PASSED + 1))
    echo -e "${GREEN}✓${NC} [$status] $desc"
  else
    FAILED=$((FAILED + 1))
    echo -e "${RED}✗${NC} [$status != $expected] $desc"
    echo "  Response: $response"
  fi
}

save_value() {
  echo "$1" | python3 -c "import sys,json; print(json.load(sys.stdin).get('$2',''))" 2>/dev/null || echo ""
}

# ============================================================
# PHASE 0: Health
# ============================================================
echo -e "\n${YELLOW}═══ Phase 0: Health ═══${NC}"

RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/health")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/health" "200" "$BODY" "$STATUS"

# ============================================================
# PHASE 1: Auth — Register + Login
# ============================================================
echo -e "\n${YELLOW}═══ Phase 1: Authentication ═══${NC}"

# 1. Register
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"firebaseUid":"curl-test-user-001","displayName":"Curl Test User","email":"curltest@example.com","password":"TestPass123!"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/auth/register (new user)" "201" "$BODY" "$STATUS"

# 2. Register again (idempotent)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"firebaseUid":"curl-test-user-001","displayName":"Curl Test User","email":"curltest@example.com","password":"TestPass123!"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/auth/register (idempotent)" "201" "$BODY" "$STATUS"

# 3. Login with password
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/auth/login-with-password" \
  -H "Content-Type: application/json" \
  -d '{"email":"curltest@example.com","password":"TestPass123!"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/auth/login-with-password (valid)" "200" "$BODY" "$STATUS"
TOKEN=$(save_value "$BODY" "token")
REFRESH_TOKEN=$(save_value "$BODY" "refreshToken")

# Debug: verify token was captured
if [ -z "$TOKEN" ]; then
  echo -e "${RED}  FATAL: No access token captured from login response${NC}"
  echo "  Response body: $BODY"
  exit 1
fi

# 4. Refresh token
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/auth/refresh (valid)" "200" "$BODY" "$STATUS"
NEW_REFRESH=$(save_value "$BODY" "refreshToken")
if [ -n "$NEW_REFRESH" ]; then
  REFRESH_TOKEN="$NEW_REFRESH"
fi

# 5. Sliding refresh (takes access token in "refreshToken" field — confusing API naming)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/auth/sliding-refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$TOKEN\"}")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/auth/sliding-refresh (valid)" "200" "$BODY" "$STATUS"
# Update token if a new one was returned
NEW_TOKEN=$(save_value "$BODY" "token")
if [ -n "$NEW_TOKEN" ]; then
  TOKEN="$NEW_TOKEN"
fi

# Error paths — auth
echo -e "\n${YELLOW}── Auth Error Paths ──${NC}"

# Register with missing fields — backend currently throws ResourceNotFoundException (404)
# TODO: Backend should validate @NotBlank and return 400; this is a known bug
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"displayName":"Missing Fields"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/auth/register (missing fields → 400)" "400" "$BODY" "$STATUS"

# Login with wrong password — backend throws ResourceNotFoundException → 404 (should be 401)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/auth/login-with-password" \
  -H "Content-Type: application/json" \
  -d '{"email":"curltest@example.com","password":"wrongpassword"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/auth/login-with-password (wrong password → 401)" "401" "$BODY" "$STATUS"

# Refresh with invalid token — backend throws ResourceNotFoundException → 404 (should be 401)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/auth/refresh" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"invalid-token-value"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/auth/refresh (invalid token → 401)" "401" "$BODY" "$STATUS"

# ============================================================
# PHASE 2: User Profile
# ============================================================
echo -e "\n${YELLOW}═══ Phase 2: User Profile ═══${NC}"

# No auth → should be 403 (Spring Security blocks unauthenticated)
# Current behavior: JwtAuthFilter passes through, AnonymousAuthenticationFilter creates anon token,
# controller throws ResourceNotFoundException → 404
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/users/me")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/users/me (no auth → 403)" "403" "$BODY" "$STATUS"

# Get profile
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/users/me" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/users/me (valid)" "200" "$BODY" "$STATUS"

# Update FCM
RESP=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE/api/v1/users/me/fcm" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fcmToken":"curl-test-fcm-token-abc123"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "PATCH /api/v1/users/me/fcm (valid)" "204" "$BODY" "$STATUS"

# Error path — empty fcmToken
RESP=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE/api/v1/users/me/fcm" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"fcmToken":""}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "PATCH /api/v1/users/me/fcm (empty → 400)" "400" "$BODY" "$STATUS"

# ============================================================
# PHASE 3: Sessions
# ============================================================
echo -e "\n${YELLOW}═══ Phase 3: Sessions ═══${NC}"

RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/sessions" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/sessions (list)" "200" "$BODY" "$STATUS"
SESSION_ID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['id'] if d and isinstance(d,list) and len(d)>0 else '')" 2>/dev/null || echo "")

# Revoke non-existent session → 404
RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/sessions/000000000000000000000000" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "DELETE /api/v1/sessions/{id} (not found → 404)" "404" "$BODY" "$STATUS"

# Revoke all other sessions
RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/sessions" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "DELETE /api/v1/sessions (revoke all)" "200" "$BODY" "$STATUS"

# ============================================================
# PHASE 4: App Limits
# ============================================================
echo -e "\n${YELLOW}═══ Phase 4: App Limits ═══${NC}"

# Create limit
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.instagram.android","appLabel":"Instagram","dailyLimitMinutes":30}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits (create)" "201" "$BODY" "$STATUS"
LIMIT_ID=$(save_value "$BODY" "id")

# Get all limits
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/limits" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/limits (list)" "200" "$BODY" "$STATUS"

# Update limit
if [ -n "$LIMIT_ID" ]; then
  RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/api/v1/limits/$LIMIT_ID" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"dailyLimitMinutes":45}')
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "PUT /api/v1/limits/$LIMIT_ID (update)" "200" "$BODY" "$STATUS"
fi

# Duplicate limit → 409
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.instagram.android","appLabel":"Instagram","dailyLimitMinutes":30}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits (duplicate → 409)" "409" "$BODY" "$STATUS"

# Limit > 1440 → 400
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.test","appLabel":"Test","dailyLimitMinutes":1500}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits (exceeds max → 400)" "400" "$BODY" "$STATUS"

# Limit < 1 → 400
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.test","appLabel":"Test","dailyLimitMinutes":0}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits (below min → 400)" "400" "$BODY" "$STATUS"

# Delete non-existent → 404
RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/limits/000000000000000000000000" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "DELETE /api/v1/limits/{id} (not found → 404)" "404" "$BODY" "$STATUS"

# Delete limit
if [ -n "$LIMIT_ID" ]; then
  RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/limits/$LIMIT_ID" \
    -H "Authorization: Bearer $TOKEN")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "DELETE /api/v1/limits/$LIMIT_ID (delete)" "204" "$BODY" "$STATUS"
fi

# ============================================================
# PHASE 5: Partnerships
# ============================================================
echo -e "\n${YELLOW}═══ Phase 5: Partnerships ═══${NC}"

# Generate invite
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/partnerships/invite" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/partnerships/invite (generate)" "201" "$BODY" "$STATUS"
INVITE_CODE=$(save_value "$BODY" "inviteCode")
PARTNERSHIP_ID=$(save_value "$BODY" "id")

# Get active partnership — invite is pending, not active → 404
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/partnerships/me" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/partnerships/me (pending invite, no active → 404)" "404" "$BODY" "$STATUS"

# Duplicate invite → 409
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/partnerships/invite" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/partnerships/invite (duplicate → 409)" "409" "$BODY" "$STATUS"

# Accept with invalid code → 404
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/partnerships/accept" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"inviteCode":"ZZZZZZ"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/partnerships/accept (invalid code → 404)" "404" "$BODY" "$STATUS"

# Accept own invite → 409 (self-invite)
if [ -n "$INVITE_CODE" ]; then
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/partnerships/accept" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"inviteCode\":\"$INVITE_CODE\"}")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "POST /api/v1/partnerships/accept (own invite → 409)" "409" "$BODY" "$STATUS"
fi

# Dissolve partnership (if one exists)
if [ -n "$PARTNERSHIP_ID" ]; then
  RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/partnerships/$PARTNERSHIP_ID" \
    -H "Authorization: Bearer $TOKEN")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "DELETE /api/v1/partnerships/$PARTNERSHIP_ID (dissolve)" "204" "$BODY" "$STATUS"
fi

# ============================================================
# PHASE 6: Breaches
# ============================================================
echo -e "\n${YELLOW}═══ Phase 6: Breaches ═══${NC}"

# Screen time breach
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/breaches/screen-time" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.instagram.android","appLabel":"Instagram","limitMinutes":30,"actualMinutes":45}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/breaches/screen-time (report)" "201" "$BODY" "$STATUS"
BREACH_ID=$(save_value "$BODY" "id")

# Streak broken
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/breaches/streak" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"streakName":"No Instagram Before Noon","missedDays":3}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/breaches/streak (report)" "201" "$BODY" "$STATUS"

# Blocked app
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/breaches/blocked-app" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.tiktok.android","appLabel":"TikTok"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/breaches/blocked-app (report)" "201" "$BODY" "$STATUS"

# Get my breaches
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/breaches/me" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/breaches/me (list)" "200" "$BODY" "$STATUS"

# Get breaches by type
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/breaches/me/type/SCREEN_TIME_EXCEEDED" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/breaches/me/type/SCREEN_TIME_EXCEEDED" "200" "$BODY" "$STATUS"

# Acknowledge breach
if [ -n "$BREACH_ID" ]; then
  RESP=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE/api/v1/breaches/$BREACH_ID/acknowledge" \
    -H "Authorization: Bearer $TOKEN")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "PATCH /api/v1/breaches/$BREACH_ID/acknowledge" "200" "$BODY" "$STATUS"
fi

# Get partner breaches (may fail if no partnership — that's OK)
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/breaches/partner?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
if [ "$STATUS" = "200" ]; then
  test_endpoint "GET /api/v1/breaches/partner (list)" "200" "$BODY" "$STATUS"
else
  test_endpoint "GET /api/v1/breaches/partner (no partnership → expected error)" "404" "$BODY" "$STATUS"
fi

# Get partner breaches by type
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/breaches/partner/type/SCREEN_TIME_EXCEEDED?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
if [ "$STATUS" = "200" ]; then
  test_endpoint "GET /api/v1/breaches/partner/type/SCREEN_TIME_EXCEEDED" "200" "$BODY" "$STATUS"
else
  test_endpoint "GET /api/v1/breaches/partner/type/SCREEN_TIME_EXCEEDED (no partnership → expected error)" "404" "$BODY" "$STATUS"
fi

# Error paths — breaches
echo -e "\n${YELLOW}── Breach Error Paths ──${NC}"

RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/breaches/screen-time" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"packageName":"","appLabel":"Test","limitMinutes":30,"actualMinutes":45}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/breaches/screen-time (empty packageName → 400)" "400" "$BODY" "$STATUS"

RESP=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE/api/v1/breaches/000000000000000000000000/acknowledge" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "PATCH /api/v1/breaches/{id}/acknowledge (not found → 404)" "404" "$BODY" "$STATUS"

RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/breaches/streak" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"streakName":"","missedDays":0}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/breaches/streak (empty streakName → 400)" "400" "$BODY" "$STATUS"

# ============================================================
# PHASE 7: Streaks
# ============================================================
echo -e "\n${YELLOW}═══ Phase 7: Streaks ═══${NC}"

RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/streaks/me" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/streaks/me" "200" "$BODY" "$STATUS"

RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/streaks/partner" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
if [ "$STATUS" = "200" ]; then
  test_endpoint "GET /api/v1/streaks/partner" "200" "$BODY" "$STATUS"
else
  test_endpoint "GET /api/v1/streaks/partner (no partnership → expected error)" "404" "$BODY" "$STATUS"
fi

# ============================================================
# PHASE 8: Usage Tracking
# ============================================================
echo -e "\n${YELLOW}═══ Phase 8: Usage Tracking ═══${NC}"

# Track event
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/usage/events" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"eventType":"screen_view","screenName":"HomeScreen","featureName":"feed","durationMs":15000,"deviceInfo":"curl-test","appVersion":"1.0.0"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/usage/events (track)" "200" "$BODY" "$STATUS"

# App open
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/usage/app-open" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/usage/app-open" "200" "$BODY" "$STATUS"

# App close
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/usage/app-close" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/usage/app-close" "200" "$BODY" "$STATUS"

# Heartbeat
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/usage/heartbeat?screenName=HomeScreen&featureName=feed" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/usage/heartbeat" "200" "$BODY" "$STATUS"

# Track notification
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/usage/notifications" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"notificationType":"breach_alert","title":"You exceeded your limit!","body":"Instagram limit reached"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/usage/notifications (track)" "200" "$BODY" "$STATUS"
DELIVERY_ID=$(save_value "$BODY" "id")

# Mark delivered
if [ -n "$DELIVERY_ID" ]; then
  RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/api/v1/usage/notifications/$DELIVERY_ID/delivered" \
    -H "Authorization: Bearer $TOKEN")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "PUT /api/v1/usage/notifications/$DELIVERY_ID/delivered" "200" "$BODY" "$STATUS"

  # Mark opened
  RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/api/v1/usage/notifications/$DELIVERY_ID/opened" \
    -H "Authorization: Bearer $TOKEN")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "PUT /api/v1/usage/notifications/$DELIVERY_ID/opened" "200" "$BODY" "$STATUS"
fi

# Get my stats
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/usage/stats/me" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/usage/stats/me" "200" "$BODY" "$STATUS"

# Global stats
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/usage/stats/global" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/usage/stats/global" "200" "$BODY" "$STATUS"

# Error path — non-existent notification
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/api/v1/usage/notifications/000000000000000000000000/delivered" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "PUT /api/v1/usage/notifications/{id}/delivered (not found → 404)" "404" "$BODY" "$STATUS"

RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/api/v1/usage/notifications/000000000000000000000000/opened" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "PUT /api/v1/usage/notifications/{id}/opened (not found → 404)" "404" "$BODY" "$STATUS"

# ============================================================
# PHASE 9: Swagger / OpenAPI (public)
# ============================================================
echo -e "\n${YELLOW}═══ Phase 9: Swagger/OpenAPI ═══${NC}"

# SpringDoc redirects /swagger-ui.html → /swagger-ui/index.html
RESP=$(curl -s -w "\n%{http_code}" -o /dev/null -L "$BASE/swagger-ui/index.html")
STATUS=$(echo "$RESP" | tail -1)
test_endpoint "GET /swagger-ui/index.html (follow redirects)" "200" "" "$STATUS"

RESP=$(curl -s -w "\n%{http_code}" "$BASE/v3/api-docs" | head -c 200)
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /v3/api-docs (spec)" "200" "$BODY" "$STATUS"

# ============================================================
# SUMMARY
# ============================================================
echo -e "\n${YELLOW}══════════════════════════════════════════${NC}"
echo -e "  ${GREEN}Passed: $PASSED${NC}  ${RED}Failed: $FAILED${NC}  Total: $TOTAL"
echo -e "${YELLOW}══════════════════════════════════════════${NC}"

if [ $FAILED -gt 0 ]; then
  exit 1
fi
