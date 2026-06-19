# MoMo Bridge — Android App

## Overview

MoMo Bridge is a standalone Android application that acts as a **local verification authority** for Mobile Money payments. It intercepts incoming MoMo SMS confirmations from Ghanaian telecom providers (MTN, Telecel, AT, T-CASH), parses them, and stores them locally on the device. The app generates **multiple API keys** (one per store/location) and connects to a **shared lightweight relay server** via WebSocket. The relay is **multi-tenant** — many app instances connect to the same relay URL, and the API key routes requests to the correct app. Any website can embed a small widget that sends claim requests through the relay to the app — the app looks up its local Room database and confirms or rejects the payment in real time.

**Key insight — the app IS the source of truth. No backend database, no wallet system, no user accounts.** The relay is just a multi-tenant message switchboard with zero storage. Each API key is the routing mechanism. The app handles all verification logic locally.

**Multi-key architecture:** A vendor with multiple stores generates one API key per store. The relay maps all keys to the same app WebSocket. Widgets embed with the specific store's key. When a claim is confirmed, the app saves which key label claimed it (`claimedByKeyLabel`), enabling per-store transaction filtering and audit trails.

### Flow Diagram

```
┌──────────────────┐  WebSocket (persistent)  ┌──────────────┐  HTTP POST  ┌──────────────────────┐
│  MoMo Bridge App  │◄────────────────────────►│  Relay        │◄───────────│  Web Widget          │
│  (vendor's phone)  │                         │  (switchboard) │           │  (customer's browser) │
│                    │                         │                │            │                      │
│  - SMS intercept   │                         │  No DB         │            │  - Customer enters   │
│  - Local Room DB   │                         │  No storage    │            │    reference #       │
│  - Verifies claims  │                         │  In-memory map │            │  - Sends to relay    │
│  - Generates API key│                        │  of apiKey↔WS  │            │  - Gets yes/no       │
└──────────────────┘                          └────────────────┘            └──────────┬───────────┘
                                                                                     │ callback
                                                                                     ▼
                                                                            ┌──────────────────────┐
                                                                            │  Parent Web App      │
                                                                            │  (your ecommerce)     │
                                                                            │                      │
                                                                            │  - Credits wallet    │
                                                                            │  - Marks paid        │
                                                                            └──────────────────────┘
```

### Full Claim Flow

1. Customer sends MoMo → vendor's phone intercepts SMS
2. App parses SMS → saves to Room DB as `PENDING`
3. Customer checks out on website → enters reference in widget
4. Widget POSTs to relay: `{ apiKey, reference, amount, callbackUrl }`
5. Relay looks up the WebSocket connection for that `apiKey`
6. Relay forwards the request to the app via WebSocket
7. App checks local Room DB:
   - Not found → reply `invalid`
   - Already `CONFIRMED` → reply `already_confirmed`
   - `PENDING` + amount matches → mark `CONFIRMED` with timestamp → reply `confirmed`
   - `PENDING` + amount mismatch → reply `amount_mismatch`
   - `EXPIRED` → reply `expired`
8. Reply flows back: App → WebSocket → Relay → Widget
9. Widget POSTs to `callbackUrl` with result
10. Web app credits the customer's wallet

## Tech Stack

| Area | Technology |
|------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt (Dagger) |
| Local DB | Room (SQLite) |
| Networking | OkHttp (WebSocket client) |
| SMS Interception | BroadcastReceiver (SMS_RECEIVED) |
| Secure Storage | EncryptedSharedPreferences (Android Keystore AES-256) |
| Relay Server | Bun (single-file, no dependencies) |
| Web Widget | Vanilla HTML/JS (zero dependencies) |
| Min SDK | API 26 (Android 8.0) |
| Target SDK | 35 |

## Project Structure

