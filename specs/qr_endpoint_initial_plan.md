# QR Endpoint Initial Plan

## Overview

Create a new public endpoint `GET /qr` that manages device cookies for QR code functionality.

## Requirements

1. **Endpoint**: `GET /qr`
2. **Cookie name**: `device`
3. **Cookie expiration**: 24 hours
4. **Device role**: `SETUP`
5. **Response**: Empty 200 OK
6. **Authentication**: Public (no auth required)

## Logic Flow

```
GET /qr
    │
    ▼
┌─────────────────────────┐
│ Read "device" cookie    │
└───────────┬─────────────┘
            │
            ▼
    ┌───────────────┐
    │ Cookie exists?│
    └───────┬───────┘
            │
      ┌─────┴────────┐
      │              │
     YES             NO
      │              │
      ▼              ▼
┌───────────────┐ ┌────────────┐
│ Deserialize   │ │ Create new │
│ & decrypt     │ │ device     │
└───────┬───────┘ └─────┬──────┘
        │               │
        ▼               ▼
  ┌───────────┐   ┌────────────┐
  │ Expired?  │   │ Set cookie │
  └─────┬─────┘   └─────┬──────┘
        │               │
   ┌────┴────┐          │
   │         │          │
  YES        NO         │
   │         │          │
   ▼         ▼          │
┌────────┐  ┌──────────┐│
│ Create │  │ Use      ││
│ new    │  │ existing ││
│ device │  │ (no-op)  ││
└────┬───┘  └────┬─────┘│
     │           │      │
     ▼           │      │
┌────────────┐   │      │
│ Set cookie │   │      │
└────┬───────┘   │      │
     │           │      │
     └─────┬─────┴──────┘
           │
           ▼
   ┌───────────────┐
   │ Return 200 OK │
   └───────────────┘
```

## Files to Change

### 1. Create `src/main/kotlin/app/qur/web/QrController.kt`

New controller with:
- Inject `DeviceService`
- `@GetMapping("/qr")` handler
- Cookie reading via `ServerWebExchange`
- Device creation with `UUID`, `DeviceRole.SETUP`, 24-hour expiry
- Cookie setting via `ResponseCookie`

### 2. Update `src/main/kotlin/app/qur/SecurityConfig.kt`

Add `/qr` to the public endpoints list:
```kotlin
.pathMatchers("/", "/login", "/logins", "/qr", "/error", "/css/**", "/js/**", "/images/**").permitAll()
```

## Implementation Details

### Cookie Configuration

| Property | Value |
|----------|-------|
| Name | `device` |
| HttpOnly | `true` |
| Secure | `false` (dev) / `true` (prod) |
| Path | `/` |
| MaxAge | 24 hours |
| SameSite | `Lax` |

### New Device Creation

```kotlin
Device(
    deviceId = UUID.randomUUID().toString(),
    deviceRole = DeviceRole.SETUP,
    expiredAt = LocalDateTime.now().plusHours(24)
)
```

## Testing Considerations

- Test with no cookie → should create new device and set cookie (NEW CASE)
- Test with valid cookie → should return 200, no new cookie set
- Test with expired cookie → should create new device and set cookie
- Test cookie decryption failure → handle gracefully (create new device)
