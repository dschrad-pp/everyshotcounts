# Keycloak Roles and Groups Setup - Complete Fix

## Current Issues

1. **Missing Roles**: ATHLETE, COACH, FAN roles don't exist in Keycloak
2. **Missing Groups**: ATHLETE, COACH, FAN groups don't exist in Keycloak  
3. **URL Configuration**: Application still using `localhost:8080` instead of `10.10.0.215:8080`
4. **User Registration Failing**: Cannot assign roles because they don't exist

---

## Step-by-Step Fix

### Step 1: Fix Keycloak URL (CRITICAL - Do This First!)

**The application is still connecting to `localhost:8080` instead of your Keycloak server.**

```bash
# Set environment variable
export KEYCLOAK_URL=http://10.10.0.215:8080

# Make it permanent
echo 'export KEYCLOAK_URL=http://10.10.0.215:8080' >> ~/.bashrc
source ~/.bashrc

# RESTART YOUR APPLICATION after setting this!
```

**Verify it's set:**
```bash
echo $KEYCLOAK_URL
# Should output: http://10.10.0.215:8080
```

---

### Step 2: Access Keycloak Admin Console

1. Open: `http://10.10.0.215:8080/admin`
2. Login:
   - Username: `admin`
   - Password: `91GYV9rseDLnqTwz`
3. **IMPORTANT**: Select `thrones_realm` from the realm dropdown (top-left)

---

### Step 3: Create Missing Realm Roles

You need to create ALL roles: **ADMIN**, **USER**, **ATHLETE**, **COACH**, **FAN**

For each role:

1. In left sidebar, click **"Realm roles"**
2. Click **"Create role"** button (top right)
3. Enter role name exactly:
   - `ADMIN`
   - `USER`
   - `ATHLETE`
   - `COACH`  
   - `FAN`
4. **Description** (optional): Same as name
5. Click **"Save"**

**Verify all roles exist:**
- ADMIN (create this)
- USER (create this)
- ATHLETE (create this)
- COACH (create this)
- FAN (create this)

---

### Step 4: Create Missing Groups

You need to create: **ATHLETE**, **COACH**, **FAN** groups

For each group:

1. In left sidebar, click **"Groups"**
2. Click **"Create group"** button (top right)
3. Enter group name exactly:
   - `ATHLETE`
   - `COACH`
   - `FAN`
4. Click **"Save"**
5. **IMPORTANT**: After creating, click on the group to view details
6. **Copy the Group ID (UUID)** - you'll need this later!

**Verify all groups exist:**
- ADMIN (create this)
- USER (create this)
- ATHLETE (create this)
- COACH (create this)
- FAN (create this)

---

### Step 5: Assign Roles to Groups

For each group (ADMIN, USER, ATHLETE, COACH, FAN):

1. Click on the group name
2. Go to **"Role mappings"** tab
3. Click **"Assign role"** button
4. In the modal:
   - Select **"Filter by realm roles"** (not client roles)
   - Find and select the matching role:
     - ADMIN group → assign ADMIN role
     - USER group → assign USER role
     - ATHLETE group → assign ATHLETE role
     - COACH group → assign COACH role
     - FAN group → assign FAN role
5. Click **"Assign"**

**Verify**: Each group should show its role in the "Assigned roles" list.

---

### Step 6: Update CoreConstants.java with Actual Group IDs

After creating groups, you need to update the code with the actual UUIDs from Keycloak.

**File**: `pallbearer-core/src/main/java/com/lektralabs/thrones/pallbearer/common/CoreConstants.java`

1. Get the actual Group IDs from Keycloak (from Step 4)
2. Update the constants:

