# MoMo Bridge

**On-device Mobile Money verification for Ghana.** A standalone Android app that intercepts MoMo SMS (MTN, Telecel, AT, T-CASH), parses them locally, and verifies payments via WebSocket — no backend database required.

## How It Works

1. **SMS received** — app intercepts and parses the SMS, storing it locally as `PENDING` in Room (SQLite)
2. **Multi-store API keys** — generate one key per store/location; all route to the same app via a shared relay
3. **Claim flow** — a website widget sends a claim request through the relay → relay forwards it via WebSocket to the app → app checks its local DB → confirms or rejects in real time
4. **Push notifications** — instant alerts when a payment is confirmed, already claimed, or expires

The relay is a **stateless message switchboard** with zero storage. The app is the sole source of truth.

## Features

- **SMS parsing engine** — regex-based, config-driven parser with heuristic fallback; supports all major Ghanaian networks
- **Auto-detect wizard** — scans your SMS inbox to build parsing rules for new sender formats
- **Multi-key management** — create, revoke, and reactivate API keys per store with usage tracking
- **Transaction log** — full history with status filtering (pending/confirmed/failed/expired) and store attribution
- **Dashboard** — summary counts, recent transactions, live connection status
- **Real-time verification** — WebSocket connection to relay server with auto-reconnect and heartbeat
- **Push notifications** — `CHANNEL_ID_EVENTS` (IMPORTANCE_HIGH) fires for confirmed claims, already-confirmed warnings, expired transactions, and claim errors
- **Widget integration** — embeddable `momobridge.js` with popup/inline/redirect modes; dark financial terminal UI with gold accent
- **Relay server** — single-file Bun server, deployable to Render free tier
- **LLM fallback** — when parsing repeatedly fails, delegates to Gemini Nano (on-device) for extraction

## Tech Stack

| Area | Technology |
|------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Local DB | Room (SQLite) |
| Networking | OkHttp WebSocket |
| Secure Storage | EncryptedSharedPreferences (AES-256) |
| SMS Parsing | Regex + heuristic engine |
| LLM Fallback | Gemini Nano (on-device) |
| Relay Server | Bun (single file, zero deps) |
| Widget | Vanilla JS (zero deps) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |

## Screenshots

| Dashboard | Transactions | API Keys | Settings |
|---|---|---|---|
| Summary counts, recent txns, connection dot | Full log with store/status filters | Key list, revoke, reactivate | Senders, relay, expiry, about |

*(Screenshots coming soon)*

## Project Structure

```
com.momobridge/
├── MomoBridgeApp.kt              — Application class, notification channels
├── data/
│   ├── local/                    — Room entities, DAOs (sms_transactions, api_keys)
│   └── repository/               — TransactionRepository, ApiKeyRepository, SmsSourceRepository
├── domain/
│   ├── model/                    — ParsedTransaction, SmsSource, ParsingRule
│   ├── parser/                   — SmsParser, FieldExtractor, AutoDetectUtils, SmsClassifier
│   └── usecase/                  — ProcessSmsUseCase, ScanInboxUseCase, LlmFallbackUseCase
├── service/
│   ├── SmsListenerService.kt     — Foreground service, expiry checker (60s)
│   ├── RelayClient.kt            — WebSocket client, claim handler, push notifications
│   ├── ClaimHandler.kt           — Local claim verification logic
│   └── NotificationHelper.kt     — Push notification dispatcher
├── receiver/
│   ├── SmsBroadcastReceiver.kt   — SMS_RECEIVED broadcast
│   └── BootReceiver.kt           — Restart services after reboot
├── di/                           — AppModule, DatabaseModule, Qualifiers
└── ui/
    ├── splash/                   — Animated gold MB logo
    ├── setup/                    — 4-step wizard (profile → stores → relay → senders)
    ├── dashboard/                — Summary cards, recent list, connection dot
    ├── transactions/             — Filterable log with detail bottom sheet
    ├── apikeys/                  — Key CRUD with copy, revoke, usage dates
    ├── settings/                 — Category menu with 4 sub-screens
    ├── help/                     — FAQ / usage guide
    ├── navigation/               — AppNavigation, BottomNavBar, MainTabViewModel
    └── components/               — TransactionCard, StatusBadge, StepIndicator, etc.
```

## External Components

```
project-root/
├── relay/
│   └── index.ts              — Bun WebSocket + HTTP relay server (deploy to Render)
├── widget/
│   ├── momobridge.js          — Embeddable widget (popup/inline/redirect)
│   ├── widget.html            — Standalone verification page
│   ├── index.html             — Landing page / docs
│   └── vercel.json            — Vercel deployment config
└── AGENTS.md                 — Full architecture documentation
```

## Build & Install

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Deploy Relay

```bash
cd relay
bun run index.ts
```

Deploy to Render: set root to `relay`, start command `bun run index.ts`, health check `/health`.

## Deploy Widget

Push `widget/` to GitHub — Vercel auto-deploys from the `widget/` directory.

## Permissions

- `RECEIVE_SMS` / `READ_SMS` — intercept MoMo SMS
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC` — listener + relay
- `POST_NOTIFICATIONS` — Android 13+ push alerts
- `RECEIVE_BOOT_COMPLETED` — restart after reboot
- `INTERNET` + `ACCESS_NETWORK_STATE` — WebSocket connection

## License

MIT
