# WBW Mobile (Android)

Native **Kotlin / Jetpack Compose** Android app for the **WBW event** ("เดินรอบดอย"),
the MFU Student Union hiking-trail activity. This is the **participant** app — it talks
to the same Go backend (`su-server`) as the web dashboard, using the `/wbw` route group.

> Not to be confused with `../su_mobile` (a separate Flutter app for the steps/leaderboard side).

## Features (current scaffold)

- **Auth** — login + registration against `POST /wbw/auth/{login,register}`, JWT bearer token
  (30-day) persisted in DataStore and attached by an OkHttp interceptor.
- **Profile viewing** — the participant's own profile from `GET /wbw/me`
  (name, bib, group, school, medical, emergency contact, check-in status).
- **Notifications** — in-app announcement feed from `GET /wbw/notifications`, pull-to-refresh,
  level colour coding (info / warning / emergency).

> **Push (FCM) is not wired yet** — the Go backend does not send pushes today
> (see the `⚠ ของเดิมยิง FCM push` note in `su-server`). The app pulls the list for now;
> add Firebase Messaging + `google-services.json` when the backend sends pushes.

## Architecture

```
app/src/main/java/th/ac/mfu/su/wbw/
├── WbwApplication.kt        # builds AppContainer, primes the auth token
├── MainActivity.kt          # splash + Compose host
├── di/AppContainer.kt       # hand-rolled DI (swap for Hilt if it grows)
├── core/network/            # Retrofit/OkHttp, AuthInterceptor, ApiResult + error parsing
├── data/
│   ├── remote/              # WbwApi (Retrofit) + DTOs mirroring the Go models
│   ├── local/SessionStore   # DataStore-backed token/session
│   └── repository/          # Auth / Profile / Notification repositories
└── ui/                      # Compose: theme, auth graph, home scaffold, profile, notifications
```

Layering: **Screen → ViewModel → Repository → WbwApi**. ViewModels expose `StateFlow` UI state
and are built via `viewModelFactory` reading the `AppContainer` from `CreationExtras`.

## Toolchain

Proven on this machine — **do not** re-add the standalone Kotlin plugin:

| Tool | Version |
|------|---------|
| Gradle | 9.1.0 (wrapper) |
| Android Gradle Plugin | 9.0.1 |
| Kotlin (compiler plugins) | 2.3.20 |
| JDK | 25 |
| compileSdk / targetSdk | 36 |
| minSdk | 26 |

> AGP 9 has **built-in Kotlin** — applying `org.jetbrains.kotlin.android` fails.
> Only the Compose and kotlinx.serialization compiler plugins are applied.

## Build & run

```bash
# local.properties must point at the SDK (sdk.dir=...); already set for this machine.
./gradlew :app:assembleDebug          # build debug APK
./gradlew :app:installDebug           # install on a connected device/emulator
```

## Backend base URL

Configured per build type via `API_BASE_URL` in `app/build.gradle.kts` (must end in `/wbw/`):

- **debug** — `http://10.0.2.2:8080/wbw/` (emulator → host's Go server).
  On a physical device, change to the LAN IP or the Cloudflare tunnel host.
- **release** — `https://api.example.com/wbw/` — **replace before shipping.**

Cleartext http is allowed only for local dev hosts (`res/xml/network_security_config.xml`).
