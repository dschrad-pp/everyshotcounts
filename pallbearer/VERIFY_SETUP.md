# Verify Keycloak Setup - All Roles Created ✓

## ✅ Status Check

### Roles Created (Confirmed):
- ✅ ADMIN: `559e567f-4c88-4b00-a593-a3e64086f603`
- ✅ ATHLETE: `67452c9c-0454-44d5-b844-8d235005e99a`
- ✅ USER: `6bf20ac0-1cf3-4e84-83b8-de15860b16f8`
- ✅ COACH: `802a6427-0b26-4267-a187-bef31a36f2ab`
- ✅ FAN: `8721d44c-bdf9-404b-89d3-889c25b49316`

### CoreConstants.java Status:
- ✅ Role IDs are updated with correct values
- ✅ Group IDs are present (need to verify they match Keycloak)

---

## Next Steps

### 1. Verify Groups Exist in Keycloak

Go to Keycloak Admin Console:
- URL: `http://10.10.0.215:8080/admin`
- Select `thrones_realm`
- Go to **Groups**
- Verify these groups exist:
  - ADMIN
  - USER
  - ATHLETE
  - COACH
  - FAN

**If groups don't exist, create them:**
1. Click **"Create group"**
2. Enter group name (e.g., "ATHLETE")
3. Click **"Save"**
4. Click on the group to see its ID
5. Update CoreConstants.java with the actual group ID

### 2. Assign Roles to Groups

For each group:
1. Click on the group name
2. Go to **"Role mappings"** tab
3. Click **"Assign role"**
4. Filter by **"Filter by realm roles"**
5. Select the matching role:
   - ADMIN group → ADMIN role
   - USER group → USER role
   - ATHLETE group → ATHLETE role
   - COACH group → COACH role
   - FAN group → FAN role
6. Click **"Assign"**

### 3. Set KEYCLOAK_URL Environment Variable

**CRITICAL:** Your application is still using `localhost:8080`. You must set:

```bash
export KEYCLOAK_URL=http://10.10.0.215:8080
```

**Then restart your Quarkus application completely.**

To verify it's set:
```bash
echo $KEYCLOAK_URL
# Should output: http://10.10.0.215:8080
```

### 4. Verify Group IDs Match

Check if the group IDs in `CoreConstants.java` match the actual group IDs in Keycloak:

**Current Group IDs in CoreConstants.java:**
- ADMIN_GROUP_ID: `40e31ac9-90b0-4681-a8fc-c5f3b7163ee7`
- USER_GROUP_ID: `13d5ed42-de73-485d-b175-3de63de6e33f`
- ATHLETE_GROUP_ID: `94665bbc-5f81-40eb-8d3b-c4ae4bb37985`
- FAN_GROUP_ID: `e3e77f80-9d30-4c18-972b-00e97fbf288a`
- COACH_GROUP_ID: `db0e94d3-36ec-4f1c-b727-c1eae427e823`

**To get actual group IDs from Keycloak:**
1. Go to Keycloak Admin Console → Groups
2. Click on each group
3. The URL will show the group ID, or check the group details

**If they don't match, update CoreConstants.java with the correct IDs.**

---

## Quick Test

After completing the above:

1. **Set KEYCLOAK_URL:**
   ```bash
   export KEYCLOAK_URL=http://10.10.0.215:8080
   ```

2. **Restart your application**

3. **Try user activation again**

4. **Check logs** - should no longer see:
   - ❌ "Failed to fetch role [ATHLETE]"
   - ❌ "Group not found"
   - ❌ "localhost:8080" (should see "10.10.0.215:8080")

---

## Troubleshooting

### Still seeing "Failed to fetch role [ATHLETE]"?
- ✅ Roles exist (confirmed above)
- Check: Are you in the correct realm? (`thrones_realm`, not `master`)
- Check: Is KEYCLOAK_URL set correctly?
- Check: Did you restart the application after setting KEYCLOAK_URL?

### Still seeing "localhost:8080" in logs?
- The environment variable isn't being picked up
- **Solution:** Set `KEYCLOAK_URL` in the SAME terminal where you start Quarkus
- Or add to startup: `KEYCLOAK_URL=http://10.10.0.215:8080 ./mvnw quarkus:dev`

### Group association failing?
- Verify groups exist in Keycloak
- Verify group IDs in CoreConstants.java match Keycloak group IDs
- Verify roles are assigned to groups
