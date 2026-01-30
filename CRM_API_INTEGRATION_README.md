# CRM Integration API Documentation

This document describes the complete API flow for CRM user registration and activation, from initial sync to Keycloak registration.

## Base URL
```
http://103.99.202.227:8000
```

---

## Complete User Registration Flow

### **Step 1: Sync CRM Registrations**

Syncs registrations from the CRM API and creates users in the database (without Keycloak). Users receive an email with a 6-digit registration code.

**Endpoint:**
```bash
POST /api/crm_integration/sync
```

**Headers:**
```
Authorization: Bearer <admin_token>
Content-Type: application/json
```

**Query Parameters:**
- `lastUpdated` (optional): Timestamp to filter registrations. If not provided, uses stored timestamp.

**Curl Command:**
```bash
curl --location --request POST 'http://103.99.202.227:8000/api/crm_integration/sync?lastUpdated=0' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ5R1F4SGRGRDk3Rjgta1BNSDRtTmZsSjgwYUtVRGlQRnVZaV9jR2RIWHJRIn0.eyJleHAiOjE3ODc3MjI3NjMsImlhdCI6MTc2MTg4OTE2MywianRpIjoiNWE3YmU5OGEtYWQ1Yi00MWQ2LWIzM2UtOWM5ZDUxYjdjYTRmIiwiaXNzIjoiaHR0cDovLzEwLjEwLjAuMjE1OjgwODAvcmVhbG1zL3Rocm9uZXNfcmVhbG0iLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZmZlMjJiNGUtMTE5ZS00YzdlLWFlYTAtYWQ4MTMzMzc0NjM0IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGhyb25lc19jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiYjk2M2MwNjctYzg3Mi00NTVkLWE0ZTMtODBlOGUzYTk5YzFmIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJBVEhMRVRFIiwidW1hX2F1dGhvcml6YXRpb24iLCJkZWZhdWx0LXJvbGVzLXRocm9uZXNfcmVhbG0iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJzaWQiOiJiOTYzYzA2Ny1jODcyLTQ1NWQtYTRlMy04MGU4ZTNhOTljMWYiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6ImV2ZSBkZCIsInByZWZlcnJlZF91c2VybmFtZSI6ImV2ZSIsImdpdmVuX25hbWUiOiJldmUiLCJmYW1pbHlfbmFtZSI6ImRkIiwiZW1haWwiOiJhbmtpdGNoYXVoYW4wOTA2QGdtYWlsLmNvbSJ9.R9bxXuGdfmbuYRURBDJX5mXSdDn_ZHVwZaO2byf2Aaahkimt6PNUQ5rzLjbZHZ7V5yufL8tk83yI7cFhN69Ab4JVereMGxbqzS-zbsAkc2cKlSmUrsfHQ6mV_6OD14xj1GEAc9pYE9d6-VUTymZiyBChlteduMX4DqP9RXaV4gj70Vzdab31Kk7bi049tkUgoiQTsbEz9laEiqvyRw5y6y2cnU52Cjti6GKzq2G93Zexfrvbd94HSlg0hNa_7FMA6xgOIEdGz1PEv4aYKseocFM0wcknIuScuE_1wulUdKfjYg71gEAXaMuB6xTI3YHLql0b8S60q82DmUaDHLFKRA' \
--header 'Content-Type: application/json'
```

**Response (Success):**
```json
{
  "status": "SUCCESS",
  "registrationsFetched": 9,
  "registrationsProcessed": 9,
  "registrationsCreated": 5,
  "registrationsUpdated": 4,
  "usersProcessed": 3,
  "usersCreated": 2,
  "usersUpdated": 1,
  "lastSyncTimestamp": 0,
  "newSyncTimestamp": 1767938071325,
  "durationMs": 446
}
```

**What happens:**
- Fetches all registrations from CRM API
- Stores all registrations in `t_crm_registration` table
- For users with `PAID` or `PARTIAL` payment status:
  - Creates user in database (without Keycloak)
  - Generates 6-digit registration code
  - Stores code in user properties
  - Sends registration email with 6-digit code

---

### **Step 2: Send OTP (Email Verification)**

Sends a 6-digit OTP to the user's email for email verification. **Note:** User must exist in the database (created via CRM sync).

**Endpoint:**
```bash
POST /api/user/send-otp
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token> (optional, but recommended)
```

**Query Parameters:**
- `email` (required): User's email address

