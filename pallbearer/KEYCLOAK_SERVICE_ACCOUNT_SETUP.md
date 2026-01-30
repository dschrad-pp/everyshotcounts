# Keycloak Service Account Setup Guide

This guide will help you configure the Keycloak service account permissions required for user registration and management.

## Prerequisites

- Access to Keycloak Admin Console
- Admin credentials for Keycloak
- The realm name: `thrones_realm`
- The client ID: `thrones_client`

---

## Step-by-Step Instructions

### Step 1: Access Keycloak Admin Console

1. Open your browser and navigate to the Keycloak Admin Console
   - URL: `http://localhost:8080` (or your Keycloak server URL)
   - Or: `http://10.10.0.215:8080` (based on your configuration)

2. Log in with your Keycloak admin credentials
   - Username: `admin`
   - Password: `91GYV9rseDLnqTwz` (or your admin password)

---

### Step 2: Select the Correct Realm

1. **IMPORTANT**: Look at the top-left corner of the Keycloak Admin Console
2. You should see a dropdown that says **"Master"** or shows the current realm name
3. **Click on the dropdown** and select **`thrones_realm`**
   - This is crucial! The service account roles must be configured in the correct realm
   - If you don't see `thrones_realm`, it may need to be created first

---

### Step 3: Navigate to Clients

1. In the left sidebar, click on **"Clients"**
   - This will show a list of all clients in the `thrones_realm`

2. Find and click on **`thrones_client`** in the list
   - If you don't see it, you may need to create it first

---

### Step 4: Enable Service Account

1. Once you're on the `thrones_client` page, you'll see several tabs at the top:
   - Settings
   - Credentials
   - Roles
   - **Service account roles** ← This is what we need
   - etc.

2. First, go to the **"Settings"** tab

3. Scroll down to find the **"Service accounts enabled"** toggle/checkbox

4. **Enable "Service accounts enabled"**
   - This allows the client to act as a service account
   - Without this, the service account roles tab won't work properly

5. Click **"Save"** at the bottom of the page

---

### Step 5: Navigate to Service Account Roles

1. After saving, click on the **"Service account roles"** tab
   - This tab shows what roles the service account has

2. You should see two sections:
   - **"Assigned roles"** (on the right) - Currently assigned roles
   - **"Available roles"** (on the left) - Roles that can be assigned

---

### Step 6: Filter by Realm-Management Client

1. Look for a dropdown or filter box that says **"Filter by clients"** or **"Client roles"**
   - This is usually above the "Available roles" section

2. **Click on the dropdown** and start typing: `realm-management`
   - As you type, it should filter the list

3. **Select `realm-management`** from the dropdown
   - This filters to show only roles from the `realm-management` client
   - The `realm-management` client is a special Keycloak client that provides administrative roles

---

### Step 7: Find and Assign Required Roles

1. After filtering by `realm-management`, you should see roles in the **"Available roles"** section:
   - `manage-users` ← **REQUIRED** - Allows creating, updating, and deleting users
   - `view-users` ← **RECOMMENDED** - Allows viewing user information
   - `query-users` ← Optional - Allows querying users
   - `manage-clients` ← Optional - Allows managing clients
   - And other roles...

2. **Select `manage-users`**:
   - Click on `manage-users` in the "Available roles" list
   - It should highlight/select

3. **Click the "Add selected" button** (usually a `>` or `→` button)
   - This moves `manage-users` from "Available roles" to "Assigned roles"

4. **Select `view-users`**:
   - Click on `view-users` in the "Available roles" list
   - Click "Add selected" again
   - This adds `view-users` to the assigned roles

---

### Step 8: Verify Assigned Roles

1. Check the **"Assigned roles"** section (on the right side)
2. You should now see:
   - `manage-users` under `realm-management`
   - `view-users` under `realm-management`

3. If you see both roles listed, you're done! ✅

---

## Troubleshooting

### Issue: "realm-management" doesn't appear in the filter dropdown

