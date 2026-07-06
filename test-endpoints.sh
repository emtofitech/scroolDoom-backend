#!/bin/bash
set -uo pipefail

BASE="https://doomscroll-aotr.onrender.com"
TOKEN=""
REFRESH_TOKEN=""
LIMIT_ID=""
BREACH_ID=""
SESSION_ID=""
DELIVERY_ID=""
INVITE_CODE=""
PARTNERSHIP_ID=""
TOKEN2=""
REFRESH_TOKEN2=""
PARTNER_LIMIT_ID=""
AUTO_LOCK_TWITTER_ID=""
AUTO_LOCK_FACEBOOK_ID=""

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
  local success_field=""
  if [ -n "$response" ]; then
    success_field=$(echo "$response" | python3 -c "import sys,json; d=json.load(sys.stdin); print(str(d.get('success','')).lower())" 2>/dev/null || echo "")
  fi

  if [ "$status" = "$expected" ]; then
    PASSED=$((PASSED + 1))
    if [ "$status" = "204" ] || [ "$status" = "403" ] || [ -z "$success_field" ]; then
      echo -e "${GREEN}✓${NC} [$status] $desc"
    elif [ "$success_field" = "true" ] && [ "$expected" = "200" -o "$expected" = "201" ]; then
      echo -e "${GREEN}✓${NC} [$status] $desc (success=true)"
    elif [ "$success_field" = "false" ] && [ "$expected" != "200" ] && [ "$expected" != "201" ]; then
      echo -e "${GREEN}✓${NC} [$status] $desc (success=false, error envelope)"
    else
      echo -e "${GREEN}✓${NC} [$status] $desc"
    fi
  else
    FAILED=$((FAILED + 1))
    echo -e "${RED}✗${NC} [$status != $expected] $desc"
    echo "  Response: $response"
  fi
}

save_value() {
  echo "$1" | python3 -c "
import sys,json
d=json.load(sys.stdin)
inner=d.get('data',d)
if isinstance(inner,dict):
    print(inner.get('$2',''))
elif isinstance(inner,list) and len(inner)>0 and isinstance(inner[0],dict):
    print(inner[0].get('$2',''))
else:
    print('')
" 2>/dev/null || echo ""
}

