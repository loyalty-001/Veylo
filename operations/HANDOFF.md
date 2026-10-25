# Veylo operations dashboard — Antigravity handoff

## User instruction

Build the entire agreed analytics and announcements plan, including download numbers. The user has now paused this environment and will continue implementation in Antigravity. Continue in this branch; preserve the working Android app and public website. Read the repository's AGENTS.md first.

## Exact state at handoff

- This branch: `work/operations-dashboard`.
- Stable main/release: `c4d3d9100a7d11c9ed22ebb1e1fd8d11ee8b8913`, tag `v3.0.2`.
- Android version: 3.0.2, versionCode 21. Do not bump or publish another release without the owner's instruction.
- GitHub: https://github.com/loyalty-001/Veylo
- Public download site: https://veylo-lra9.onrender.com/
- Official download: https://github.com/loyalty-001/Veylo/releases/latest/download/Veylo.apk
- Veylo 3.0.2 is published, tested, and signed with the same permanent certificate as 3.0.1. The current release updates the official installed app in place.
- The operations backend/dashboard has NOT been implemented or deployed. There is no dashboard login yet.
- Only npm scaffolding and a dedicated, empty Cloudflare D1 database have been created. No app analytics are being collected yet. No announcements API or telemetry is wired into Android or the website.
- Dependencies were installed locally; node_modules is intentionally ignored. Use `npm ci` in `operations/` elsewhere. The package lock pins the installed versions. Declared scripts are implementation targets; build.mjs and application sources do not exist yet.
- No admin password, bootstrap token, or runtime authentication secret has been generated. Never reuse a GitHub token as an application secret. Use connected GitHub/Cloudflare tooling or owner-provided environment credentials; never commit credentials.

## Hosting handoff

Keep the public download site on Render. Use a separate Cloudflare Worker and a dedicated D1 database for persistent operations data and the admin dashboard.

A dedicated empty database was prepared in the owner's connected Cloudflare account. Locate that resource through the owner's Cloudflare dashboard or authenticated tooling. Infrastructure identifiers and deployment credentials are intentionally excluded from this public handoff. Do not modify databases belonging to other projects.

Choose and verify the Worker name and origin during setup. No operations Worker or admin URL has been deployed yet. The current public website source is `website/` and its existing Render deployment must be preserved.

## Full agreed scope

### Overview and usage

- Daily/weekly/monthly active opted-in installations, first launches, retention, app-version adoption, and feature usage.
- Device model/family and Android version for reliability troubleshooting, without unnecessary identifiers.
- Treat an installation ID as an installation, not a known person. Reinstalls/data clearing can change IDs. Do not claim exact unique people.
- Show real data and honest empty states. Instrumentation starts with the next instrumented app release; never fabricate historical active-user or install counts.

### Downloads and growth

- GitHub download_count for each release and APK variant, with totals and freshness timestamps.
- Website page views and download-button clicks.
- Distinguish clicks, APK downloads, first launches, and active installations. A download is neither a verified installation nor a unique person.
- Cache public GitHub Releases API results server-side to respect rate limits. A GitHub personal token is not required for public read-only counts.
- Preserve lifetime GitHub counts; calculate daily increases only after recording real snapshots.

### App health and feedback

- Crash/error counts, playback failures, buffering metrics, download failures, update-download/verification/install failures.
- Group failures by app version, device/Android, and safe error code or fingerprint.
- A Report a problem form in the app with optional user-entered detail and an explicit send action. Include version/device information transparently.
- Dashboard list/detail/status workflow for reports.
- Use the existing crash handler and playback/update hooks. Do not automatically upload raw logs, exception messages, file paths, URLs, tokens, or watch-history breadcrumbs.

### Announcements and status

- Draft, preview, publish, schedule, expire, edit, and unpublish announcements.
- Types: general banner, important popup, service-status notice.
- Version targeting, start/end dates, title/body, optional action label and HTTPS URL, dismiss control, and show-once tracking for popups.
- Plain text rendering and safe links; no arbitrary HTML or remotely executed code.
- Do not interrupt video, Shorts, picture-in-picture, onboarding, or existing update dialogs. Coordinate prompts so they cannot stack with donation/analytics prompts.
- Changes reach clients on refresh/startup; do not promise instantaneous pushes without implementing an appropriate delivery channel.

### Releases and emergency controls

- Release/adoption/download overview and a control to stop offering a faulty version.
- A backend update endpoint should select only an eligible, unblocked stable release and preserve the raw release/asset fields the existing Android parser needs.
- Respect the block in both the in-app update path and the website's recommended download path. Do not fall back to an unfiltered GitHub latest endpoint when a release is intentionally blocked.
- Blocking an offered release does not uninstall or downgrade already installed apps, and cannot remove public GitHub files. A fixed app still requires a higher versionCode.
- Fixed, allowlisted switches for actual app features such as video playback, Shorts, music, downloads, and offering updates. Wire switches to central entry points; dashboard-only controls do not fulfill the requirement.
- Keep local defaults and bounded configuration caching so backend outages do not break normal app startup/playback. Do not introduce an indefinite remote lockout or arbitrary-code control.

### Admin security

