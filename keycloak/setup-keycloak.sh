#!/bin/bash

# Keycloak setup script - Creates realm and development user via REST API
# Usage: ./keycloak/setup-keycloak.sh

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
REALM="qur"
USERNAME="user1"
PASSWORD="password1"

echo "Waiting for Keycloak to be ready..."
until curl -s "${KEYCLOAK_URL}/health/ready" > /dev/null 2>&1; do
  sleep 2
done
echo "Keycloak is ready!"

# Get admin access token
echo "Getting admin access token..."
TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "Failed to get admin token"
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

# Check if user exists
USER_EXISTS=$(curl -s \
  -H "Authorization: Bearer ${TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=${USERNAME}" | jq 'length')

if [ "$USER_EXISTS" == "0" ]; then
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
echo "  User: ${USERNAME}"
echo "  Password: ${PASSWORD}"
echo "  Keycloak URL: ${KEYCLOAK_URL}"
