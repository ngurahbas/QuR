#!/bin/bash

# Keycloak setup script
KEYCLOAK_URL="${KEYCLOAK_URL:-http://keycloak:8080}"
REALM="qur"
USERNAME="user1"
PASSWORD="password1"
CLIENT_ID="qur-client"
CLIENT_SECRET="tItZt1hOUxxNFFaeuvL35r0lQZva3et6"

# Wait for Keycloak
until curl -s "${KEYCLOAK_URL}/realms/master/.well-known/openid-configuration" > /dev/null; do sleep 2; done

# Get admin token
TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')

# Create realm if not exists
if [ "$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer ${TOKEN}" "${KEYCLOAK_URL}/admin/realms/${REALM}")" == "404" ]; then
  curl -s -X POST "${KEYCLOAK_URL}/admin/realms" -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" -d "{\"realm\": \"${REALM}\", \"enabled\": true}"
fi

# Create client if not exists
if [ "$(curl -s -H "Authorization: Bearer ${TOKEN}" "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}")" == "[]" ]; then
  curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
    -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
    -d "{\"clientId\": \"${CLIENT_ID}\", \"enabled\": true, \"publicClient\": false, \"secret\": \"${CLIENT_SECRET}\", \"standardFlowEnabled\": true, \"directAccessGrantsEnabled\": true, \"redirectUris\": [\"http://localhost:8080/*\"], \"webOrigins\": [\"http://localhost:8080\"]}"
fi

# Create user if not exists
if [ "$(curl -s -H "Authorization: Bearer ${TOKEN}" "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=${USERNAME}")" == "[]" ]; then
  curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users" \
    -H "Authorization: Bearer ${TOKEN}" -H "Content-Type: application/json" \
    -d "{\"username\": \"${USERNAME}\", \"enabled\": true, \"email\": \"${USERNAME}@example.com\", \"emailVerified\": true, \"credentials\": [{\"type\": \"password\", \"value\": \"${PASSWORD}\", \"temporary\": false}]}"
fi

echo "Keycloak setup complete - user: ${USERNAME}/${PASSWORD}"