```java
// Replace these with actual UUIDs from Keycloak
UUID ADMIN_GROUP_ID = UUID.fromString("YOUR_ACTUAL_ADMIN_GROUP_ID");
UUID USER_GROUP_ID = UUID.fromString("YOUR_ACTUAL_USER_GROUP_ID");
UUID ATHLETE_GROUP_ID = UUID.fromString("YOUR_ACTUAL_ATHLETE_GROUP_ID");
UUID FAN_GROUP_ID = UUID.fromString("YOUR_ACTUAL_FAN_GROUP_ID");
UUID COACH_GROUP_ID = UUID.fromString("YOUR_ACTUAL_COACH_GROUP_ID");
```

**How to get Group IDs:**
- Option 1: In Keycloak Admin Console, click on each group → the URL will show the ID
- Option 2: Use API (see below)

---

### Step 7: Verify Setup via API

You can verify roles and groups exist using curl:

```bash
# Get admin token
TOKEN=$(curl -s -X POST "http://10.10.0.215:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=91GYV9rseDLnqTwz" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

# List all realm roles
curl -s -X GET "http://10.10.0.215:8080/admin/realms/thrones_realm/roles" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | grep -E '"name"|"id"'

# List all groups
curl -s -X GET "http://10.10.0.215:8080/admin/realms/thrones_realm/groups" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

---

## Quick Checklist

- [ ] Set `KEYCLOAK_URL=http://10.10.0.215:8080` environment variable
- [ ] Restart application
- [ ] Create ADMIN role in Keycloak
- [ ] Create USER role in Keycloak
- [ ] Create ATHLETE role in Keycloak
- [ ] Create COACH role in Keycloak
- [ ] Create FAN role in Keycloak
- [ ] Create ADMIN group in Keycloak
- [ ] Create USER group in Keycloak
- [ ] Create ATHLETE group in Keycloak
- [ ] Create COACH group in Keycloak
- [ ] Create FAN group in Keycloak
- [ ] Assign ADMIN role to ADMIN group
- [ ] Assign USER role to USER group
- [ ] Assign ATHLETE role to ATHLETE group
- [ ] Assign COACH role to COACH group
- [ ] Assign FAN role to FAN group
- [ ] Get actual Group IDs from Keycloak
- [ ] Update CoreConstants.java with actual Group IDs
- [ ] Test user registration

## Automated Setup (Alternative)

You can use the provided script to automate role and group creation:

```bash
# Make sure KEYCLOAK_URL is set
export KEYCLOAK_URL=http://10.10.0.215:8080

# Run the script
./create_keycloak_roles_groups.sh
```

The script will:
1. Create all required roles (ADMIN, USER, ATHLETE, COACH, FAN)
2. Create all required groups
3. Assign roles to groups
4. Display the Group IDs you need to update in CoreConstants.java

---

## Expected Error After Fix

After creating roles, you should see the user registration progress further. However, you may still see:

```
"Account is not fully set up"
```

This means the user was created but:
- The role assignment might have failed
- The group association might have failed
- The user might need email verification disabled

**To fix "Account is not fully set up":**
1. Go to Keycloak Admin Console
2. Find the user (Users → Search)
3. Click on the user
4. Go to **"Credentials"** tab
5. Set a password and click **"Set password"**
6. Go to **"Details"** tab
7. Enable **"Email verified"** (toggle ON)
8. Enable **"User enabled"** (toggle ON)

---

## Troubleshooting

### Still seeing "localhost:8080" in logs?
- Check: `echo $KEYCLOAK_URL`
- Restart your application completely
- Check application.properties doesn't override it

### Role fetch still failing?
- Verify role name matches exactly (case-sensitive)
- Check you're in the correct realm (`thrones_realm`)
- Verify service account has permissions (see KEYCLOAK_SERVICE_ACCOUNT_SETUP.md)

### Group association failing?
- Verify group IDs in CoreConstants match Keycloak group IDs
- Check group exists in Keycloak
- Verify service account has `manage-users` permission

---

## Alternative: Use Keycloak Admin Client

If you prefer to create roles/groups programmatically, you can use the Keycloak Admin Client API. However, the manual method above is simpler and more reliable.
