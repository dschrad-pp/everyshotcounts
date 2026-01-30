# Quick Fix Instructions - ATHLETE Role Missing

## Current Problem
- Application is using `localhost:8080` instead of `10.10.0.215:8080`
- ATHLETE role doesn't exist in Keycloak
- Error: `Failed to fetch role [ATHLETE]`

---

## Step 1: Set Environment Variable (CRITICAL)

**In the terminal where you run your Quarkus application:**

```bash
export KEYCLOAK_URL=http://10.10.0.215:8080
```

**Then restart your Quarkus application completely** (stop and start again).

**To make it permanent:**
```bash
echo 'export KEYCLOAK_URL=http://10.10.0.215:8080' >> ~/.bashrc
source ~/.bashrc
```

---

## Step 2: Create ATHLETE Role in Keycloak

1. **Open Keycloak Admin Console:**
   - URL: `http://10.10.0.215:8080/admin`
   - Login with admin credentials

2. **Select the Correct Realm:**
   - Look at top-left corner
   - Click the realm dropdown
   - Select **`thrones_realm`** (NOT master)

3. **Create ATHLETE Role:**
   - In left sidebar, click **"Realm roles"**
   - Click **"Create role"** button (top right)
   - **Role name:** `ATHLETE` (exactly, case-sensitive)
   - Click **"Save"**

4. **Create Other Missing Roles (if needed):**
   - Repeat for: `ADMIN`, `USER`, `COACH`, `FAN`
   - Check which ones already exist first

---

## Step 3: Verify Role Was Created

After creating the role, you should be able to see it in the "Realm roles" list.

---

## Step 4: Test Again

After creating the ATHLETE role and setting KEYCLOAK_URL:

1. **Restart your Quarkus application** (with KEYCLOAK_URL set)
2. Try the user activation again
3. Check logs - should no longer see "Failed to fetch role [ATHLETE]"

---

## Verification Commands

To verify the role exists via API:

```bash
# Get admin token (adjust credentials if needed)
TOKEN=$(curl -s -X POST "http://10.10.0.215:8080/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin" \
  -d "password=YOUR_ADMIN_PASSWORD" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | python3 -c "import sys, json; print(json.load(sys.stdin)['access_token'])")

# List all roles
curl -s -X GET "http://10.10.0.215:8080/admin/realms/thrones_realm/roles" \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool | grep -E '"name"'
```

---

## Common Issues

### Still seeing "localhost:8080" in logs?
- The environment variable isn't being picked up
- **Solution:** Make sure you set `KEYCLOAK_URL` in the SAME terminal where you start Quarkus
- Or add it to your application startup command: `KEYCLOAK_URL=http://10.10.0.215:8080 ./mvnw quarkus:dev`

### Role still not found?
- Make sure you're in the correct realm (`thrones_realm`, not `master`)
- Check the role name matches exactly (case-sensitive: `ATHLETE` not `athlete`)
- Verify the role appears in the "Realm roles" list in Keycloak Admin Console

### Authentication failing?
- Check your admin password in KeycloakConstants.java (line 36)
- Or try the default Keycloak admin password

---

## Next Steps After ATHLETE Role is Created

Once the ATHLETE role exists, you may also need to:
1. Create ATHLETE group
2. Assign ATHLETE role to ATHLETE group
3. Update CoreConstants.java with actual group IDs

But first, let's get the role created and the URL fixed!