```
com.momobridge/
├── MomoBridgeApp.kt              # Application class, Hilt entry point, notification channels
├── data/
│   ├── local/                    # Room database layer
│   │   ├── SmsTransactionEntity.kt   # Room entity with status: PENDING/CONFIRMED/FAILED/EXPIRED
│   │   ├── SmsTransactionDao.kt      # DAO with Flow-based observation + findByReference
│   │   ├── ApiKeyEntity.kt           # Room entity for API keys (label, hash, active, dates)
│   │   ├── ApiKeyDao.kt              # DAO for API key CRUD + active count Flow
│   │   └── MomoBridgeDatabase.kt     # Room database definition (v3)
│   └── repository/
│       ├── TransactionRepository.kt  # Save, query, confirm transactions locally
│       └── ApiKeyRepository.kt       # Generate, store, revoke, reactivate API keys
├── domain/
│   ├── model/
│   │   ├── ParsedTransaction.kt      # Pure Kotlin data class
│   │   └── SmsSource.kt             # Sender address + parsing rule config
│   ├── parser/
│   │   ├── AutoDetectUtils.kt       # Shared auto-detect utility
│   │   └── SmsParser.kt            # Parsing engine — apply ParsingRule → ParsedTransaction
│   └── usecase/
│       └── ProcessSmsUseCase.kt     # Parse + save locally
├── service/
│   ├── SmsListenerService.kt     # Foreground service — processes incoming SMS
│   ├── RelayClient.kt            # WebSocket client — connects to relay, handles messages
│   └── ClaimHandler.kt           # Verifies claim requests against local Room DB
├── receiver/
│   ├── SmsBroadcastReceiver.kt   # SMS_RECEIVED broadcast → starts SmsListenerService
│   └── BootReceiver.kt           # Restart listener + relay after reboot
├── di/
│   ├── AppModule.kt              # EncryptedSharedPreferences + regular SharedPreferences
│   └── DatabaseModule.kt         # Room database + DAO singleton
├── domain/
│   └── ApiKeyGenerator.kt        # Generate mb_ prefixed API key
└── ui/
    ├── MainActivity.kt           # Single activity, NavHost with routes
    ├── theme/                    # MomoColors, MomoType, MomoSpacing, MomoShapes, MomoMotion, Theme
    ├── splash/
    │   └── SplashScreen.kt      # Animated gold MB logo + version
    ├── setup/
    │   ├── SetupScreen.kt       # 4-step wizard: profile → add store(s) → relay → senders
    │   └── SetupViewModel.kt
    ├── dashboard/
    │   ├── DashboardScreen.kt   # Summary counts + recent transactions + connection dot
    │   └── DashboardViewModel.kt
    ├── settings/
    │   ├── SettingsScreen.kt    # Relay status, sender management, expiry config
    │   └── SettingsViewModel.kt
    ├── transactions/
    │   ├── TransactionsScreen.kt # Full transaction log with store/status filters
    │   └── TransactionsViewModel.kt
    ├── apikeys/
    │   ├── ApiKeysScreen.kt     # Active + revoked keys, add, edit, revoke, reactivate
    │   └── ApiKeysViewModel.kt
    ├── settings/
    │   ├── SenderConfigScreen.kt # 3-step wizard for parsing rules
    │   └── SenderConfigViewModel.kt
    ├── help/
    │   └── HelpScreen.kt        # FAQ / usage guide
    ├── navigation/
    │   ├── AppNavigation.kt     # Outer nav (splash, setup, main, sender config, help)
    │   ├── BottomNavBar.kt      # 4-tab bottom bar (Dashboard, Transactions, Keys, Settings)
    │   └── MainTabViewModel.kt  # Provides apiKeyCount for badge
    └── components/
        ├── TransactionCard.kt        # Core UI atom — network, amount, status
        ├── TransactionDetailDialog.kt # Bottom-sheet detail view
        ├── StatusBadge.kt            # Status indicator with count
        ├── SmsSourceCard.kt          # Sender config card
        ├── StepIndicator.kt          # Setup wizard step dots
        ├── MomoButtons.kt            # Gold pill CTA / outline buttons
        ├── MomoTextField.kt          # Dark input with gold focus ring
        ├── DisplayHelpers.kt         # AmountText, TimestampText helpers
        ├── EmptyState.kt             # Crafted empty state illustration
        └── SkeletonShimmer.kt        # Loading shimmer placeholder
```

## External Files

```
project-root/
├── relay/
│   └── index.ts              # Bun server — WebSocket + HTTP claim endpoint
├── widget/
│   ├── momobridge.js          # Embeddable widget library — popup/inline/redirect modes
│   ├── widget.html            # Standalone verification page (redirect flow)
│   └── example.html           # Demo page with all three modes
├── AGENTS.md                 # This file
├── DESIGN.md                 # Full design system specification
└── sms_samples.txt           # Known SMS formats for testing
```

## Key Architecture Decisions

