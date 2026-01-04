# QR Setup Endpoints Implementation Plan

## Overview

Implementing 3 endpoints for device QR setup flow:
1. `GET /qr/setup` - Display QR code for device authorization
2. `GET /qr/setup/check/{approval_id}` - Long polling endpoint
3. `POST /authorize/qr` - Authorize device (authenticated users only)

## Requirements

### `GET /qr/setup`
- Get device from cookie
- Device role should be SETUP
- Generate random string as approval_id
- Store approval_id in Redis with device mapping
- Return view with QR code containing approval_id
- Reference design: `../htmlmockup/mockups/qr.html`

### `GET /qr/setup/check/{approval_id}`
- Long polling endpoint (30 second timeout)
- Check if approval_id is approved
- On approval: set device cookie with new role, send HTMX redirect
  - QUEUE_TAKING_QR_TARGET -> redirect to `/qr/queue-taking`
  - QUEUE_DISPLAY -> redirect to `/qr/queue-display`
- On timeout: return 204 No Content (client retries)

### `POST /authorize/qr`
- Requires authentication
- Parameters:
  - `approval_id`: approval_id to be approved
  - `new_role`: QUEUE_TAKING_QR_TARGET or QUEUE_DISPLAY
- Approve the device and end the long polling

## Technical Decisions

| Decision | Choice | Reason |
|----------|--------|--------|
| Approval storage | Redis | Persistent, already configured in project |
| QR code generation | Server-side SVG | Using `qrcode-kotlin` library |
| QR code content | approval_id only | Simple string, admin scans with phone |
| Long polling timeout | 30 seconds | Balance between responsiveness and server load |
| Redirect mechanism | HTMX `HX-Redirect` header | Project uses HTMX |

## Files to Create/Modify

| File | Action | Description |
|------|--------|-------------|
| `build.gradle.kts` | Modify | Add `io.github.g0dkar:qrcode-kotlin:4.2.0` |
| `src/main/kotlin/app/qur/service/ApprovalService.kt` | Create | Redis-backed approval management |
| `src/main/kotlin/app/qur/web/QrController.kt` | Modify | Add `/qr/setup` and `/qr/setup/check/{approval_id}` |
| `src/main/kotlin/app/qur/web/AuthorizeController.kt` | Create | `POST /authorize/qr` endpoint |
| `src/main/resources/templates/qr-setup.mustache` | Create | QR code display template |
| `src/main/kotlin/app/qur/SecurityConfig.kt` | Modify | Add `/qr/setup/**` to permitAll |
| `src/test/kotlin/app/qur/service/ApprovalServiceTest.kt` | Create | Unit tests |
| `src/test/kotlin/app/qur/web/AuthorizeControllerTest.kt` | Create | Integration tests |
| `src/test/kotlin/app/qur/web/QrControllerTest.kt` | Modify | Add tests for new endpoints |

## Implementation Details

### 1. `build.gradle.kts`

Add dependency:
```kotlin
implementation("io.github.g0dkar:qrcode-kotlin:4.2.0")
```

### 2. `ApprovalService.kt`

```kotlin
@Service
class ApprovalService(
    private val redisTemplate: ReactiveStringRedisTemplate
) {
    companion object {
        private const val KEY_PREFIX = "approval:"
        private val EXPIRATION = Duration.ofMinutes(5)
    }

    // Creates approval entry: approval:{id}:device -> deviceId
    fun createApproval(deviceId: String): Mono<String>

    // Checks if role has been set: approval:{id}:role
    // Returns null if pending, ApprovalResult if approved
    fun checkApproval(approvalId: String): Mono<ApprovalResult?>

    // Sets role: approval:{id}:role -> newRole.name
    // Returns false if approval_id doesn't exist
    fun approve(approvalId: String, newRole: DeviceRole): Mono<Boolean>

    // Cleanup keys after processing
    fun cleanup(approvalId: String): Mono<Boolean>
}

data class ApprovalResult(val deviceId: String, val newRole: DeviceRole)
```

**Redis Key Structure:**
- `approval:{approval_id}:device` -> deviceId (TTL: 5 min)
- `approval:{approval_id}:role` -> role name (set when approved)

### 3. `QrController.kt` - New Endpoints

**`GET /qr/setup`**
```kotlin
@GetMapping("/qr/setup")
fun qrSetup(exchange: ServerWebExchange, model: Model): Mono<String> {
    // 1. Get device from cookie, validate role == SETUP
    // 2. Generate UUID as approval_id
    // 3. Store in Redis via ApprovalService.createApproval()
    // 4. Generate QR code SVG with approval_id content
    // 5. Add to model: qrCodeSvg, approvalId
    // 6. Return "qr-setup"
}
```