- Private web admin page usable on phone/laptop, with approved admin identity, password sign-in and authenticator-based two-factor authentication.
- Maintained auth/session/TOTP implementation; no homemade crypto, password hashing, sessions, or TOTP.
- Better Auth 1.7.13 is installed and supports native Cloudflare D1. Its twoFactor plugin supports TOTP enrollment, verified codes, and backup codes.
- Better Auth 1.7.13's @better-auth/utils package has a workerd export for native node:crypto password hashing. Bundle with the workerd condition and supported Node compatibility; verify the deployed login path, not just Node tests.
- Use Better Auth's schema/migration tooling instead of guessing its tables.
- A single-use private bootstrap flow can let the owner create the first admin and enroll their own authenticator. Keep setup secrets out of source, logs, app binaries, and public URLs/referrers. Close public registration after bootstrap. Do not create an owner password or authenticator on their behalf.
- Require completed 2FA for every admin API, not merely a hidden URL or a frontend check.
- Secure HttpOnly cookies, session expiry/logout, CSRF/origin protection, rate limiting, server-side validation, prepared SQL, and an audit trail for administrative changes.
- No runtime GitHub write credential is needed for this dashboard.

### Privacy controls

- Keep viewing/listening history on the user's phone. No video/song titles, content IDs, watch URLs, accounts, advertising IDs, contacts, or raw IP retention in analytics.
- Explain the collected data, provide an analytics opt-out and a way to delete/reset the installation's collected data.
- Public announcements/configuration must continue to work when analytics is disabled.
- Bounded telemetry queues, batch uploads, backoff, and retention/cleanup. Never block app startup/playback on reporting.
- Clearly identify opted-in/pseudonymous metrics and collection limits in the dashboard.

## Suggested implementation layout

- `operations/src/`: Hono Worker, Better Auth setup, input schemas, D1 access, admin/public routes, GitHub release cache, scheduled cleanup.
- `operations/web/`: responsive branded admin UI. A small vanilla-JS UI is sufficient; no fake numbers or decorative gradients/glass/glows.
- `operations/migrations/`: generated auth schema plus operations tables and indexes.
- `operations/tests/`: meaningful access-control, 2FA-gating, targeting/scheduling, data-deletion, release-blocking, and ingestion-validation tests.
- `operations/wrangler.jsonc`: Worker/D1 configuration; secrets are runtime bindings only.
- Android: reuse OkHttp, kotlinx.serialization, existing DataStore, WorkManager, Hilt, Material 3, and shared dialogs/settings components. Do not add overlapping SDKs/dependencies.
- Website: record page views/download clicks and use the controlled release/download endpoint while retaining the V favicon, share preview, existing design, source/license credits, and latest-version display.

## Relevant Android entry points

- `FlowApplication.kt`: existing crash-handler setup and app initialization.
- `ui/FlowApp.kt`: root navigation and prompt/player visibility coordination.
- `data/update/UpdateRepository.kt`: release fetching and announcement scheduling.
- `ui/screens/update/UpdateLaunchViewModel.kt`: current update popup.
- `ui/startup/LaunchPrompts.kt`: startup update/donation coordination.
- `utils/FlowCrashHandler.kt`: local crash logs; these can contain sensitive breadcrumbs and must not be blindly uploaded.
- `player/analytics/PlaybackAnalyticsLogger.kt`: existing local playback diagnostics.
- `player/EnhancedPlayerManager.kt` and `player/EnhancedMusicPlayerManager.kt`: playback entry points (locate actual methods before modifying).
- `data/video/VideoDownloadManager.kt` and `data/music/DownloadManager.kt`: central download entry points.
- `ui/screens/settings/history/HistorySettingsScreen.kt`, corresponding ViewModel, and `ui/screens/settings/index/HistoryIndex.kt`: current History & privacy settings.
- `data/local/LocalDataManager.kt` exposes the shared `Context.dataStore`. Reuse it instead of creating a competing preferences store.
- `app/src/main/res/values/strings.xml`: all new English user-facing Android strings. Do not alter locale files.

## Validation and delivery

1. Inspect AGENTS.md and existing APIs; keep changes focused and preserve GPL/Flow credits.
2. Implement backend, UI, public APIs, Android instrumentation/controls, and website integration; do not stop at a static dashboard mockup.
3. Validate unauthorized access, first-owner bootstrap, MFA enrollment/login, blocked signups, announcement scheduling/version targeting, opting out/deleting data, release blocking, and actual feature-switch enforcement.
4. Verify real D1 persistence and deployed Worker behavior. Keep test data separate or remove it; do not pollute real user counts.
5. Run TypeScript checks and meaningful backend tests. Run the repository's pinned Kotlin formatter before Kotlin/Gradle commits and the appropriate Android CI/build checks.
6. Test the admin UI on narrow and desktop layouts, with loading/error/empty states, drafts, preview, publish/unpublish, and report/release controls.
7. Push the completed work and provide the deployed admin URL plus a private first-owner setup path. Human authenticator enrollment remains the owner's action.
8. Do not publish a new Android version merely for this handoff. Finish and validate implementation before the owner authorizes the next app release.
