# Publishing BiteWise on F-Droid

F-Droid only publishes open-source Android apps that it builds itself from your source code. It never accepts an APK you upload. Submitting means adding one small metadata file to F-Droid's `fdroiddata` repository on GitLab through a merge request. A volunteer reviews it, and F-Droid's build server then builds BiteWise from this GitHub repository.

## Already done in this repository

- [x] **Open-source license:** `LICENSE` (GPL-3.0-or-later).
- [x] **Android app:** `android/` is a small WebView app that bundles `index.html` and its files, so it works offline. It has no Google or other non-free libraries. It was tested on an Android 15 emulator without Google Play Services: launch, barcode lookup, camera permission, saving data across restarts, export and import of backups, the back button, and dark mode.
- [x] **No CDN or Google Fonts:** the barcode library and the font are in `vendor/`. F-Droid reviewers flag apps that load code or fonts from third-party servers.
- [x] **Store listing:** `fastlane/metadata/android/en-US/` holds the title, short and full description, icon, 5 screenshots and the changelog for version 25.
- [x] **Version:** versionCode 25 / versionName "25", the same number shown in Settings.
- [x] **CI build:** `.github/workflows/android.yml` builds the APK on every push.
- [x] **Metadata file for F-Droid:** [`io.github.sumitsingh34.bitewise.yml`](io.github.sumitsingh34.bitewise.yml).

> The app ID `io.github.sumitsingh34.bitewise` is permanent once published. Changing it later means a new app, and users would have to reinstall.

## Step 1: Push to GitHub and check the build

1. Commit everything and push to `main` on GitHub.
2. On GitHub, open **Actions → Android build** and check that it passes.
3. Optional: download the `bitewise-debug-apk` artifact from that run and install it on your phone to try it.

This also updates the web version on GitHub Pages to version 25, which now uses the bundled font and scanner.

## Step 2: Tag the release

F-Droid builds tagged commits only. The tag must match the version:

```
git tag v25
git push origin v25
```

Then get the full commit hash of the tag. The F-Droid file needs the hash, not the tag name:

```
git rev-list -n 1 v25
```

Put that 40-character hash in the `commit:` line of `docs/fdroid/io.github.sumitsingh34.bitewise.yml`.

## Step 3: Create a GitLab account

Sign up at <https://gitlab.com/users/sign_up> if you don't have an account. The name you use is shown on your merge request.

If GitLab asks for a phone number or credit card to run pipelines, skip that. Mention it in the merge request instead, and the F-Droid team will run the pipelines for you.

## Step 4: Fork fdroiddata and add the file

1. Open <https://gitlab.com/fdroid/fdroiddata> and click **Fork**. Keep the fork **public**.
2. Clone your fork (it's large, so use a shallow clone) and make a branch named after the app ID:

   ```
   git clone --depth=1 https://gitlab.com/YOUR-GITLAB-NAME/fdroiddata.git
   cd fdroiddata
   git checkout -b io.github.sumitsingh34.bitewise
   ```

3. Copy `docs/fdroid/io.github.sumitsingh34.bitewise.yml` from this repository to `metadata/io.github.sumitsingh34.bitewise.yml` in fdroiddata. The file needs LF line endings. The copy in this repository already has them.
4. Commit and push:

   ```
   git add metadata/io.github.sumitsingh34.bitewise.yml
   git commit -m "New app: BiteWise"
   git push origin io.github.sumitsingh34.bitewise
   ```

## Step 5: Check the pipelines on your fork

Pushing starts GitLab CI on your fork. It runs `fdroid lint`, `fdroid rewritemeta`, `fdroid checkupdates` and a full `fdroid build` of BiteWise. Open **Build → Pipelines** in your fork and wait until every job is green (takes about 10–30 minutes).

- If `rewritemeta` fails, it shows the formatting it expects. Copy that into the file, commit and push again.
- If `build` fails, open the job log, find the first error, and fix it in this repository or in the metadata file. If you change this repository, it needs a new tag (v26) and a new hash.

## Step 6: Open the merge request

1. On your fork, click **Create merge request** for your branch. Target `fdroid/fdroiddata`, branch `master`.
2. Title: `New app: BiteWise`.
3. Pick the **App inclusion** template and work through its checklist. Text you can paste for the first section:

   > **About the app:** BiteWise is a personal calorie and nutrition tracker for meals, exercise, water and weight. You can scan barcodes (Open Food Facts), search plain foods (USDA FoodData Central), build home recipes, see where calories come from, and keep weight and calorie history. It has no account, no ads and no tracking, and all data stays on the phone (localStorage inside the app, with JSON export and import). Screenshots are in `fastlane/metadata/android/en-US/images/phoneScreenshots/`.
   >
   > **Why:** I wanted a simple tracker with no account or cloud sync, that also does home recipes and shows where calories come from for each meal.
   >
   > **Technical notes:** The app is a web app (`index.html`, also served on GitHub Pages) shown in a plain Android WebView (`android/`). All web files, including the vendored html5-qrcode library (Apache-2.0, from npm `html5-qrcode@2.3.8`) and the Archivo font (OFL-1.1, from `@fontsource-variable/archivo`), are bundled in the APK at build time. Nothing is loaded from a CDN. The WebView's built-in BarcodeDetector is turned off because it needs Google Play Services. The app has no dependencies besides the Android Gradle Plugin.
   >
   > **Reproducible builds:** not enabled for now. BiteWise is only distributed through F-Droid, so F-Droid signing is fine.

4. Submit, then reply to reviewer comments promptly. Reviewers may:
   - add the **NonFreeNet** anti-feature, because food search uses USDA's API. That's normal and doesn't block inclusion.
   - ask about the minified `html5-qrcode.min.js`. Point them to its npm package and GitHub source (Apache-2.0).
   - change the metadata themselves before merging.

## Step 7: After it's merged

F-Droid's build server builds and signs the app in batches. BiteWise usually appears 1–3 days after the merge (sometimes longer) at:

<https://f-droid.org/packages/io.github.sumitsingh34.bitewise/>

If it doesn't appear, check <https://monitor.f-droid.org/builds> for build errors.

## Releasing updates later

You don't need another merge request for updates. With `AutoUpdateMode: Version` and `UpdateCheckMode: Tags`, F-Droid checks this GitHub repository for new tags and builds them by itself. See **Releasing a new Android version** in the main [README](../../README.md): bump the version in the three places, add a changelog file, commit, tag `vN`, and push the tag.

## Optional: reproducible builds (decide before Step 6)

By default F-Droid signs the APK with its own key. This **can't be changed later**. If you might ever also offer the APK on GitHub Releases or Google Play, and want people to be able to switch between those and F-Droid without reinstalling, set up reproducible builds now:

1. Create a release signing key: `keytool -genkey -v -keystore bitewise-release.jks -alias bitewise -keyalg RSA -keysize 4096 -validity 10000`. Back it up safely. Losing it means you can never update the app again.
2. For each release, build `assembleRelease` from the tagged commit, sign it with `apksigner`, and attach it to a GitHub Release named `v25`.
3. Add these to the metadata: `binary: https://github.com/sumitsingh34/bitewise/releases/download/v%v/bitewise-%v.apk` in the build block, and `AllowedAPKSigningKeys: <sha256 of your certificate>` (from `apksigner verify --print-certs`).

F-Droid then builds the APK itself, checks that it matches yours byte for byte, and publishes your signed copy. The `.gitattributes` file in this repository keeps line endings the same on Windows and Linux, which this check needs.
