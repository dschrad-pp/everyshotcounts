# CRM API Integration Documentation

## Overview

This document describes the simplified API endpoints required for integrating a new CRM system to replace LeagueApps. The API focuses on the essential data needed to:

1. Create/update users in the system
2. Establish coach-athlete relationships through team assignments

---

## Base URL

```
https://your-crm-api.com/api/v1
```

---

## Authentication

All API requests (except the authentication endpoint) require an access token in the Authorization header.

### Getting an Access Token

Before making API requests, you must obtain an access token using the authentication endpoint.

### Using the Access Token

Once you have an access token, include it in all API requests:

```
Authorization: Bearer <access_token>
```

---

## API Endpoints

### 0. Authenticate / Get Access Token

Obtains an access token for API authentication. This endpoint should be called first before making any other API requests.

#### Endpoint

```
POST /auth/token
```

#### Request Body

**Option 1: Client Credentials (Recommended for Server-to-Server)**

```json
{
  "grant_type": "client_credentials",
  "client_id": "your_client_id",
  "client_secret": "your_client_secret"
}
```

**Option 2: Username/Password**

```json
{
  "grant_type": "password",
  "username": "your_username",
  "password": "your_password"
}
```

#### Request Example

```http
POST /api/v1/auth/token
Content-Type: application/json

{
  "grant_type": "client_credentials",
  "client_id": "your_client_id",
  "client_secret": "your_client_secret"
}
```

