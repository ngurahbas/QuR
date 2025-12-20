#!/bin/bash

# Keycloak setup script - Creates realm, client and development user via REST API
# Usage: ./keycloak/setup-keycloak.sh

# Use different URLs when running inside Docker vs from host
if [ -n "$DOCKER_CONTAINER" ] || [ -f /.dockerenv ]; then
    KEYCLOAK_URL="${KEYCLOAK_URL:-http://keycloak:8080}"
else
    KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
fi

ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM="qur"
USERNAME="user1"
PASSWORD="password1"
CLIENT_ID="qur-client"

echo "Using Keycloak URL: ${KEYCLOAK_URL}"
echo "Waiting for Keycloak to be ready..."
until curl -s "${KEYCLOAK_URL}/realms/master/.well-known/openid-configuration" > /dev/null 2>&1; do
  sleep 2
done
echo "Keycloak is ready!"

# Get admin access token
echo "Getting admin access token..."
TOKEN_RESPONSE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli")

# Extract access token using sed (no jq dependency)
TOKEN=$(echo "$TOKEN_RESPONSE" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

if [ -z "$TOKEN" ]; then
  echo "Failed to get admin token"
  echo "Response: $TOKEN_RESPONSE"
  exit 1
fi

echo "Admin token obtained successfully"

# Check if realm exists
REALM_EXISTS=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer ${TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}")

if [ "$REALM_EXISTS" == "404" ]; then
  # Create realm
  echo "Creating realm '${REALM}'..."
  curl -s -X POST "${KEYCLOAK_URL}/admin/realms" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{\"realm\": \"${REALM}\", \"enabled\": true}"
  echo "Realm '${REALM}' created"
else
  echo "Realm '${REALM}' already exists"
fi

# Check if client exists
CLIENT_EXISTS=$(curl -s \
  -H "Authorization: Bearer ${TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}")

if [ "$CLIENT_EXISTS" == "[]" ] || [ -z "$CLIENT_EXISTS" ]; then
  # Create client
  echo "Creating client '${CLIENT_ID}'..."
  curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"clientId\": \"${CLIENT_ID}\",
      \"name\": \"QuR Application\",
      \"description\": \"Client for QuR application\",
      \"enabled\": true,
      \"clientAuthenticatorType\": \"client-secret\",
      \"publicClient\": false,
      \"directAccessGrantsEnabled\": true,
      \"serviceAccountsEnabled\": true,
      \"standardFlowEnabled\": true,
      \"redirectUris\": [\"http://localhost:8080/*\"],
      \"webOrigins\": [\"http://localhost:8080\"]
    }"
  echo "Client '${CLIENT_ID}' created"
else
  echo "Client '${CLIENT_ID}' already exists"
fi

# Check if user exists
USER_RESPONSE=$(curl -s \
  -H "Authorization: Bearer ${TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=${USERNAME}")

# Check if response is empty array (user doesn't exist)
if [ "$USER_RESPONSE" == "[]" ] || [ -z "$USER_RESPONSE" ]; then
  # Create user
  echo "Creating user '${USERNAME}'..."
  curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"${USERNAME}\",
      \"enabled\": true,
      \"email\": \"${USERNAME}@example.com\",
      \"emailVerified\": true,
      \"credentials\": [{
        \"type\": \"password\",
        \"value\": \"${PASSWORD}\",
        \"temporary\": false
      }]
    }"
  echo "User '${USERNAME}' created with password '${PASSWORD}'"
else
  echo "User '${USERNAME}' already exists"
fi

echo "Keycloak setup complete!"
echo "  Realm: ${REALM}"
echo "  Client: ${CLIENT_ID}"
echo "  User: ${USERNAME}"
echo "  Password: ${PASSWORD}"
echo "  Keycloak URL: ${KEYCLOAK_URL}"
echo ""
echo "To test authentication, you can use:"
echo "  http://localhost:8081/realms/${REALM}/protocol/openid-connect/auth?client_id=${CLIENT_ID}&response_type=code&redirect_uri=http://localhost:8080"
