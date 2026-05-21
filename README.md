# COMPSCI 702 · Reverse Engineering Project (Android)

University of Auckland **COMPSCI 702** course project: an Android client that talks **directly** to the course server. Main flow: **prompt → `/auth` → `/generate_image` → image preview → save to gallery**.

---

## Requirements

- JDK 17
- Android Studio (stable recommended)
- Android SDK (API 34 recommended)
- AVD emulator or USB-debugged physical device

---

## Local configuration (`secrets.properties`)

Gradle reads **`secrets.properties`** at the repository root (never commit this file).

```bash
cp secrets.example.properties secrets.properties
```

| Key | Debug | Release (submission) |
|-----|-------|----------------------|
| `API_BASE_URL` | Required | Required (HTTPS base URL) |
| `AUTH_HEADER_VALUE` | Required for bootstrap | Required — encrypted into the APK at build time |
| `CERT_PINS` | Optional (easier local proxy) | **Required** — comma-separated `sha256/...` pins |
| `RELEASE_SIGNING_SHA256` | Optional | **Required** when signing with a non-debug keystore (lowercase hex, no colons) |

- **Debug:** Gradle writes `AUTH_HEADER_VALUE` to `app/src/debug/assets/auth_bootstrap.txt` (gitignored) and `BuildConfig.LOCAL_BOOTSTRAP_AUTH_VALUE`.
- **Release:** Gradle AES-GCM-encrypts `AUTH_HEADER_VALUE` into build-generated credential fragments. Plaintext is not stored in `BuildConfig` or committed sources.
- Use the **production** bootstrap key for your final signed release APK, not a test key.
- If `RELEASE_SIGNING_SHA256` is empty, the build falls back to `~/.android/debug.keystore` (emulator-only release builds).

---

## Option 1 — Run Debug (development)

### Android Studio

1. Open the **repository root** (folder with `settings.gradle.kts`).
2. Create and fill in `secrets.properties` (see above).
3. Wait for Gradle sync, select a device, run the **app** configuration (debug variant).

### Command line

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APK path: `app/build/outputs/apk/debug/app-debug.apk`

---

## Option 2 — Build signed Release (submission APK)

Configure `secrets.properties` with your **production** `AUTH_HEADER_VALUE`, valid `CERT_PINS`, and `RELEASE_SIGNING_SHA256` matching the keystore you will use to sign the APK.

### Android Studio (recommended for signing)

1. **Build → Generate Signed Bundle / APK → APK**.
2. Select your release keystore and build the **release** variant.
3. Install the signed APK on a device or emulator and verify: prompt → generate → preview → save.

Credential fragments are derived from the signing certificate SHA-256 at build time. The installed APK’s signature must match `RELEASE_SIGNING_SHA256`, or bootstrap decryption and integrity checks may fail.

### Command line

```bash
./gradlew :app:assembleRelease
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk` (may be unsigned depending on your Studio/Gradle signing setup). Sign with Android Studio or your keystore before installing on a device.

---

## Submission package

| Deliverable | Contents |
|-------------|----------|
| **Source zip** | Source tree, `README.md`, `secrets.example.properties`, Gradle wrapper |
| **Release APK** | Signed release APK built with production credentials (separate file) |

**Do not include** in the source zip: `secrets.properties`, `*.jks` / `*.keystore`, `app/build/`, `.gradle/`, `local.properties`, or `app/src/debug/assets/auth_bootstrap.txt`.

---

## Security (summary)

- Release: **R8 / ProGuard**, resource shrinking, no cleartext, certificate pinning, host allowlist, build-time encrypted bootstrap credential.
- **StringFog** (open source) on app package string literals only — not a substitute for secret protection.
- **EncryptedSharedPreferences** for session tokens after bootstrap.
- No commercial obfuscators; app communicates only with the course server.

---

## Troubleshooting

| Issue | Check |
|-------|--------|
| Release build fails at configure | `CERT_PINS` set in `secrets.properties` |
| `/auth` returns 401 on release | Production `AUTH_HEADER_VALUE`; rebuild after changing secrets; `RELEASE_SIGNING_SHA256` matches signing cert |
| Certificate / SSL errors | Update `CERT_PINS`; disable MITM proxy on release builds |
| Generation blocked immediately | Release integrity checks (signature mismatch, hook/debug risk) |

---

*Course assignment repository — not a production product.*
