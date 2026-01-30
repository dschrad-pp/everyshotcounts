#!/bin/bash

# Script to create all required Keycloak roles and groups
# Usage: ./create_keycloak_roles_groups.sh

KEYCLOAK_URL="${KEYCLOAK_URL:-http://10.10.0.215:8080}"
REALM="thrones_realm"
ADMIN_USER="admin"
ADMIN_PASS="91GYV9rseDLnqTwz"

echo "=========================================="
echo "Keycloak Roles and Groups Setup Script"
echo "=========================================="
echo "Keycloak URL: $KEYCLOAK_URL"
echo "Realm: $REALM"
echo ""

# Step 1: Get admin token
echo "Step 1: Getting admin token..."
TOKEN_RESPONSE=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=$ADMIN_USER" \
  -d "password=$ADMIN_PASS" \
  -d "grant_type=password" \
  -d "client_id=admin-cli")

# Extract token using Python (more reliable than grep)
TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin).get('access_token', ''))" 2>/dev/null)

if [ -z "$TOKEN" ] || [ "$TOKEN" == "None" ]; then
  echo "ERROR: Failed to get admin token"
  echo "Response: $TOKEN_RESPONSE"
  exit 1
fi

echo "✓ Admin token obtained"
echo ""

# Step 2: Check existing roles
echo "Step 2: Checking existing roles..."
EXISTING_ROLES=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/roles" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "Existing roles:"
echo "$EXISTING_ROLES" | python3 -c "import sys, json; [print(f\"  - {r['name']}\") for r in json.load(sys.stdin)]" 2>/dev/null || echo "  (Unable to parse)"
echo ""

# Step 3: Create missing roles
echo "Step 3: Creating missing roles..."
REQUIRED_ROLES=("ADMIN" "USER" "ATHLETE" "COACH" "FAN")

for ROLE in "${REQUIRED_ROLES[@]}"; do
  echo -n "Creating role '$ROLE'... "
  
  # Check if role exists
  ROLE_EXISTS=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/roles/$ROLE" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -w "%{http_code}" -o /dev/null)
  
  if [ "$ROLE_EXISTS" == "200" ]; then
    echo "✓ Already exists"
  else
    # Create role
    CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/roles" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"$ROLE\"}")
    
    HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" == "201" ] || [ "$HTTP_CODE" == "409" ]; then
      echo "✓ Created (or already exists)"
    else
      echo "✗ Failed (HTTP $HTTP_CODE)"
      echo "  Response: $(echo "$CREATE_RESPONSE" | head -n-1)"
    fi
  fi
done

echo ""

# Step 4: Check existing groups
echo "Step 4: Checking existing groups..."
EXISTING_GROUPS=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/groups" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "Existing groups:"
echo "$EXISTING_GROUPS" | python3 -c "import sys, json; [print(f\"  - {g['name']} (ID: {g['id']})\") for g in json.load(sys.stdin)]" 2>/dev/null || echo "  (Unable to parse)"
echo ""

# Step 5: Create missing groups
echo "Step 5: Creating missing groups..."
REQUIRED_GROUPS=("ADMIN" "USER" "ATHLETE" "COACH" "FAN")

for GROUP in "${REQUIRED_GROUPS[@]}"; do
  echo -n "Creating group '$GROUP'... "
  
  # Check if group exists
  GROUP_EXISTS=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/groups?search=$GROUP" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json")
  
  GROUP_FOUND=$(echo "$GROUP_EXISTS" | python3 -c "import sys, json; groups = json.load(sys.stdin); print('yes' if any(g['name'] == '$GROUP' for g in groups) else 'no')" 2>/dev/null)
  
  if [ "$GROUP_FOUND" == "yes" ]; then
    echo "✓ Already exists"
  else
    # Create group
    CREATE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/groups" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"$GROUP\"}")
    
    HTTP_CODE=$(echo "$CREATE_RESPONSE" | tail -n1)
    
    if [ "$HTTP_CODE" == "201" ] || [ "$HTTP_CODE" == "409" ]; then
      echo "✓ Created"
    else
      echo "✗ Failed (HTTP $HTTP_CODE)"
      echo "  Response: $(echo "$CREATE_RESPONSE" | head -n-1)"
    fi
  fi
done

echo ""

# Step 6: Get all groups with IDs
echo "Step 6: Fetching group IDs..."
ALL_GROUPS=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/groups" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json")

echo "Group IDs (update CoreConstants.java with these):"
echo "$ALL_GROUPS" | python3 -c "
import sys, json
groups = json.load(sys.stdin)
for group in groups:
    if group['name'] in ['ADMIN', 'USER', 'ATHLETE', 'COACH', 'FAN']:
        print(f\"  UUID {group['name']}_GROUP_ID = UUID.fromString(\\\"{group['id']}\\\");\")
" 2>/dev/null || echo "  (Unable to parse)"
echo ""

# Step 7: Assign roles to groups
echo "Step 7: Assigning roles to groups..."
for GROUP in "${REQUIRED_GROUPS[@]}"; do
  echo -n "Assigning $GROUP role to $GROUP group... "
  
  # Get group ID
  GROUP_ID=$(echo "$ALL_GROUPS" | python3 -c "
import sys, json
groups = json.load(sys.stdin)
for g in groups:
    if g['name'] == '$GROUP':
        print(g['id'])
        break
" 2>/dev/null)
  
  if [ -z "$GROUP_ID" ]; then
    echo "✗ Group not found"
    continue
  fi
  
  # Get role representation
  ROLE_REP=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/roles/$GROUP" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json")
  
  if [ -z "$ROLE_REP" ] || echo "$ROLE_REP" | grep -q "error"; then
    echo "✗ Role not found"
    continue
  fi
  
  # Assign role to group
  ASSIGN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$KEYCLOAK_URL/admin/realms/$REALM/groups/$GROUP_ID/role-mappings/realm" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "[$ROLE_REP]")
  
  HTTP_CODE=$(echo "$ASSIGN_RESPONSE" | tail -n1)
  
  if [ "$HTTP_CODE" == "204" ] || [ "$HTTP_CODE" == "200" ]; then
    echo "✓ Assigned"
  else
    # Check if already assigned
    EXISTING_ROLES=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM/groups/$GROUP_ID/role-mappings/realm" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json")
    
    ROLE_EXISTS=$(echo "$EXISTING_ROLES" | python3 -c "
import sys, json
roles = json.load(sys.stdin)
print('yes' if any(r['name'] == '$GROUP' for r in roles) else 'no')
" 2>/dev/null)
    
    if [ "$ROLE_EXISTS" == "yes" ]; then
      echo "✓ Already assigned"
    else
      echo "✗ Failed (HTTP $HTTP_CODE)"
      echo "  Response: $(echo "$ASSIGN_RESPONSE" | head -n-1)"
    fi
  fi
done

echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Copy the Group IDs shown above"
echo "2. Update CoreConstants.java with the actual Group IDs"
echo "3. Restart your application"
echo "4. Test user registration"
