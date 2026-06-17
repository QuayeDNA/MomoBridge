# MoMo Bridge — Android App

A standalone Android application that acts as a **local verification authority** for Mobile Money payments in Ghana. Intercepts MoMo SMS confirmations from telecom providers (MTN, Telecel, AT, T-CASH), parses them, and stores them locally. Generates API keys per store/location and connects to a lightweight relay server via WebSocket to verify payments in real time — no backend database required.

## How It Works

1. SMS received → app parses and stores locally as `PENDING`
2. Customer enters transaction reference on a website widget
3. Widget sends claim via relay → relay forwards to app via WebSocket
4. App checks local Room database → confirms or rejects the payment
5. Result flows back to the website in real time

## Key Features

- **Multi-key support** — one API key per store/location, all routed to the same app
- **Local-first** — all transaction data stored on-device with Room (SQLite)
- **Zero backend** — relay is a stateless message switchboard with no storage
- **Config-driven SMS parsing** — add new sender formats without code changes
- **Jetpack Compose UI** — Material 3 design system

## Tech Stack

| Area | Technology |
|------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Local DB | Room (SQLite) |
| Networking | OkHttp WebSocket |
| Secure Storage | EncryptedSharedPreferences (AES-256) |

## Project Structure

```
com.momobridge/
├── data/          — Room DB, DAOs, repositories
├── domain/        — Models, SMS parser, use cases
├── service/       — SMS listener, WebSocket relay client, claim handler
├── receiver/      — SMS broadcast receiver, boot receiver
├── di/            — Hilt modules
└── ui/            — Compose screens, navigation, components
```

## External Components

```
relay/             — Bun relay server (single file, zero dependencies)
widget/            — Embeddable HTML/JS verification widget
```

## Build

```bash
./gradlew assembleDebug
```

## License

MIT
