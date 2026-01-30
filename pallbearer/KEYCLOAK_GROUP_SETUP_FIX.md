# Keycloak Group Setup Fix Guide

## Problem Summary

Your application is failing with:
```
ERROR: Failed to associate group with user. Status: 404, Response: {"error":"Group not found"}
```

**Root Causes:**
1. **Missing Groups**: Keycloak realm only has ADMIN and USER groups, but the code expects ATHLETE, FAN, and COACH groups
2. **Group ID Mismatch**: The code uses hardcoded UUIDs that don't match the actual group IDs in Keycloak
3. **URL Configuration**: Application is using `http://localhost:8080` instead of `http://10.10.0.215:8080`

---

## Solution Steps

### Step 1: Fix Keycloak URL Configuration

Set the environment variable so your application connects to the correct Keycloak instance:

```bash
export KEYCLOAK_URL=http://10.10.0.215:8080
```

Or add to your `~/.bashrc`:
```bash
echo 'export KEYCLOAK_URL=http://10.10.0.215:8080' >> ~/.bashrc
source ~/.bashrc
```

**Restart your application** after setting this variable.

---

### Step 2: Access Keycloak Admin Console

1. Open browser: `http://10.10.0.215:8080/admin`
2. Login:
   - Username: `admin`
   - Password: `91GYV9rseDLnqTwz`
3. **IMPORTANT**: Select `thrones_realm` from the realm dropdown (top-left corner)

---

### Step 3: Check Existing Groups

1. In the left sidebar, click **"Groups"**
2. You should see existing groups listed
3. Note the **ID** of each group (click on a group to see its details)

**Expected Groups:**
- ADMIN (should exist)
- USER (should exist)
- ATHLETE (missing - needs to be created)
- FAN (missing - needs to be created)
- COACH (missing - needs to be created)

---

### Step 4: Create Missing Groups

For each missing group (ATHLETE, FAN, COACH):

1. Click **"Create group"** button (top right)
2. Enter the group name exactly as shown:
   - `ATHLETE`
   - `FAN`
   - `COACH`
3. Click **"Save"**
4. **IMPORTANT**: After creating each group, click on it to view details
5. Copy the **Group ID** (UUID) shown in the URL or group details
6. Note down the ID for each group

---

### Step 5: Assign Roles to Groups

For each group (ADMIN, USER, ATHLETE, FAN, COACH):

1. Click on the group name
2. Go to the **"Role mappings"** tab
3. Click **"Assign role"**
4. Filter by **"Filter by clients"** → select **"Filter by realm roles"**
5. Find and assign the matching role:
   - ADMIN group → assign ADMIN role
   - USER group → assign USER role
   - ATHLETE group → assign ATHLETE role
   - FAN group → assign FAN role
   - COACH group → assign COACH role
6. Click **"Assign"**

**Note**: If roles don't exist, create them first:
- Go to **"Realm roles"** → **"Create role"**
- Create: ADMIN, USER, ATHLETE, FAN, COACH

---

### Step 6: Update CoreConstants.java with Actual Group IDs

After creating groups and noting their actual UUIDs, update the code:

**File**: `pallbearer-core/src/main/java/com/lektralabs/thrones/pallbearer/common/CoreConstants.java`

Replace the group ID constants with the actual UUIDs from Keycloak:

```java
UUID ADMIN_GROUP_ID = UUID.fromString("YOUR_ACTUAL_ADMIN_GROUP_ID");
UUID USER_GROUP_ID = UUID.fromString("YOUR_ACTUAL_USER_GROUP_ID");
UUID ATHLETE_GROUP_ID = UUID.fromString("YOUR_ACTUAL_ATHLETE_GROUP_ID");
UUID FAN_GROUP_ID = UUID.fromString("YOUR_ACTUAL_FAN_GROUP_ID");
UUID COACH_GROUP_ID = UUID.fromString("YOUR_ACTUAL_COACH_GROUP_ID");
```

**Alternative Approach**: Modify the code to look up groups by name instead of UUID (more flexible but requires code changes).

---

### Step 7: Verify Groups Exist via API

You can verify groups using curl:

```bash
# Get admin token
TOKEN=$(curl -s -X POST "http://10.10.0.215:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=91GYV9rseDLnqTwz" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

# List all groups
curl -s -X GET "http://10.10.0.215:8080/admin/realms/thrones_realm/groups" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" | python3 -m json.tool
```

---

## Quick Reference: Expected Group IDs (from CoreConstants.java)

The code currently expects these UUIDs (but they may not match your Keycloak):

- **ADMIN**: `7f85ddfe-6566-42e0-aa1a-6b3f15f12050`
- **USER**: `64cb29bd-44a4-4b2a-9b92-d2c63e446be2`
- **ATHLETE**: `dc609b09-5ac5-4e6b-b66c-d748890a85cb`
- **FAN**: `c5a52846-6408-4396-9f2e-22b14a32cea3`
- **COACH**: `e1d9f28b-37e8-4476-82e6-c88556968a11`

**Important**: Keycloak generates its own UUIDs when you create groups. You have two options:
1. Update `CoreConstants.java` with the actual UUIDs from Keycloak (recommended)
2. Use Keycloak's import feature to create groups with specific UUIDs (advanced)

---

## Testing

After completing the setup:

1. Restart your application with `KEYCLOAK_URL=http://10.10.0.215:8080`
2. Try registering/activating a user with role ATHLETE
3. Check the logs - the group association should succeed

---

## Troubleshooting

### If groups still not found:
- Verify you're in the correct realm (`thrones_realm`)
- Check group names match exactly (case-sensitive)
- Verify group IDs in CoreConstants match Keycloak group IDs

### If URL still wrong:
- Check environment variable: `echo $KEYCLOAK_URL`
- Restart your application after setting the variable
- Check application logs for the URL being used

---

## Alternative: Use Group Name Lookup (Future Improvement)

Instead of hardcoded UUIDs, the code could be modified to:
1. List all groups
2. Find group by name
3. Use the found group's ID

This would make the system more flexible but requires code changes to `KeycloakProvider.java`.
