# CRM Webhook Setup Guide

## Overview

A webhook endpoint has been created to allow the CRM system to register new or updated registrations in real-time. This complements the existing CRON job that syncs CRM data every 20 seconds.

## CRON Job Configuration

The CRON job is already configured and running:
- **Status**: Enabled (`crm.sync.enabled=true`)
- **Schedule**: Every 20 seconds (`crm.sync.cron=*/20 * * * * ?`)
- **Location**: `CrmSyncScheduler.java`

The CRON job automatically syncs all registrations from the CRM API and processes them to create/update users.

## Webhook Endpoint

### Endpoint Details

**URL**: `POST /api/crm_integration/webhook/register`

**Base URL**: `http://103.99.202.227:8000` (or your server URL)

**Full URL**: `http://103.99.202.227:8000/api/crm_integration/webhook/register`

**Authentication**: API Key required (see Security section below)

**Content-Type**: `application/json`

**Required Headers**:
- `X-CRM-Webhook-Key`: API key for authentication (required)
- `Content-Type`: `application/json` (required)

### Request Body

The webhook expects a JSON payload matching the `CrmRegistration` model:

```json
{
  "registrationId": 12345,
  "userId": 67890,
  "username": "johndoe",
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phoneNumber": "+1234567890",
  "role": "ATHLETE",
  "teamId": 1,
  "paymentStatus": "PAID",
  "subscriptionStartDate": 1704067200000,
  "subscriptionEndDate": 1735689600000,
  "lastUpdated": 1704067200000
}
```

### Required Fields

- `registrationId` (Long): Unique identifier for the registration
- `email` (String): User's email address

### Optional Fields

- `userId` (Long)
- `username` (String)
- `firstName` (String)
- `lastName` (String)
- `phoneNumber` (String)
- `role` (String): Defaults to "ATHLETE" if not provided
- `teamId` (Long)
- `paymentStatus` (String): Must be "PAID" or "PARTIAL" for user creation
- `subscriptionStartDate` (Long): Timestamp in milliseconds
- `subscriptionEndDate` (Long): Timestamp in milliseconds
- `lastUpdated` (Long): Timestamp in milliseconds (auto-set to current time if not provided)

### Response Format

**Success Response (200 OK)**:
```json
{
  "status": "SUCCESS",
  "registrationId": 12345,
  "registrationCreated": true,
  "registrationUpdated": false,
  "usersProcessed": 1,
  "usersCreated": 1,
  "usersUpdated": 0,
  "message": "Registration created and processed"
}
```

**Error Response (400 Bad Request)**:
```json
{
  "status": "FAILED",
  "error": "registrationId is required"
}
```

**Error Response (401 Unauthorized)**:
```json
{
  "status": "FAILED",
  "error": "Invalid or missing API key",
  "message": "Please provide a valid API key in the X-CRM-Webhook-Key header"
}
```

**Error Response (500 Internal Server Error)**:
```json
{
  "status": "FAILED",
  "error": "Error message details"
}
```

### Example cURL Request

**Basic Request with API Key**:
```bash
curl --location --request POST 'http://103.99.202.227:8000/api/crm_integration/webhook/register' \
--header 'Content-Type: application/json' \
--header 'X-CRM-Webhook-Key: your-api-key-here' \
--data-raw '{
  "registrationId": 12345,
  "email": "john.doe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "username": "johndoe",
  "role": "ATHLETE",
  "paymentStatus": "PAID",
  "lastUpdated": 1704067200000
}'
```

**Minimal Request (only required fields)**:
```bash
curl --location --request POST 'http://103.99.202.227:8000/api/crm_integration/webhook/register' \
--header 'Content-Type: application/json' \
--header 'X-CRM-Webhook-Key: your-api-key-here' \
--data-raw '{
  "registrationId": 12345,
  "email": "john.doe@example.com"
}'
```

## Webhook Behavior

1. **Registration Storage**: The registration is stored in the `crm_registration` table
2. **User Processing**: If `paymentStatus` is "PAID" or "PARTIAL":
   - Creates a new user in the database (if email doesn't exist)
   - Updates existing user properties (if email exists)
   - Generates a 6-digit registration code
   - Sends registration email with the code
3. **Idempotency**: The endpoint is idempotent - calling it multiple times with the same `registrationId` will update the existing registration

## Payment Status Handling

- Only registrations with `paymentStatus` of "PAID" or "PARTIAL" will trigger user creation/update
- Other payment statuses will be stored but won't create users
- Users already registered with Keycloak will have their payment status updated but won't receive new registration emails

## Implementation Files

- **Webhook Endpoint**: `pallbearer-api/src/main/java/com/lektralabs/thrones/pallbearer/api/resource/CrmIntegrationResource.java`
- **Webhook Handler**: `pallbearer-api/src/main/java/com/lektralabs/thrones/crm/CrmIntegration.java` (method: `handleWebhookRegistration`)
- **Security Service**: `pallbearer-api/src/main/java/com/lektralabs/thrones/crm/WebhookSecurityService.java`
- **CRON Scheduler**: `pallbearer-api/src/main/java/com/lektralabs/thrones/crm/CrmSyncScheduler.java`
- **Configuration**: `pallbearer-api/src/main/resources/application.properties`

## Security

### API Key Authentication

The webhook endpoint requires API key authentication for security. All requests must include a valid API key in the `X-CRM-Webhook-Key` header.

**Configuration**:
- API key is configured in `application.properties` via `crm.webhook.api.key`
- Can be set via environment variable: `CRM_WEBHOOK_API_KEY`
- Security can be disabled for development by setting `crm.webhook.security.enabled=false`

**Security Features**:
- ✅ API key validation (required)
- ✅ Constant-time comparison to prevent timing attacks
- ✅ Request logging for audit purposes
- ✅ HMAC signature support (optional, for additional security)

### Getting Your API Key

Contact the system administrator to obtain your webhook API key. The API key should be:
- Kept secret and secure
- Not shared or committed to version control
- Rotated periodically for security

### HMAC Signature Verification (Optional)

For additional security, you can optionally include an HMAC signature in the `X-CRM-Webhook-Signature` header. The signature is calculated using HMAC-SHA256 with the webhook secret.

**Note**: Full HMAC signature verification requires access to the raw request body, which may require additional implementation depending on your CRM system's capabilities.

### Security Best Practices

1. **Always use HTTPS** in production (not HTTP)
2. **Keep your API key secure** - don't expose it in logs or error messages
3. **Rotate API keys periodically** - change them every 90 days or if compromised
4. **Monitor webhook requests** - check logs for unauthorized access attempts
5. **Use strong API keys** - generate random, long strings (at least 32 characters)
6. **Validate webhook payloads** - ensure data integrity before processing

### Disabling Security (Development Only)

For local development/testing, security can be temporarily disabled:

```properties
crm.webhook.security.enabled=false
```

**⚠️ WARNING**: Never disable security in production environments!

## Testing

To test the webhook endpoint:

1. Use the provided cURL command above
2. Check the application logs for processing details
3. Verify the user was created/updated in the database
4. Check that the registration email was sent (if it's a new user)

## Support

For issues or questions, check the application logs or contact the development team.
