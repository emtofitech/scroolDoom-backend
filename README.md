# ScrollDoom API

Backend API for ScrollDoom — a social accountability app that helps users limit app usage with a partner.

## Tech Stack

- Java 17, Spring Boot 3.2, Maven
- MongoDB Atlas (Spring Data MongoDB)
- Firebase Admin SDK 9.2.0 (Auth + FCM)
- Lombok

## Local Setup

### Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB (local or Atlas)
- Firebase Admin SDK service account JSON file

### Environment Variables

| Variable | Description | Example |
|---|---|---|
| `MONGODB_URI` | MongoDB connection string | `mongodb+srv://user:pass@cluster.mongodb.net/scrolldoom` |
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase service account JSON | `/etc/secrets/firebase-service-account.json` |

### Run Locally

```bash
# Set environment variables
export MONGODB_URI="mongodb://localhost:27017/scrolldoom"
export FIREBASE_CREDENTIALS_PATH="/path/to/serviceAccount.json"

# Build
./mvnw clean package -DskipTests

# Run
java -jar target/scrolldoom-0.0.1-SNAPSHOT.jar

# Or using Maven
./mvnw spring-boot:run
```

### Run Tests

```bash
./mvnw test
```

## API Endpoints

Base path: `/api/v1`

### Health

```
GET /api/v1/health

Response 200:
{
  "status": "UP"
}
```

### Auth

```
POST /api/v1/auth/register
Content-Type: application/json

Request:
{
  "firebaseUid": "abc123...",
  "displayName": "John Doe",
  "email": "john@example.com",
  "fcmToken": "fcm-device-token-optional"
}

Response 201:
{
  "id": "664a1b2c...",
  "displayName": "John Doe",
  "email": "john@example.com",
  "avatarUrl": null,
  "firebaseUid": "abc123...",
  "createdAt": "2025-01-01T00:00:00.000+00:00"
}
```

### Users (authenticated)

```
GET /api/v1/users/me
Authorization: Bearer <firebase-id-token>

Response 200:
{
  "id": "664a1b2c...",
  "displayName": "John Doe",
  "email": "john@example.com",
  "avatarUrl": null,
  "firebaseUid": "abc123...",
  "createdAt": "2025-01-01T00:00:00.000+00:00"
}
```

```
PATCH /api/v1/users/me/fcm
Authorization: Bearer <firebase-id-token>
Content-Type: application/json

Request:
{
  "fcmToken": "new-fcm-device-token"
}

Response 200: (no body)
```

### App Limits (authenticated)

```
GET /api/v1/limits
Authorization: Bearer <firebase-id-token>

Response 200:
[
  {
    "id": "664a1b2c...",
    "packageName": "com.instagram.android",
    "appLabel": "Instagram",
    "dailyLimitMinutes": 30,
    "updatedAt": "2025-01-01T00:00:00.000+00:00"
  }
]
```

```
POST /api/v1/limits
Authorization: Bearer <firebase-id-token>
Content-Type: application/json

Request:
{
  "packageName": "com.instagram.android",
  "appLabel": "Instagram",
  "dailyLimitMinutes": 30
}

Response 201:
{
  "id": "664a1b2c...",
  "packageName": "com.instagram.android",
  "appLabel": "Instagram",
  "dailyLimitMinutes": 30,
  "updatedAt": "2025-01-01T00:00:00.000+00:00"
}
```

```
PUT /api/v1/limits/{id}
Authorization: Bearer <firebase-id-token>
Content-Type: application/json

Request:
{
  "dailyLimitMinutes": 45
}

Response 200:
{
  "id": "664a1b2c...",
  "packageName": "com.instagram.android",
  "appLabel": "Instagram",
  "dailyLimitMinutes": 45,
  "updatedAt": "2025-01-01T00:00:00.000+00:00"
}
```

```
DELETE /api/v1/limits/{id}
Authorization: Bearer <firebase-id-token>

Response 204: (no body)
```