**Solution:**
1. Go back to **Clients** (left sidebar)
2. Check if `realm-management` exists in the clients list
3. If it doesn't exist:
   - The `realm-management` client should exist by default in Keycloak
   - If it's missing, you may need to recreate the realm or check Keycloak configuration
   - Try logging out and back in, or restarting Keycloak

### Issue: "Service account roles" tab is missing or grayed out

**Solution:**
1. Go back to the **Settings** tab
2. Make sure **"Service accounts enabled"** is checked/enabled
3. Click **Save**
4. Refresh the page
5. The "Service account roles" tab should now be available

### Issue: Roles are assigned but still getting 403 errors

**Solution:**
1. Double-check that you're in the correct realm (`thrones_realm`)
2. Verify the roles are assigned to `thrones_client` (not another client)
3. Try logging out and back into Keycloak Admin Console
4. Restart the application (to refresh the service account token)
5. Check Keycloak server logs for more detailed error messages

### Issue: Can't find `thrones_client` or `thrones_realm`

**Solution:**
1. If `thrones_realm` doesn't exist:
   - You may need to create it first
   - Or check if it's named differently
   - Check your application configuration files for the correct realm name

2. If `thrones_client` doesn't exist:
   - You may need to create it first
   - Go to Clients → Create client
   - Set Client ID to `thrones_client`
   - Enable "Service accounts enabled" in Settings
   - Then follow the steps above

---

## Verification Steps

After completing the setup, verify it's working:

1. **Test the service account token**:
   ```bash
   curl --location 'http://localhost:8080/realms/thrones_realm/protocol/openid-connect/token' \
   --header 'Content-Type: application/x-www-form-urlencoded' \
   --data-urlencode 'grant_type=client_credentials' \
   --data-urlencode 'client_id=thrones_client' \
   --data-urlencode 'client_secret=YOUR_CLIENT_SECRET'
   ```

2. **Try user activation**:
   - Use the activation API endpoint
   - If it works, you should see a 201 Created response instead of 403 Forbidden

3. **Check application logs**:
   - Look for "Successfully obtained admin access token"
   - Look for successful user registration messages
   - No more 403 errors

---

## Quick Reference Checklist

- [ ] Logged into Keycloak Admin Console
- [ ] Selected `thrones_realm` from realm dropdown (top-left)
- [ ] Navigated to Clients → `thrones_client`
- [ ] Enabled "Service accounts enabled" in Settings tab
- [ ] Clicked on "Service account roles" tab
- [ ] Filtered by `realm-management` client
- [ ] Added `manage-users` role to Assigned roles
- [ ] Added `view-users` role to Assigned roles
- [ ] Verified both roles appear in Assigned roles section
- [ ] Tested user activation API

---

## Additional Notes

### Why These Permissions Are Needed

- **`manage-users`**: Required to create, update, and delete users via the Keycloak Admin API
- **`view-users`**: Allows the service account to query and view user information
- **`realm-management`**: This is a special Keycloak client that provides administrative roles for realm management

### Security Considerations

- The service account has administrative privileges
- Keep the client secret secure
- Only grant the minimum required permissions
- Regularly audit service account permissions

### Alternative: Realm-Level Roles

If you can't find `realm-management` in the client filter, you can also assign realm-level roles:

1. Go to **Roles** (left sidebar) → **Realm roles**
2. Look for `manage-users` and `view-users` as realm roles
3. Go back to Clients → `thrones_client` → Service account roles
4. Click **"Assign role"** button (if available)
5. Select the realm roles instead

However, **client roles from `realm-management` are preferred** as they're more specific and secure.

---

## Need Help?

If you're still experiencing issues:

1. Check the application logs for detailed error messages
2. Check Keycloak server logs (usually in Keycloak logs directory)
3. Verify your Keycloak version compatibility
4. Ensure Keycloak is running and accessible
5. Verify network connectivity between your application and Keycloak

---

## Summary

The key steps are:
1. **Select the correct realm** (`thrones_realm`)
2. **Enable service accounts** for `thrones_client`
3. **Assign `manage-users` role** from `realm-management` client
4. **Verify** the roles are assigned

Once these steps are complete, your application should be able to create users in Keycloak without 403 errors.