check_field() {
  local response="$1"
  local field="$2"
  local expected="$3"
  local desc="$4"

  local actual
  actual=$(echo "$response" | python3 -c "
import sys,json
d=json.load(sys.stdin)
inner=d.get('data',d)
if isinstance(inner,dict):
    print(inner.get('$field','__MISSING__'))
elif isinstance(inner,list) and len(inner)>0 and isinstance(inner[0],dict):
    print(inner[0].get('$field','__MISSING__'))
else:
    print('__MISSING__')
" 2>/dev/null || echo "__PARSE_ERR__")

  if [ "$actual" = "$expected" ]; then
    echo -e "    ${GREEN}✓ field $field=$actual${NC}"
    return 0
  else
    echo -e "    ${RED}✗ $desc: field $field expected '$expected' got '$actual'${NC}"
    FAILED=$((FAILED + 1))
    return 1
  fi
}

check_contains() {
  local response="$1"
  local field="$2"
  local desc="$3"

  local found
  found=$(echo "$response" | python3 -c "
import sys,json
d=json.load(sys.stdin)
inner=d.get('data',d)
if isinstance(inner,dict):
    print('true' if '$field' in inner else 'false')
elif isinstance(inner,list):
    if len(inner)>0 and isinstance(inner[0],dict):
        print('true' if '$field' in inner[0] else 'false')
    else:
        print('false')
else:
    print('false')
" 2>/dev/null || echo "false")

  if [ "$found" = "true" ]; then
    echo -e "    ${GREEN}✓ contains $field${NC}"
    return 0
  else
    echo -e "    ${RED}✗ $desc: missing field '$field'${NC}"
    FAILED=$((FAILED + 1))
    return 1
  fi
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

# 6. Register second user (for partnership tests)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"firebaseUid":"curl-test-user-002","displayName":"Curl Partner User","email":"partner@example.com","password":"PartnerPass123!"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/auth/register (second user)" "201" "$BODY" "$STATUS"

# 7. Login second user
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/auth/login-with-password" \
  -H "Content-Type: application/json" \
  -d '{"email":"partner@example.com","password":"PartnerPass123!"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/auth/login-with-password (second user)" "200" "$BODY" "$STATUS"
TOKEN2=$(save_value "$BODY" "token")
REFRESH_TOKEN2=$(save_value "$BODY" "refreshToken")

# Debug: verify second token was captured
if [ -z "$TOKEN2" ]; then
  echo -e "${RED}  FATAL: No access token captured from second user login${NC}"
  echo "  Response body: $BODY"
  exit 1
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
SESSION_ID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); inner=d.get('data',d); print(inner[0]['id'] if isinstance(inner,list) and len(inner)>0 else '')" 2>/dev/null || echo "")

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
  -d '{"packageName":"com.instagram.android","appLabel":"Instagram","dailyLimitMinutes":30,"breachThreshold":3}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits (create, breachThreshold=3)" "201" "$BODY" "$STATUS"
LIMIT_ID=$(save_value "$BODY" "id")

# Get all limits
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/limits" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/limits (list)" "200" "$BODY" "$STATUS"

# Get limit status (no breach yet)
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/limits/status" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/limits/status (initial, not exceeded)" "200" "$BODY" "$STATUS"
# Validate new lockout fields are present
check_contains "$BODY" "blocked" "limits/status should have blocked"
check_contains "$BODY" "breachThreshold" "limits/status should have breachThreshold"
check_contains "$BODY" "breachesRemaining" "limits/status should have breachesRemaining"
check_contains "$BODY" "blockedBy" "limits/status should have blockedBy"
check_contains "$BODY" "lockedUntil" "limits/status should have lockedUntil"

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
# PHASE 5: Partnerships (User 1 invites User 2)
# ============================================================
echo -e "\n${YELLOW}═══ Phase 5: Partnerships ═══${NC}"

# 1. User 1 generates invite
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/partnerships/invite" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/partnerships/invite (User 1 generates)" "201" "$BODY" "$STATUS"
INVITE_CODE=$(save_value "$BODY" "inviteCode")

# 2. User 1 duplicate invite → 409
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/partnerships/invite" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/partnerships/invite (User 1 duplicate → 409)" "409" "$BODY" "$STATUS"

# 3. User 2 accepts with invalid code → 404
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/partnerships/accept" \
  -H "Authorization: Bearer $TOKEN2" \
  -H "Content-Type: application/json" \
  -d '{"inviteCode":"ZZZZZZ"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/partnerships/accept (User 2 invalid code → 404)" "404" "$BODY" "$STATUS"

# 4. User 1 tries to accept own invite → 409
if [ -n "$INVITE_CODE" ]; then
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/partnerships/accept" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"inviteCode\":\"$INVITE_CODE\"}")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "POST /api/v1/partnerships/accept (User 1 own invite → 409)" "409" "$BODY" "$STATUS"
fi

# 5. User 2 accepts the invite → 200 (partnership activated)
if [ -n "$INVITE_CODE" ]; then
  RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/partnerships/accept" \
    -H "Authorization: Bearer $TOKEN2" \
    -H "Content-Type: application/json" \
    -d "{\"inviteCode\":\"$INVITE_CODE\"}")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "POST /api/v1/partnerships/accept (User 2 accepts → activated)" "200" "$BODY" "$STATUS"
  PARTNERSHIP_ID=$(save_value "$BODY" "id")
  # Validate partnership response data
  check_field "$BODY" "status" "active" "partnership status should be active"
  check_contains "$BODY" "partner" "partnership should have partner object"
fi

# 6. User 1 GET /partnerships/me → 200 (now active)
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/partnerships/me" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/partnerships/me (User 1, active)" "200" "$BODY" "$STATUS"

# 7. User 2 GET /partnerships/me → 200
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/partnerships/me" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/partnerships/me (User 2, active)" "200" "$BODY" "$STATUS"

# 8. User 1 delete own pending invite (no pending invite, only active → 404)
RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/partnerships/invite" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "DELETE /api/v1/partnerships/invite (no pending → 404)" "404" "$BODY" "$STATUS"

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
BREACH_ID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); inner=d.get('data',d); print(inner.get('id','') if isinstance(inner,dict) else '')" 2>/dev/null || echo "")