**Curl Command:**
```bash
curl --location --request POST 'http://103.99.202.227:8000/api/user/send-otp?email=garry.basketball%40yopmail.com' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ5R1F4SGRGRDk3Rjgta1BNSDRtTmZsSjgwYUtVRGlQRnVZaV9jR2RIWHJRIn0.eyJleHAiOjE3ODc3MjI3NjMsImlhdCI6MTc2MTg4OTE2MywianRpIjoiNWE3YmU5OGEtYWQ1Yi00MWQ2LWIzM2UtOWM5ZDUxYjdjYTRmIiwiaXNzIjoiaHR0cDovLzEwLjEwLjAuMjE1OjgwODAvcmVhbG1zL3Rocm9uZXNfcmVhbG0iLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZmZlMjJiNGUtMTE5ZS00YzdlLWFlYTAtYWQ4MTMzMzc0NjM0IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGhyb25lc19jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiYjk2M2MwNjctYzg3Mi00NTVkLWE0ZTMtODBlOGUzYTk5YzFmIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJBVEhMRVRFIiwidW1hX2F1dGhvcml6YXRpb24iLCJkZWZhdWx0LXJvbGVzLXRocm9uZXNfcmVhbG0iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJzaWQiOiJiOTYzYzA2Ny1jODcyLTQ1NWQtYTRlMy04MGU4ZTNhOTljMWYiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6ImV2ZSBkZCIsInByZWZlcnJlZF91c2VybmFtZSI6ImV2ZSIsImdpdmVuX25hbWUiOiJldmUiLCJmYW1pbHlfbmFtZSI6ImRkIiwiZW1haWwiOiJhbmtpdGNoYXVoYW4wOTA2QGdtYWlsLmNvbSJ9.R9bxXuGdfmbuYRURBDJX5mXSdDn_ZHVwZaO2byf2Aaahkimt6PNUQ5rzLjbZHZ7V5yufL8tk83yI7cFhN69Ab4JVereMGxbqzS-zbsAkc2cKlSmUrsfHQ6mV_6OD14xj1GEAc9pYE9d6-VUTymZiyBChlteduMX4DqP9RXaV4gj70Vzdab31Kk7bi049tkUgoiQTsbEz9laEiqvyRw5y6y2cnU52Cjti6GKzq2G93Zexfrvbd94HSlg0hNa_7FMA6xgOIEdGz1PEv4aYKseocFM0wcknIuScuE_1wulUdKfjYg71gEAXaMuB6xTI3YHLql0b8S60q82DmUaDHLFKRA' \
--data ''
```

**Response (Success):**
```json
{
  "status": 200,
  "message": "OTP sent to email successfully",
  "data": null
}
```

**Response (Error - User not found):**
```json
{
  "status": 400,
  "message": "User not found",
  "data": null
}
```

**What happens:**
- Checks if user exists in database by email
- Generates a 6-digit OTP
- Clears any existing OTPs for the email
- Stores OTP in database
- Sends OTP email to user

---

### **Step 3: Verify OTP**

Verifies the OTP sent to the user's email.

**Endpoint:**
```bash
POST /api/user/verify-otp
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer <token> (optional, but recommended)
```

**Query Parameters:**
- `email` (required): User's email address
- `otp` (required): 6-digit OTP code received via email

**Curl Command:**
```bash
curl --location --request POST 'http://103.99.202.227:8000/api/user/verify-otp?email=garry.basketball%40yopmail.com&otp=123456' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ5R1F4SGRGRDk3Rjgta1BNSDRtTmZsSjgwYUtVRGlQRnVZaV9jR2RIWHJRIn0.eyJleHAiOjE3ODc3MjI3NjMsImlhdCI6MTc2MTg4OTE2MywianRpIjoiNWE3YmU5OGEtYWQ1Yi00MWQ2LWIzM2UtOWM5ZDUxYjdjYTRmIiwiaXNzIjoiaHR0cDovLzEwLjEwLjAuMjE1OjgwODAvcmVhbG1zL3Rocm9uZXNfcmVhbG0iLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZmZlMjJiNGUtMTE5ZS00YzdlLWFlYTAtYWQ4MTMzMzc0NjM0IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGhyb25lc19jbGllbnQiLCJzZXNzaW9uX3N0YXRlIjoiYjk2M2MwNjctYzg3Mi00NTVkLWE0ZTMtODBlOGUzYTk5YzFmIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6WyIvKiJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJBVEhMRVRFIiwidW1hX2F1dGhvcml6YXRpb24iLCJkZWZhdWx0LXJvbGVzLXRocm9uZXNfcmVhbG0iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6InByb2ZpbGUgZW1haWwiLCJzaWQiOiJiOTYzYzA2Ny1jODcyLTQ1NWQtYTRlMy04MGU4ZTNhOTljMWYiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6ImV2ZSBkZCIsInByZWZlcnJlZF91c2VybmFtZSI6ImV2ZSIsImdpdmVuX25hbWUiOiJldmUiLCJmYW1pbHlfbmFtZSI6ImRkIiwiZW1haWwiOiJhbmtpdGNoYXVoYW4wOTA2QGdtYWlsLmNvbSJ9.R9bxXuGdfmbuYRURBDJX5mXSdDn_ZHVwZaO2byf2Aaahkimt6PNUQ5rzLjbZHZ7V5yufL8tk83yI7cFhN69Ab4JVereMGxbqzS-zbsAkc2cKlSmUrsfHQ6mV_6OD14xj1GEAc9pYE9d6-VUTymZiyBChlteduMX4DqP9RXaV4gj70Vzdab31Kk7bi049tkUgoiQTsbEz9laEiqvyRw5y6y2cnU52Cjti6GKzq2G93Zexfrvbd94HSlg0hNa_7FMA6xgOIEdGz1PEv4aYKseocFM0wcknIuScuE_1wulUdKfjYg71gEAXaMuB6xTI3YHLql0b8S60q82DmUaDHLFKRA' \
--data ''
```