### Relay Server (Bun, single file)
- One HTTP endpoint: `POST /claim` — receives `{ apiKey, reference, amount, callbackUrl }`
- One WebSocket path: `/ws?apiKey=xxx` — app connects and stays alive
- In-memory `Map<apiKey, WebSocket>` + reverse map for multi-key — no database, no file system, no persistence
- App sends `{ type: "auth", allKeys: ["mb_...", "mb_..."] }` on connect — relay registers all keys pointing to same WebSocket
- `revoke_key` message removes a single key from the map without disconnecting
- On `/claim` request: look up WebSocket by apiKey → forward `{ type: "claim_request", apiKey, reference, amount }` → wait for app response → respond to HTTP caller
- If app is disconnected, return `{ confirmed: false, message: "phone offline" }`
- Heartbeat every 30s from both sides to detect stale connections
- Deploys to Render free tier: `bun run relay/index.ts`

### App-Side Claim Verification
- `RelayClient` connects to relay on app startup (or background reconnect)
- Uses OkHttp WebSocket — no additional dependencies
- Authenticates immediately after connect by sending `{ type: "auth", allKeys: [...] }` with all active key values from ApiKeyRepository
- Listens for `{ type: "claim_request", apiKey, reference, amount }` messages
- Looks up the key entity by value to get the label, marks `lastUsedAt` on that key
- Delegates to `ClaimHandler` which queries Room DB:
  - `findByReference(reference)` → returns the most recent match by receivedAt
  - Validates: exists? amount matches (±0.01)? not expired? not already confirmed?
  - If valid: updates status to `CONFIRMED`, sets `confirmedAt`, sets `claimedByKeyLabel`, returns success
  - Returns specific error message for each failure case
- `ClaimHandler` returns a result object; `RelayClient` serializes and sends back via WebSocket

### SMS Parsing Engine (unchanged)
- Config-driven: each network has a `SmsSource` with sender address + regex patterns
- Parser returns null (discarded) for unparseable messages — never throws
- New networks added by creating one `SmsSource` entry; no parser changes needed
- Auto-detect highlights likely fields from inbox messages

### API Key Generation
- Generated during setup: `mb_` + 32 random hex chars (48 chars total)
- Key value stored in EncryptedSharedPreferences (Android Keystore AES-256)
- Key metadata (label, hash, active flag, dates) stored in Room `api_keys` table
- Multiple keys supported — create one per store/location
- Keys can be revoked (deactivated) without deleting the value — reactivation restores access
- Permanent deletion removes both Room metadata and SecurePrefs value
- Key detail bottom sheet shows full key (copyable), label (editable), creation/last-used dates
- Active key count shown as badge on Keys tab in bottom navigation

### Transaction Lifecycle (locally)

| Status | Meaning | Set When |
|--------|---------|----------|
| `PENDING` | SMS received, parsed, awaiting claim | On SMS intercept |
| `CONFIRMED` | Verified through widget, reference used | On successful claim via relay |
| `EXPIRED` | 24h passed without claim | Background expiry check |
| `FAILED` | Parse error, corrupted data | On SMS parse failure |

### Statuses Removed
- `UPLOADED` — no backend to upload to
- `DUPLICATE` — no external duplicate detection needed (local DB unique constraint handles it)
- `CLAIMED` — renamed to `CONFIRMED` for clarity (claiming implies a backend action; confirming is local verification)

### Database
- Unique index on `reference` only (no vendorId composite)
- `confirmedAt: Long?` column added — null until confirmed
- `claimedByKeyLabel: String?` column added — populated on confirmation with the store label
- `vendorId` column removed — no longer needed
- `expiresAt` still set to receivedAt + 24h — Room query filters expired

### Widget Interface
- Embeddable HTML snippet: `<iframe>` or inline `<div>` with vanilla JS
- Input: transaction reference text field
- Optional: amount (pre-filled from cart, shown to customer to confirm)
- `currencySymbol` option (default `GH₵`) — configurable via options or `&currencySymbol=` URL param in redirect mode
- Submit button → POST to relay (CORS handled by relay)
- Response shown in rich result card:
  - **Success:** green card with checkmark, confirmed amount, reference, friendly message
  - **Already confirmed:** amber warning card
  - **Error:** red card with specific friendly message (invalid/amount_mismatch/expired/offline), "Try Again" button
- Friendly error mapping converts raw relay messages to user-friendly text
- PostMessage to parent window: `window.parent.postMessage({ type: "momo_result", confirmed, reference, amount }, "*")`
- Parent web app listens for this message and triggers wallet credit