# Re-create Instagram limit and check status now shows exceeded
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.instagram.android","appLabel":"Instagram","dailyLimitMinutes":30,"breachThreshold":3}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
INSTA_LIMIT_ID=$(echo "$BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); inner=d.get('data',d); print(inner.get('id','') if isinstance(inner,dict) else '')" 2>/dev/null || echo "")
[ -n "$INSTA_LIMIT_ID" ] && test_endpoint "POST /api/v1/limits (re-create for status check)" "201" "$BODY" "$STATUS"

if [ -n "$INSTA_LIMIT_ID" ]; then
  RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/limits/status" \
    -H "Authorization: Bearer $TOKEN")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "GET /api/v1/limits/status (after breach, exceeded)" "200" "$BODY" "$STATUS"
  # Validate exceeded state
  check_field "$BODY" "exceeded" "True" "limit should be exceeded after breach"
  check_contains "$BODY" "breachesRemaining" "breachesRemaining should exist after breach"

  # Cleanup
  RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/limits/$INSTA_LIMIT_ID" \
    -H "Authorization: Bearer $TOKEN")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "DELETE /api/v1/limits/$INSTA_LIMIT_ID (cleanup)" "204" "$BODY" "$STATUS"
fi

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

# Get partner breaches (User 2 sees User 1's breaches via partnership)
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/breaches/partner?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/breaches/partner (User 2 sees User 1 breaches)" "200" "$BODY" "$STATUS"

# Get partner breaches by type
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/breaches/partner/type/SCREEN_TIME_EXCEEDED?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/breaches/partner/type/SCREEN_TIME_EXCEEDED (User 2)" "200" "$BODY" "$STATUS"

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
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/streaks/partner (User 2 sees User 1 streak)" "200" "$BODY" "$STATUS"

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

RESP=$(curl -s -w "\n%{http_code}" "$BASE/v3/api-docs")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1 | head -c 100)
test_endpoint "GET /v3/api-docs (spec)" "200" "$BODY" "$STATUS"

# ============================================================
# PHASE 10: Notifications
# ============================================================
echo -e "\n${YELLOW}═══ Phase 10: Notifications ═══${NC}"

RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/notifications?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/notifications (paginated)" "200" "$BODY" "$STATUS"
# Validate Page structure
check_contains "$BODY" "content" "notifications list should have content field"
check_contains "$BODY" "totalElements" "notifications list should have totalElements"
check_contains "$BODY" "totalPages" "notifications list should have totalPages"
check_contains "$BODY" "number" "notifications list should have number"

RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/notifications/unread-count" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/notifications/unread-count" "200" "$BODY" "$STATUS"

RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/api/v1/notifications/read-all" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "PUT /api/v1/notifications/read-all" "200" "$BODY" "$STATUS"

RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/notifications/preferences" \
  -H "Authorization: Bearer $TOKEN")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/notifications/preferences (defaults)" "200" "$BODY" "$STATUS"
check_field "$BODY" "breachAlerts" "True" "default breachAlerts should be true"
check_field "$BODY" "streakBroken" "True" "default streakBroken should be true"
check_field "$BODY" "appLocked" "True" "default appLocked should be true"

RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/api/v1/notifications/preferences" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"breachAlerts":false,"streakBroken":true,"appLocked":true}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "PUT /api/v1/notifications/preferences (breachAlerts off)" "200" "$BODY" "$STATUS"
check_field "$BODY" "breachAlerts" "False" "breachAlerts should be false after toggle"
check_field "$BODY" "streakBroken" "True" "streakBroken should remain true"

RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/api/v1/notifications/preferences" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"breachAlerts":true}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "PUT /api/v1/notifications/preferences (breachAlerts back on)" "200" "$BODY" "$STATUS"

# Error path — invalid preference field (should still 200 via partial map)
RESP=$(curl -s -w "\n%{http_code}" -X PUT "$BASE/api/v1/notifications/preferences" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"unknownField":false}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "PUT /api/v1/notifications/preferences (unknown field ignored)" "200" "$BODY" "$STATUS"

# ============================================================
# PHASE 11: Blocked Apps / Lockout
# ============================================================
echo -e "\n${YELLOW}═══ Phase 11: Blocked Apps / Lockout ═══${NC}"

# 1. User 2 creates a limit with low breachThreshold for lockout testing
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits" \
  -H "Authorization: Bearer $TOKEN2" \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.instagram.android","appLabel":"Instagram","dailyLimitMinutes":30,"breachThreshold":2}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits (User 2, breachThreshold=2)" "201" "$BODY" "$STATUS"
PARTNER_LIMIT_ID=$(save_value "$BODY" "id")

# 2. List blocked apps (should be empty)
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/limits/blocked" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/limits/blocked (User 2, empty)" "200" "$BODY" "$STATUS"

# 3. User 1 locks an app for partner (User 2)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits/blocked" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.instagram.android","appLabel":"Instagram"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits/blocked (User 1 locks for partner)" "200" "$BODY" "$STATUS"
check_field "$BODY" "blockedBy" "partner" "locked app should be blockedBy=partner"
check_field "$BODY" "packageName" "com.instagram.android" "locked app package should match"

# 4. Verify User 2 sees the blocked app
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/limits/blocked" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/limits/blocked (User 2 sees locked app)" "200" "$BODY" "$STATUS"
check_field "$BODY" "blockedBy" "partner" "User 2 should see blockedBy=partner"
check_field "$BODY" "packageName" "com.instagram.android" "User 2 should see the locked package"

# 5. User 2 unlocks the app
RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/limits/blocked/com.instagram.android" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "DELETE /api/v1/limits/blocked/com.instagram.android (User 2 unlocks)" "204" "$BODY" "$STATUS"

# 6. Verify blocked list is empty again
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/limits/blocked" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/limits/blocked (User 2, empty after unlock)" "200" "$BODY" "$STATUS"

# 7. Delete non-existent blocked app (idempotent → 204)
RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/limits/blocked/com.nonexistent.app" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "DELETE /api/v1/limits/blocked/nonexistent (idempotent → 204)" "204" "$BODY" "$STATUS"

# 8. Partner lock without partnership → 404 (generate new invite but don't accept)
# First create a pending invite for User 1 (will fail because they already have active partnership)
# Actually User 1 has active partnership, so we'll use a different approach.
# We can test with no auth → 403
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits/blocked" \
  -H "Content-Type: application/json" \
  -d '{"packageName":"com.test","appLabel":"Test"}')
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits/blocked (no auth → 403)" "403" "$BODY" "$STATUS"

# ============================================================
# Auto-Lock tests
# ============================================================
echo -e "\n${YELLOW}── Auto-Lock Tests ──${NC}"

# 9. Create limit for User 2 with breachThreshold=1
TWITTER_PKG="com.twitter.android"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits" \
  -H "Authorization: Bearer $TOKEN2" \
  -H "Content-Type: application/json" \
  -d "{\"packageName\":\"$TWITTER_PKG\",\"appLabel\":\"Twitter\",\"dailyLimitMinutes\":30,\"breachThreshold\":1}")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits (User 2, Twitter, threshold=1)" "201" "$BODY" "$STATUS"
AUTO_LOCK_TWITTER_ID=$(save_value "$BODY" "id")

# 10. Auto-lock with no breaches yet → threshold not reached
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits/$TWITTER_PKG/auto-lock" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits/{package}/auto-lock (User 2, 0 breaches)" "200" "$BODY" "$STATUS"
check_field "$BODY" "locked" "False" "should not lock with 0 breaches"
check_contains "$BODY" "message" "auto-lock response should have message"