**`GET /qr/setup/check/{approval_id}`**
```kotlin
@GetMapping("/qr/setup/check/{approval_id}")
fun checkApproval(
    @PathVariable approvalId: String,
    exchange: ServerWebExchange
): Mono<ResponseEntity<Void>> {
    // Poll every 500ms for 30 seconds
    // On approval:
    //   - Update device cookie with new role
    //   - Return HX-Redirect header based on role
    //   - Cleanup Redis entry
    // On timeout: Return 204 No Content
    // On not found: Return 404
}
```

### 4. `AuthorizeController.kt`

```kotlin
@RestController
class AuthorizeController(
    private val approvalService: ApprovalService
) {
    @PostMapping("/authorize/qr")
    fun authorizeQr(
        @AuthenticationPrincipal principal: JwtUserPrincipal,
        @RequestParam("approval_id") approvalId: String,
        @RequestParam("new_role") newRole: DeviceRole
    ): Mono<ResponseEntity<Map<String, String>>> {
        // Validate newRole is QUEUE_TAKING_QR_TARGET or QUEUE_DISPLAY
        // Call approvalService.approve()
        // Return {"status": "success"} or {"status": "error", "message": "..."}
    }
}
```

### 5. `qr-setup.mustache`

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Device Setup</title>
    <link rel="stylesheet" href="/css/tailwind.css">
    <script src="https://unpkg.com/htmx.org@2.0.4"></script>
</head>
<body>
<div class="flex flex-col justify-center items-center min-h-screen bg-gray-50">
    <div class="p-4 bg-white rounded-2xl border border-gray-200 shadow-xl min-w-[400px]">
        <h1 class="mb-2 text-2xl font-bold text-center text-gray-800">Device Setup</h1>
        <div id="qr-container" 
             class="flex justify-center items-center bg-gray-50 rounded-lg border border-gray-100"
             hx-get="/qr/setup/check/{{approvalId}}"
             hx-trigger="load delay:500ms"
             hx-swap="none">
            {{{qrCodeSvg}}}
        </div>
        <p class="mt-2 text-sm text-center text-gray-500">Scan this code with admin user to set device up</p>
    </div>
</div>
</body>
</html>
```

### 6. `SecurityConfig.kt`

Add to permitAll paths:
```kotlin
.pathMatchers(
    "/", "/login", "/logins", 
    "/qr", "/qr/setup", "/qr/setup/check/**",
    "/error", "/css/**", "/js/**", "/images/**"
).permitAll()
```

## Flow Diagram

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│  Device Browser │         │     Server      │         │   Admin Phone   │
└────────┬────────┘         └────────┬────────┘         └────────┬────────┘
         │                           │                           │
         │  GET /qr/setup            │                           │
         │──────────────────────────>│                           │
         │                           │  Create approval in Redis │
         │                           │  Generate QR SVG          │
         │<──────────────────────────│                           │
         │  HTML with QR code        │                           │
         │                           │                           │
         │  GET /qr/setup/check/{id} │                           │
         │──────────────────────────>│  (long poll 30s)          │
         │          ...              │                           │
         │                           │   Scan QR, get approval_id│
         │                           │<──────────────────────────│
         │                           │  POST /authorize/qr       │
         │                           │  (approval_id, new_role)  │
         │                           │──────────────────────────>│
         │                           │  {"status": "success"}    │
         │                           │                           │
         │  HX-Redirect: /qr/...     │                           │
         │<──────────────────────────│                           │
         │  (browser redirects)      │                           │
```

## Test Plan

### ApprovalServiceTest.kt
- Test createApproval stores device ID in Redis
- Test checkApproval returns null when pending
- Test checkApproval returns ApprovalResult when approved
- Test approve sets role and returns true
- Test approve returns false for non-existent approval_id
- Test cleanup removes keys

### QrControllerTest.kt (additions)
- Test GET /qr/setup returns QR code page for SETUP device
- Test GET /qr/setup returns error for non-SETUP device
- Test GET /qr/setup returns error for missing device cookie
- Test GET /qr/setup/check returns 204 on timeout
- Test GET /qr/setup/check returns HX-Redirect on approval
- Test GET /qr/setup/check returns 404 for invalid approval_id

### AuthorizeControllerTest.kt
- Test POST /authorize/qr succeeds with valid approval_id and role
- Test POST /authorize/qr fails with invalid approval_id
- Test POST /authorize/qr fails with SETUP role
- Test POST /authorize/qr requires authentication