## Permissions (AndroidManifest.xml)
- `RECEIVE_SMS` / `READ_SMS` — intercept MoMo SMS
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` — keep listener + relay alive
- `POST_NOTIFICATIONS` — Android 13+ foreground notification
- `RECEIVE_BOOT_COMPLETED` — restart service after reboot
- `INTERNET` + `ACCESS_NETWORK_STATE` — WebSocket connection to relay

## Security
- API key stored in EncryptedSharedPreferences (Android Keystore AES-256)
- Relay should use HTTPS in production (Render provides SSL by default)
- WebSocket always `wss://` in production
- API key is the sole authentication credential — treat it like a password
- If a device is lost/stolen, the API key is compromised; reinstall and generate new key
- No user data leaves the device except the API key and claim request/response pairs

## Build & Deploy

### Android App
```bash
cd MomoBridge
./gradlew assembleDebug
```

### Relay Server
```bash
cd relay
bun install    # (no deps, just for type checking)
bun run index.ts
```

### Deploy to Render
1. Create new Web Service on Render
2. Set root directory to `relay`
3. Build command: `bun install` (empty, runs)
4. Start command: `bun run index.ts`
5. Set environment: `PORT=8080`
6. Health check path: `/health`

### Widget
- Serve `widget/momobridge.js` as the embeddable library (exposes `window.MoMoBridge`)
- Three modes: `popup(apiKey, relayUrl)`, `inline(apiKey, relayUrl, containerId)`, `redirect(apiKey, relayUrl)`
- `widget/widget.html` is the standalone verification page for redirect flow
- `widget/example.html` is a demo page with config inputs and all three modes
- Configure `relayUrl` and `apiKey` in script config
- Place in checkout page where customer enters payment reference

## Testing Notes
- Test SMS samples in `sms_samples.txt`
- Test relay locally: `curl -X POST http://localhost:8080/claim -H "Content-Type: application/json" -d '{"apiKey":"...","reference":"0000013331054115","amount":18}'`
- Verify Widget → Relay → App → Relay → Widget round trip
- Test offline: app without internet → claim returns "phone offline"
- Test expiry: set `expiresAt` to past in Room DB → claim returns "expired"
- Test double-claim: first claim succeeds, second returns "already_confirmed"
- Test multi-key: create two keys (Main Shop, Online Store) → claim with first key → transaction shows `via Main Shop`

---

## Session Summary — 2026-06-19

### Settings Redesign — Category Menu with Sub-Screens

Rewrote `SettingsScreen.kt` from a flat 632-line scrolling list with mixed concerns to a clean **category menu** with 4 dedicated sub-screens:

- **Category menu row layout** — "Senders & Parsing", "Connection", "Transaction Rules", "About". Each row shows title + summary subtitle + gold chevron, separated by thin dividers.
- **Senders & Parsing sub-screen** — sender list with SmsSourceCard (toggle/configure/delete), Scan Inbox button + dialog, Add Manually button + dialog. Empty state text when no senders.
- **Connection sub-screen** — status dot + label, relay URL (tappable → edit dialog), API key with copy button, reconnect button.
- **Transaction Rules sub-screen** — expiry toggle switch + hour text field (with day/hour helper text), "Scan Inbox for Past Transactions" gold button with result text.
- **About sub-screen** — MoMo Bridge title + version, "Help & Support" outline button, "Reset All Settings" danger button with confirmation dialog.
- Sub-screen navigation via `SettingsCategory` enum state — no new navigation routes needed. Back button ("← Settings" with arrow icon + gold text) returns to menu. Temporary state (scan results, historical result) cleared on back.
- `SubScreenHeader` composable shared across all 4 sub-screens (back row + title).
- Kept existing `SettingsViewModel` unchanged — it already provides all state and methods for all sub-screens.

### Bottom NavBar Redesign

`BottomNavBar.kt` — gold active indicator line (20×3dp pill above icon), uppercase monospace labels, 1.05x spring scale animation on active icon, zero tonal elevation. Keys badge preserved.

### Header Consistency Across All Screens

Changed `containerColor` from `MomoColors.GroundMedium` to `MomoColors.GroundDark` on all TopAppBars:

| File | Change |
|------|--------|
| `DashboardScreen.kt:111` | GroundMedium → GroundDark |
| `TransactionsScreen.kt:77` | GroundMedium → GroundDark |
| `ApiKeysScreen.kt:285` | GroundMedium → GroundDark |
| `HelpScreen.kt:98` | GroundMedium → GroundDark |
| `SetupScreen.kt:110` | GroundMedium → GroundDark |
| `SenderConfigScreen.kt:69` | GroundMedium → GroundDark |

Headers now blend into the page background — only gold text and status dots are visible, consistent with Settings.

### Build Verification
- Build: SUCCESSFUL
- ADB install: Success