# 11. Report a screen-time breach for User 2 (triggers auto-lock internally)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/breaches/screen-time" \
  -H "Authorization: Bearer $TOKEN2" \
  -H "Content-Type: application/json" \
  -d "{\"packageName\":\"$TWITTER_PKG\",\"appLabel\":\"Twitter\",\"limitMinutes\":30,\"actualMinutes\":45}")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/breaches/screen-time (User 2, Twitter, trigger auto-lock)" "201" "$BODY" "$STATUS"

# 12. Auto-lock again → now locked (breach met threshold)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits/$TWITTER_PKG/auto-lock" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits/{package}/auto-lock (User 2, after breach, locked)" "200" "$BODY" "$STATUS"
check_field "$BODY" "locked" "True" "should lock after reaching threshold"
check_contains "$BODY" "blockedApp" "response should contain blockedApp when locked"
check_contains "$BODY" "message" "response should have message"

# Validate blocked app fields via nested field check
BLOCKED_BY=$(echo "$BODY" | python3 -c "
import sys,json
d=json.load(sys.stdin)
inner=d.get('data',d)
ba=inner.get('blockedApp',{})
print(ba.get('blockedBy','__MISSING__'))
" 2>/dev/null || echo "__PARSE_ERR__")
if [ "$BLOCKED_BY" = "auto" ]; then
  echo -e "    ${GREEN}✓ blockedApp.blockedBy=auto${NC}"
else
  echo -e "    ${RED}✗ blockedApp.blockedBy expected 'auto' got '$BLOCKED_BY'${NC}"
  FAILED=$((FAILED + 1))
fi

EXPIRES=$(echo "$BODY" | python3 -c "
import sys,json
d=json.load(sys.stdin)
inner=d.get('data',d)
ba=inner.get('blockedApp',{})
print(ba.get('expiresAt','__MISSING__'))
" 2>/dev/null || echo "__PARSE_ERR__")
if [ "$EXPIRES" != "__MISSING__" ] && [ "$EXPIRES" != "" ]; then
  echo -e "    ${GREEN}✓ blockedApp.expiresAt present${NC}"
else
  echo -e "    ${RED}✗ blockedApp.expiresAt missing or empty${NC}"
  FAILED=$((FAILED + 1))
fi

# 13. Verify blocked list shows the auto-locked app
RESP=$(curl -s -w "\n%{http_code}" "$BASE/api/v1/limits/blocked" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "GET /api/v1/limits/blocked (User 2, auto-locked app visible)" "200" "$BODY" "$STATUS"
check_field "$BODY" "blockedBy" "auto" "auto-locked app should show blockedBy=auto"
check_field "$BODY" "packageName" "$TWITTER_PKG" "blocked app package should match"

# 14. Auto-lock again → idempotent, already locked
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits/$TWITTER_PKG/auto-lock" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits/{package}/auto-lock (User 2, already locked, idempotent)" "200" "$BODY" "$STATUS"
check_field "$BODY" "locked" "True" "should still be locked on repeat call"
# Python check for "already locked" in message
MSG_HAS_ALREADY=$(echo "$BODY" | python3 -c "
import sys,json
d=json.load(sys.stdin)
inner=d.get('data',d)
msg=inner.get('message','')
print('true' if 'already' in msg.lower() else 'false')
" 2>/dev/null || echo "false")
if [ "$MSG_HAS_ALREADY" = "true" ]; then
  echo -e "    ${GREEN}✓ message indicates already locked${NC}"
else
  echo -e "    ${RED}✗ message should indicate already locked${NC}"
  FAILED=$((FAILED + 1))
fi

# 15. Unlock the app
RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/limits/blocked/$TWITTER_PKG" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "DELETE /api/v1/limits/blocked/$TWITTER_PKG (User 2, unlock)" "204" "$BODY" "$STATUS"

# 16. Auto-lock on package with no AppLimit → 404
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits/com.nonexistent.app/auto-lock" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits/{package}/auto-lock (no limit → 404)" "404" "$BODY" "$STATUS"

# 17. Create another limit with threshold=3, report 1 breach → not locked
FB_PKG="com.facebook.android"
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits" \
  -H "Authorization: Bearer $TOKEN2" \
  -H "Content-Type: application/json" \
  -d "{\"packageName\":\"$FB_PKG\",\"appLabel\":\"Facebook\",\"dailyLimitMinutes\":30,\"breachThreshold\":3}")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits (User 2, Facebook, threshold=3)" "201" "$BODY" "$STATUS"
AUTO_LOCK_FACEBOOK_ID=$(save_value "$BODY" "id")

# Report 1 breach
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/breaches/screen-time" \
  -H "Authorization: Bearer $TOKEN2" \
  -H "Content-Type: application/json" \
  -d "{\"packageName\":\"$FB_PKG\",\"appLabel\":\"Facebook\",\"limitMinutes\":30,\"actualMinutes\":45}")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/breaches/screen-time (User 2, Facebook, 1 breach)" "201" "$BODY" "$STATUS"

# Auto-lock should say not reached (1/3)
RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE/api/v1/limits/$FB_PKG/auto-lock" \
  -H "Authorization: Bearer $TOKEN2")
STATUS=$(echo "$RESP" | tail -1)
BODY=$(echo "$RESP" | head -n -1)
test_endpoint "POST /api/v1/limits/{package}/auto-lock (User 2, 1/3 breaches)" "200" "$BODY" "$STATUS"
check_field "$BODY" "locked" "False" "should not lock with only 1/3 breaches"
MSG_REMAINING=$(echo "$BODY" | python3 -c "
import sys,json
d=json.load(sys.stdin)
inner=d.get('data',d)
msg=inner.get('message','')
print('true' if 'remaining' in msg.lower() else 'false')
" 2>/dev/null || echo "false")
if [ "$MSG_REMAINING" = "true" ]; then
  echo -e "    ${GREEN}✓ message mentions remaining breaches${NC}"
else
  echo -e "    ${RED}✗ message should mention remaining breaches${NC}"
  FAILED=$((FAILED + 1))
fi

# 18. Cleanup all User 2's limits
echo -e "\n${YELLOW}── Auto-Lock Cleanup ──${NC}"

for LID in "$PARTNER_LIMIT_ID" "$AUTO_LOCK_TWITTER_ID" "$AUTO_LOCK_FACEBOOK_ID"; do
  if [ -n "$LID" ]; then
    RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/limits/$LID" \
      -H "Authorization: Bearer $TOKEN2")
    STATUS=$(echo "$RESP" | tail -1)
    BODY=$(echo "$RESP" | head -n -1)
    test_endpoint "DELETE /api/v1/limits/$LID (User 2 cleanup)" "204" "$BODY" "$STATUS"
  fi
done

# ============================================================
# PHASE 12: Cleanup — dissolve partnership
# ============================================================
echo -e "\n${YELLOW}═══ Phase 12: Cleanup ═══${NC}"

if [ -n "$PARTNERSHIP_ID" ]; then
  RESP=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE/api/v1/partnerships/$PARTNERSHIP_ID" \
    -H "Authorization: Bearer $TOKEN")
  STATUS=$(echo "$RESP" | tail -1)
  BODY=$(echo "$RESP" | head -n -1)
  test_endpoint "DELETE /api/v1/partnerships/$PARTNERSHIP_ID (dissolve)" "204" "$BODY" "$STATUS"
fi

# ============================================================
# ============================================================
echo -e "\n${YELLOW}══════════════════════════════════════════${NC}"
echo -e "  ${GREEN}Passed: $PASSED${NC}  ${RED}Failed: $FAILED${NC}  Total: $TOTAL"
echo -e "${YELLOW}══════════════════════════════════════════${NC}"

if [ $FAILED -gt 0 ]; then
  exit 1
fi
