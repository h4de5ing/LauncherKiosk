# SingleAppKiosk

[中文文档](README.zh-CN.md)

SingleAppKiosk is an open-source Android launcher for simple kiosk-style devices. It replaces the normal home screen with a controlled app launcher, lets an administrator choose which apps are available, and provides lightweight safeguards for devices used in public, retail, education, demo, or dedicated-task environments.

The project is intentionally small and self-contained. It is built with native Android views, Kotlin, Gradle, and AndroidX.

## Features

- Home launcher mode with `HOME` intent support.
- App allowlist for controlling which installed apps can be launched.
- Administrator password before entering settings.
- First-run setup flow for creating the admin password.
- Device Admin integration to raise the uninstall barrier.
- Accessibility-service based blocking for selected System UI controls.
- English and Simplified Chinese UI strings.
- No backend service and no account requirement.

## Screens and Flow

1. Open the app and set an administrator password.
2. Enter settings with the administrator password.
3. Select apps for the allowlist.
4. Set SingleAppKiosk as the default home app in Android system settings.
5. Optionally enable Device Admin and Accessibility Service for stronger kiosk behavior.
6. Return to the kiosk home screen and launch only allowlisted apps.

## Requirements

- Android Studio or command-line Gradle.
- JDK 21.
- Android SDK with API level 37 installed.
- Android 7.0 or later on device (`minSdk 24`).

## Build

Clone the repository and build the debug APK:

```bash
./gradlew assembleDebug
```

Build the release APK:

```bash
./gradlew assembleRelease
```

The APK output is generated under:

```text
app/build/outputs/apk/
```

## Install

Install a debug build with ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

After installation, open Android system settings and set SingleAppKiosk as the default home app.

## Permissions

SingleAppKiosk uses privileged Android entry points that require explicit user or administrator approval:

- Default home app: makes the app the launcher shown when pressing Home.
- Device Admin: makes the app harder to uninstall from casual use.
- Accessibility Service: helps detect and block selected System UI areas.

These permissions are not silently enabled. They must be configured on the device by the user or administrator.

## Security Notes

This project provides a lightweight kiosk launcher, not a full enterprise MDM solution. Android behavior differs by device vendor and OS version, especially around System UI, accessibility overlays, and launcher defaults.

For stronger production deployments, test on the exact device model and Android version you plan to use. For managed fleets, consider Android Enterprise, Device Owner mode, or a dedicated MDM solution.

## Release Builds on GitHub

The repository includes a GitHub Actions workflow that builds APKs and uploads them to GitHub Releases.

Create and push a tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions will build the APK and attach it to the release created for that tag.

You can also run the workflow manually from the GitHub Actions tab.

### Optional Release Signing

Without signing secrets, the workflow uploads a debug APK and an unsigned release APK. To publish a signed release APK, add these repository secrets in GitHub:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

Create `ANDROID_KEYSTORE_BASE64` from your keystore file:

```bash
base64 -w 0 release-keystore.jks
```

## Project Structure

```text
app/src/main/java/com/android/launcherkiosk/
  data/        Local settings and allowlist storage
  policy/      Device policy and system status checks
  receiver/    Device Admin receiver
  service/     Accessibility service
  ui/          Home, setup, settings, and allowlist UI
```

## License

No license file is included yet. Add a license before publishing the project for broader open-source use.