**Response (Success):**
```json
{
  "status": 200,
  "message": "OTP verified successfully",
  "data": null
}
```

**Response (Error - Invalid OTP):**
```json
{
  "status": 400,
  "message": "Invalid or expired OTP",
  "data": null
}
```

**What happens:**
- Validates OTP against stored OTP for the email
- If valid, deletes the OTP from database
- Returns success response

---

### **Step 4: User Activation (Keycloak Registration)**

Activates the user and registers them in Keycloak. This is the final step that completes the registration process.

**Endpoint:**
```bash
POST /api/user/activation
```

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "email": "garry.basketball@yopmail.com",
  "username": "garrybasketball",
  "password": "UserPassword123!",
  "role": "ATHLETE"
}
```

**Curl Command:**
```bash
curl --location --request POST 'http://103.99.202.227:8000/api/user/activation' \
--header 'Content-Type: application/json' \
--data '{
  "email": "garry.basketball@yopmail.com",
  "username": "garrybasketball",
  "password": "UserPassword123!",
  "role": "ATHLETE"
}'
```

**Response (Success):**
```json
{
  "status": 200,
  "message": "User activated successfully",
  "data": {
    "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ5R1F4SGRGRDk3Rjgta1BNSDRtTmZsSjgwYUtVRGlQRnVZaV9jR2RIWHJRIn0...",
    "expires_in": 3600,
    "refresh_token": "...",
    "token_type": "Bearer"
  }
}
```

**Response (Error - User already registered):**
```json
{
  "status": 400,
  "message": "User already registered",
  "data": null
}
```

**Response (Error - Username collision):**
```json
{
  "status": 400,
  "message": "Username already exists",
  "data": null
}
```

**What happens:**
- Finds user by email (must exist from CRM sync)
- Validates user is not already registered in Keycloak
- Checks username is not already taken
- Updates username in database
- Registers user in Keycloak
- Assigns "ATHLETE" role in Keycloak
- Associates user with Keycloak group
- Saves Keycloak ID to database
- Returns access token (user is automatically logged in)

---

### **Step 5: Login (After Activation)**

User can login with username and password after activation.

**Endpoint:**
```bash
POST /api/sso/login
```

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "username": "garrybasketball",
  "password": "UserPassword123!"
}
```

**Curl Command:**
```bash
curl --location --request POST 'http://103.99.202.227:8000/api/sso/login' \
--header 'Content-Type: application/json' \
--data '{
  "username": "garrybasketball",
  "password": "UserPassword123!"
}'
```

**Response (Success):**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ5R1F4SGRGRDk3Rjgta1BNSDRtTmZsSjgwYUtVRGlQRnVZaV9jR2RIWHJRIn0...",
  "expires_in": 3600,
  "refresh_token": "...",
  "token_type": "Bearer"
}
```

**Response (Error):**
```
You shall not pass
```

---

## Complete Flow Summary

1. **CRM Sync** → Creates user in DB, sends email with 6-digit registration code
2. **Send OTP** (Optional) → Sends email verification OTP
3. **Verify OTP** (Optional) → Verifies email OTP
4. **User Activation** → User provides email/username/password → Registers in Keycloak
5. **Login** → User can login with username/password

## Important Notes

- **CRM Sync** only processes users with `PAID` or `PARTIAL` payment status
- Users are created in the database **without Keycloak** during sync
- The 6-digit registration code is stored in user properties and sent via email
- **Activation** is required to register the user in Keycloak
- After activation, the user is fully registered and can login
- OTP endpoints require the user to exist in the database (created via CRM sync)

## Error Handling

- All endpoints return appropriate HTTP status codes (200, 400, 403, 500)
- Error messages are included in the response body
- Check the `status` and `message` fields in the response for details

## Common Issues and Troubleshooting

### 403 Forbidden Error During User Activation

If you receive a `403 Forbidden` error when trying to activate a user, it means the Keycloak service account doesn't have the required permissions.

**Solution**: Follow the detailed guide in `KEYCLOAK_SERVICE_ACCOUNT_SETUP.md` to configure the service account permissions.

**Quick Fix**:
1. Go to Keycloak Admin Console → Select `thrones_realm`
2. Navigate to Clients → `thrones_client`
3. Enable "Service accounts enabled" in Settings
4. Go to "Service account roles" tab
5. Filter by `realm-management` client
6. Add `manage-users` and `view-users` roles

See `KEYCLOAK_SERVICE_ACCOUNT_SETUP.md` for detailed step-by-step instructions.

## Authentication

- Most endpoints require a Bearer token in the Authorization header
- The `/activation`, `/send-otp`, and `/verify-otp` endpoints are `@PermitAll` (no authentication required)
- The `/login` endpoint is public (no authentication required)