#### Response Example (Success)

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "read write"
}
```

#### Response Example (Error)

```json
{
  "error": "invalid_client",
  "error_description": "Invalid client credentials"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `access_token` | String | **Required** - Access token to use in Authorization header |
| `token_type` | String | Token type (usually "Bearer") |
| `expires_in` | Integer | Token expiration time in seconds (e.g., 3600 = 1 hour) |
| `scope` | String | Token scope/permissions |

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `grant_type` | String | **Yes** | Must be `"client_credentials"` or `"password"` |
| `client_id` | String | **Yes** (for client_credentials) | Your API client ID |
| `client_secret` | String | **Yes** (for client_credentials) | Your API client secret |
| `username` | String | **Yes** (for password) | Username for authentication |
| `password` | String | **Yes** (for password) | Password for authentication |

#### Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `invalid_client` | 401 | Invalid client_id or client_secret |
| `invalid_grant` | 401 | Invalid grant type or credentials |
| `invalid_request` | 400 | Missing required parameters |
| `unsupported_grant_type` | 400 | Grant type not supported |

#### Notes

- Access tokens typically expire after 1 hour (3600 seconds)
- Store the token securely and refresh it before expiration
- Include the token in the `Authorization: Bearer <token>` header for all subsequent requests
- If a token expires, call this endpoint again to get a new token

---

### 1. Get User Registrations (Simplified)

### 1. Get User Registrations (Simplified)

Fetches user registration data with only the essential fields needed to create users and establish coach-athlete connections.

#### Endpoint

```
GET /registrations
```

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `lastUpdated` | Long (timestamp) | No | Fetch only registrations updated after this timestamp (milliseconds since epoch). Used for incremental sync. |
| `page` | Integer | No | Page number (default: 1) |
| `pageSize` | Integer | No | Number of records per page (default: 100, max: 1000) |

#### Request Example

```http
GET /api/v1/registrations?lastUpdated=1727807311000&page=1&pageSize=100
Authorization: Bearer <access_token>
```

**Note**: Replace `<access_token>` with the token obtained from the `/auth/token` endpoint.

#### Response Example

```json
{
  "status": "success",
  "data": [
    {
      "registrationId": 100001,
      "userId": 67890,
      "username": "john.smith",
      "email": "john.smith@example.com",
      "firstName": "John",
      "lastName": "Smith",
      "phoneNumber": "+1234567890",
      "birthDate": 946684800000,
      "role": "ATHLETE",
      "teamId": 200,
      "paymentStatus": "PAID",
      "subscriptionStartDate": 1704067200000,
      "subscriptionEndDate": 1719792000000,
      "lastUpdated": 1727807311000
    },
    {
      "registrationId": 100002,
      "userId": 67891,
      "username": "jane.coach",
      "email": "jane.coach@example.com",
      "firstName": "Jane",
      "lastName": "Coach",
      "phoneNumber": "+1234567891",
      "birthDate": 631152000000,
      "role": "COACH",
      "teamId": 200,
      "paymentStatus": "PAID",
      "subscriptionStartDate": 1704067200000,
      "subscriptionEndDate": 1719792000000,
      "lastUpdated": 1727807311000
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 100,
    "totalRecords": 150,
    "totalPages": 2,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

#### Response Fields (Required)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `registrationId` | Long | **Yes** | Unique registration identifier |
| `userId` | Long | **Yes** | User ID in CRM (used to link registrations to the same user) |
| `username` | String | **Yes** | Unique username for the user |
| `email` | String | **Yes** | Email address (used for user identification and communication) |
| `firstName` | String | **Yes** | User's first name |
| `lastName` | String | **Yes** | User's last name |
| `phoneNumber` | String | **Yes** | Phone number |
| `birthDate` | Long | **Yes** | Birth date (timestamp in milliseconds since Unix epoch) |
| `role` | String | **Yes** | User role: **"COACH"** or **"ATHLETE"** (case-insensitive) |
| `teamId` | Long | **Yes** | **Team ID - CRITICAL for coach-athlete connection. Users with the same teamId are connected.** |
| `paymentStatus` | String | **Yes** | Payment status: **"PAID"**, **"TRIAL"**, **"PARTIAL"**, **"UNPAID"** (case-insensitive) |
| `subscriptionStartDate` | Long | **Yes** | Subscription start date (timestamp in milliseconds since Unix epoch) |
| `subscriptionEndDate` | Long | **Yes** | Subscription end date (timestamp in milliseconds since Unix epoch) |
| `lastUpdated` | Long | **Yes** | Last update timestamp (milliseconds since Unix epoch) - used for incremental sync |

---

## Critical Fields for Coach-Athlete Connection

### How Coach-Athlete Connection Works

1. **`teamId`** (Long) - **MOST IMPORTANT**
   - Users with the same `teamId` are assigned to the same team
   - This creates the coach-athlete relationship
   - Example: If Coach has `teamId: 200` and Athlete has `teamId: 200`, they are connected

2. **`role`** (String) - **REQUIRED**
   - Determines if user is a COACH or ATHLETE
   - Valid values: `"COACH"` or `"ATHLETE"` (case-insensitive)
   - Used to assign the correct role in the system

### Example Connection Scenario

```json
// Coach Registration
{
  "registrationId": 100002,
  "userId": 67891,
  "username": "jane.coach",
  "email": "jane.coach@example.com",
  "firstName": "Jane",
  "lastName": "Coach",
  "phoneNumber": "+1234567891",
  "birthDate": 631152000000,
  "role": "COACH",
  "teamId": 200,  // ← Same teamId
  "paymentStatus": "PAID",
  "subscriptionStartDate": 1704067200000,
  "subscriptionEndDate": 1719792000000,
  "lastUpdated": 1727807311000
}

// Athlete Registration
{
  "registrationId": 100001,
  "userId": 67890,
  "username": "john.smith",
  "email": "john.smith@example.com",
  "firstName": "John",
  "lastName": "Smith",
  "phoneNumber": "+1234567890",
  "birthDate": 946684800000,
  "role": "ATHLETE",
  "teamId": 200,  // ← Same teamId = CONNECTED!
  "paymentStatus": "PAID",
  "subscriptionStartDate": 1704067200000,
  "subscriptionEndDate": 1719792000000,
  "lastUpdated": 1727807311000
}
```

**Result**: Coach and Athlete are connected because they share the same `teamId: 200`

---

## Integration Flow

### Step 1: Authenticate

1. **Get Access Token**
   ```
   POST /api/v1/auth/token
   Content-Type: application/json
   
   {
     "grant_type": "client_credentials",
     "client_id": "your_client_id",
     "client_secret": "your_client_secret"
   }
   ```
   - Store the `access_token` from the response
   - Use this token in all subsequent API requests

### Step 2: Initial Full Sync

1. **Fetch All Registrations**
   ```
   GET /api/v1/registrations?page=1&pageSize=1000
   Authorization: Bearer <access_token>
   ```
   - Continue paginating until all registrations are fetched
   - Process each registration to create/update users

2. **Process Each Registration**
   - Create or update user with: `username`, `email`, `firstName`, `lastName`, `phoneNumber`, `birthDate`
   - Assign role based on `role` field (COACH or ATHLETE)
   - Assign user to team based on `teamId`
   - Users with the same `teamId` are automatically connected (coach-athlete relationship)

3. **Store Last Sync Timestamp**
   - Save the maximum `lastUpdated` value for next incremental sync

### Incremental Sync

1. **Get Access Token** (if expired)
   ```
   POST /api/v1/auth/token
   ```
   - Refresh token if it has expired

2. **Get Last Sync Timestamp**
   - Retrieve the last successful sync timestamp from your system

3. **Fetch Updated Registrations**
   ```
   GET /api/v1/registrations?lastUpdated=<timestamp>&page=1&pageSize=1000
   Authorization: Bearer <access_token>
   ```
   - Only fetch registrations updated since last sync

3. **Process Updates**
   - Update existing users or create new ones
   - Update team memberships if `teamId` changed
   - Update roles if `role` changed
   - Re-establish coach-athlete connections based on updated `teamId`

4. **Update Sync Timestamp**
   - Store the current maximum `lastUpdated` value for the next sync

---

## Data Requirements

### Required Fields

All fields in the response are **required** and must be provided:

- `registrationId` - Must be unique
- `userId` - Must be consistent across registrations for the same user
- `username` - Must be unique
- `email` - Must be valid email format
- `firstName` - Cannot be empty
- `lastName` - Cannot be empty
- `phoneNumber` - Must be provided
- `birthDate` - Must be valid timestamp (milliseconds)
- `role` - Must be "COACH" or "ATHLETE"
- `teamId` - Must be provided (cannot be null)
- `paymentStatus` - Must be provided (see valid values below)
- `subscriptionStartDate` - Must be valid timestamp (milliseconds)
- `subscriptionEndDate` - Must be valid timestamp (milliseconds)
- `lastUpdated` - Must be valid timestamp (milliseconds)

### Data Validation

- `email`: Must be a valid email format
- `role`: Must be exactly "COACH" or "ATHLETE" (case-insensitive)
- `teamId`: Must be a positive integer
- `birthDate`: Must be a valid timestamp (milliseconds since Unix epoch)
- `paymentStatus`: Must be one of: **"PAID"**, **"TRIAL"**, **"PARTIAL"**, **"UNPAID"** (case-insensitive)
- `subscriptionStartDate`: Must be a valid timestamp (milliseconds since Unix epoch)
- `subscriptionEndDate`: Must be a valid timestamp (milliseconds since Unix epoch) and should be after `subscriptionStartDate`
- `lastUpdated`: Must be a valid timestamp (milliseconds since Unix epoch)

### Payment Status Values

| Value | Description |
|-------|-------------|
| `PAID` | User has paid in full |
| `TRIAL` | User is on a trial period |
| `PARTIAL` | User has made partial payment |
| `UNPAID` | User has not paid |

---

## Pagination

All list endpoints support pagination.

### Request Parameters

- `page`: Page number (1-indexed, default: 1)
- `pageSize`: Records per page (default: 100, max: 1000)

### Response Structure

```json
{
  "data": [...],
  "pagination": {
    "page": 1,
    "pageSize": 100,
    "totalRecords": 500,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

## Timestamps

All timestamps must be in **milliseconds since Unix epoch** (January 1, 1970 UTC).

Examples:
- `946684800000` = January 1, 2000 00:00:00 UTC
- `1727807311000` = October 1, 2024 12:15:11 UTC

---

## Error Handling

### Standard Error Response

```json
{
  "status": "error",
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message"
  },
  "timestamp": 1727807311000
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `UNAUTHORIZED` | 401 | Invalid or missing authentication |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `VALIDATION_ERROR` | 400 | Invalid request parameters or missing required fields |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_ERROR` | 500 | Server error |

### Rate Limiting

If rate limiting is implemented, include the following headers:

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1727807311000
```

---

## Health Check

### Endpoint

```
GET /health
```

#### Request Example

```http
GET /api/v1/health
Authorization: Bearer <access_token>
```

**Note**: Replace `<access_token>` with the token obtained from the `/auth/token` endpoint.

#### Response Example

```json
{
  "status": "success",
  "message": "API is healthy",
  "timestamp": 1727807311000
}
```

---

## Authentication Flow Example

### Complete Example: Getting Token and Making API Call

```bash
# Step 1: Get access token
TOKEN_RESPONSE=$(curl -X POST https://your-crm-api.com/api/v1/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "grant_type": "client_credentials",
    "client_id": "your_client_id",
    "client_secret": "your_client_secret"
  }')

# Extract access token
ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.access_token')

# Step 2: Use token to fetch registrations
curl -X GET "https://your-crm-api.com/api/v1/registrations?page=1&pageSize=100" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

### Token Refresh Strategy

- Tokens typically expire after 1 hour
- Check `expires_in` value from token response
- Refresh token before expiration
- Implement token caching to avoid unnecessary authentication calls

---

## Summary

### What We Need

1. **User Information** (to create users):
   - `username`, `email`, `firstName`, `lastName`, `phoneNumber`, `birthDate`

2. **Role Assignment** (to assign COACH or ATHLETE):
   - `role` (must be "COACH" or "ATHLETE")

3. **Team Assignment** (to connect coach and athlete):
   - `teamId` (users with same teamId are connected)

4. **Payment & Subscription Information** (to track user payments):
   - `paymentStatus` (PAID, TRIAL, PARTIAL, UNPAID)
   - `subscriptionStartDate` (when subscription starts)
   - `subscriptionEndDate` (when subscription ends)

5. **Sync Support**:
   - `lastUpdated` (for incremental sync)
   - `registrationId` (unique identifier)
   - `userId` (to link registrations)

### What We Don't Need

- Payment information
- Program details
- Parent information
- Address details (beyond what's in user creation)
- Newsletter preferences
- Grade level
- School information
- Any other metadata not directly used for user creation or team assignment

---

## Support

For questions or issues with the API integration, please contact:
- Email: support@your-crm.com
- Documentation: https://docs.your-crm.com

---

## Changelog

### Version 2.1.0 (2024-10-01)
- Added authentication endpoint (`POST /auth/token`)
- Added payment status and subscription date fields
- Updated integration flow with authentication steps

### Version 2.0.0 (2024-10-01)
- Simplified API to only include essential fields
- Removed unnecessary fields (payment, program, parent info, etc.)
- Focus on user creation and coach-athlete connection only

### Version 1.0.0 (2024-10-01)
- Initial API specification
