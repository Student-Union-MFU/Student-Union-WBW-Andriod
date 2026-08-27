# WBW Mobile (Android)

Native **Kotlin / Jetpack Compose** Android app for the **WBW event** ("เดินรอบดอย"),
the MFU Student Union hiking-trail activity. This is the **participant** app — it talks
to the same Go backend (`su-server`) as the web dashboard, using the `/wbw` route group.

> Not to be confused with `../su_mobile` (a separate Flutter app for the steps/leaderboard side).

## Download

<img src="store-assets/download-qr.png" alt="QR code to download the WBW app" width="220" align="right">

**[ดาวน์โหลดแอป · Download the app](https://play.googleapis.com/download/playconsole/AOTCm0Tp65IQUCV_knCZoUlTlC2jFjpjXZoSn_QDVgBj-qwazh22WrMP19w8bvl5YWOfrKILS5zelVbExCNVKCi0DW5F_JO23sXLV_ZDJn4je595nk_QI1T83t0I5sHJtC9i_wCIwwcI312BWTzcv_Uo_Mr9i6c81ybRFt9DWOpwXwJsTdFDGBoj8O2urtXJR6Bq06E)**
&nbsp;·&nbsp; scan the QR, or open the link on the phone.

- **versionName 0.4.1 / versionCode 5**, minSdk 26 (Android 8.0+), 39.5 MB.
- The link is Play's **app-sharing** download for that upload. It needs no Google account —
  it answers `200 application/vnd.android.package-archive` to anyone — and it serves this
  exact build: `sha256 1a10d8a5d51ef44becf4239975423bcda075ecceeddd72a0e8dd3f00588d6256`.
- Mirror, same bytes and same hash: [`wbw-0.4.1-universal.apk`](https://github.com/Student-Union-MFU/Student-Union-WBW-Andriod/releases/download/v0.4.1/wbw-0.4.1-universal.apk) on the v0.4.1 release.
- Being the Play build, it carries the **Play App Signing** certificate. It will therefore
  **not install over** an earlier side-loaded `wbw.apk`, which was signed with the local
  release key — Android answers `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Uninstall the old
  build first; that takes its data with it.
- The browser doing the download needs Android's **"install unknown apps"** permission.

<br clear="right">

> The URL above is tied to **this upload**. A new bundle gets a new link and a new QR —
> regenerate `store-assets/download-qr.png` when the version changes, or the poster will go
> on pointing at 0.4.1. Note also that a `…/playconsole/…` URL copied out of the Console's
> own UI is *not* this link: those are session-bound and answer HTTP 400 to everyone else.

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
