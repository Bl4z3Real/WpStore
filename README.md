# Metro Store (Android)

Android store with a Windows Phone "Metro" look: panorama home with parallax title, accent-coloured
tiles that flip in, press feedback, turnstile-style page transitions, Segoe WP Light / Segoe UI Symbol
fonts, and the expandable "..." bottom menu (my apps / downloads / settings).

Built with Kotlin + Jetpack Compose (foundation only, no Material). Min SDK 26, target SDK 34.

## What it does
- **Play Store apps**: catalog entries with `packageName` open the official listing
  (`market://` with fallback to `https://play.google.com/...`). Nothing from the Play Store is re-distributed.
- **My apps**: lists only apps that are really installed on the device (catalog apps, yours included, plus
  anything installed through the store). Tap to open; menu > *uninstall selected* removes one.
- **APK from the phone**: *my apps* > round button > pick an `.apk`. It is validated (parsed by
  `PackageManager`) and installed through the **system installer** (`ACTION_VIEW` + `FileProvider`);
  it shows up in *my apps* once Android has installed it.
- **Authorised APK sources**: a catalog entry with an `apkUrl` (https only) gets an *install* button that
  downloads the file and hands it to the system installer. Use only URLs you are allowed to distribute.
- **Permission**: if "install unknown apps" is not allowed for this app, the user is sent to the
  official Android settings screen, then taps *install* again. No protection is bypassed.
- **Errors** (shown in a Metro toast): invalid APK, unreadable file, failed download, missing permission,
  no installer available.

## Add your own apps
1. Copy your `.apk` into `app/src/main/assets/apks/`.
2. Add an entry to `app/src/main/assets/my_apps.json` (see `_example` in that file):
```json
{ "apps": [
  { "id": "my-first-app", "name": "My First App", "publisher": "Me",
    "description": "...", "version": "1.0", "color": "#1BA1E2",
    "apkAsset": "my-first-app.apk", "featured": true }
] }
```
3. Rebuild (or push to GitHub). The app appears on the home page, in categories ("my apps" by default),
   in search, and has an
   **install** button that installs the bundled APK through the system installer.
Instead of `apkAsset` you can use `apkUrl` (https) to download the APK from a link you control.
Same fields as the catalog below.

## Edit the catalog
`app/src/main/assets/catalog.json`

```json
{ "id": "my-app", "name": "My App", "publisher": "Me", "description": "...",
  "category": "tools + productivity", "version": "1.2", "size": "12 MB",
  "packageName": "com.example.myapp",
  "apkUrl": "https://example.com/my-app.apk",
  "iconUrl": "https://example.com/icon.png",
  "price": "free", "rating": 4.5, "color": "#1E9E1E",
  "featured": true, "new": false }
```
`packageName`, `apkUrl`, `iconUrl`, `version`, `size`, `rating` are optional.
Categories are created automatically from the `category` field.

## Build locally
Requirements: JDK 17, Android SDK (platform 34), Gradle 8.7+ (or Android Studio).

```bash
# once, optional: create the wrapper
gradle wrapper --gradle-version 8.7
# build
./gradlew assembleDebug          # or: gradle assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`. With Android Studio just open the folder and press Run.

## Build with GitHub Actions
1. Push the project to a GitHub repository (branch `main` or `master`).
2. The workflow `.github/workflows/build.yml` runs on every push / pull request, or manually from
   *Actions > Build APK > Run workflow*.
3. Download the APK from the run page, under **Artifacts > MetroStore-debug-apk**.

The workflow installs JDK 17 and the Android SDK and uses `gradle/actions/setup-gradle` with Gradle 8.7,
so no Gradle wrapper is required in the repo.

## Structure
```
app/src/main/java/com/metrostore/app/
  MainActivity.kt, StoreViewModel.kt
  data/  Models.kt (catalog + filters)  LocalStore.kt  ApkInstaller.kt (+ Downloader)
  ui/    Theme.kt (fonts, colours, type scale)  Components.kt (tiles, bars, glyphs, motion)
         MetroApp.kt (navigation)  HomeScreen / ListingScreen / DetailScreen / MyAppsScreen / OtherScreens
app/src/main/res/font/   segoe_wp_light.ttf, seguisym.ttf
```