### Partnerships (authenticated)

```
POST /api/v1/partnerships/invite
Authorization: Bearer <firebase-id-token>

Response 201:
{
  "id": "664a1b2c...",
  "status": "pending",
  "inviteCode": "XK4M9P",
  "createdAt": "2025-01-01T00:00:00.000+00:00"
}
```

```
POST /api/v1/partnerships/accept
Authorization: Bearer <firebase-id-token>
Content-Type: application/json

Request:
{
  "inviteCode": "XK4M9P"
}

Response 200:
{
  "id": "664a1b2c...",
  "status": "active",
  "inviteCode": "XK4M9P",
  "createdAt": "2025-01-01T00:00:00.000+00:00",
  "acceptedAt": "2025-01-01T01:00:00.000+00:00",
  "partner": {
    "id": "664a1b2c...",
    "displayName": "John Doe",
    "email": "john@example.com",
    "avatarUrl": null,
    "firebaseUid": "abc123...",
    "createdAt": "2025-01-01T00:00:00.000+00:00"
  }
}
```

```
GET /api/v1/partnerships/me
Authorization: Bearer <firebase-id-token>

Response 200:
{
  "id": "664a1b2c...",
  "status": "active",
  "inviteCode": "XK4M9P",
  "createdAt": "2025-01-01T00:00:00.000+00:00",
  "acceptedAt": "2025-01-01T01:00:00.000+00:00",
  "partner": { ... }
}
```

```
DELETE /api/v1/partnerships/{id}
Authorization: Bearer <firebase-id-token>

Response 204: (no body)
```

### Breaches (authenticated)

```
POST /api/v1/breaches
Authorization: Bearer <firebase-id-token>
Content-Type: application/json

Request:
{
  "packageName": "com.instagram.android",
  "appLabel": "Instagram",
  "limitMinutes": 30,
  "actualMinutes": 45
}

Response 201:
{
  "id": "664a1b2c...",
  "packageName": "com.instagram.android",
  "appLabel": "Instagram",
  "limitMinutes": 30,
  "actualMinutes": 45,
  "partnerNotified": true,
  "breachedAt": "2025-01-01T00:00:00.000+00:00"
}
```

```
GET /api/v1/breaches/me
Authorization: Bearer <firebase-id-token>

Response 200:
[ { ... }, { ... } ]
```

```
GET /api/v1/breaches/partner
Authorization: Bearer <firebase-id-token>

Response 200:
[ { ... }, { ... } ]
```

### Streaks (authenticated)

```
GET /api/v1/streaks/me
Authorization: Bearer <firebase-id-token>

Response 200:
{
  "currentStreak": 5,
  "longestStreak": 12,
  "lastSuccessDate": "2025-01-01",
  "updatedAt": "2025-01-01T00:00:00.000+00:00"
}
```

```
GET /api/v1/streaks/partner
Authorization: Bearer <firebase-id-token>

Response 200:
{
  "currentStreak": 3,
  "longestStreak": 8,
  "lastSuccessDate": "2024-12-30",
  "updatedAt": "2024-12-31T00:00:00.000+00:00"
}
```

## Railway Deployment

### 1. Push to GitHub

```bash
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/yourusername/scrolldoom.git
git push -u origin main
```

### 2. Deploy on Railway

- Go to [railway.app](https://railway.app) and create a new project
- Select **Deploy from GitHub repo**
- Connect your repository
- Add the environment variables (see table above)
- Railway auto-detects Java from the `pom.xml` and builds with Nixpacks
- The `railway.json` configures the health check path and restart policy

### 3. Verify Deployment

```bash
curl https://your-app.railway.app/api/v1/health
# {"status":"UP"}
```

### Environment Variables on Railway

| Variable | Railway Setting |
|---|---|
| `MONGODB_URI` | Your MongoDB Atlas connection string |
| `FIREBASE_CREDENTIALS_PATH` | Path to Firebase credentials file (use Railway volumes or raw JSON in env) |
