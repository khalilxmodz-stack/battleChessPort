# ChessBattlePort by KhalilXmodz (Chess++ Android port)

Android port of the Java Swing game by Abdullah Wali and Mert Ege Can.
Game rules/pieces/abilities are reused as-is; the Swing UI was rewritten as a native Android UI.

## Build with GitHub Actions
1. Push this whole folder to a GitHub repo (keep `.github/workflows/android.yml` at that exact path).
2. Open the repo's Actions tab. The "Build Android APK" workflow runs on every push (or press "Run workflow").
3. When it finishes, open the run and download the `ChessPlusPlus-debug-apk` artifact, unzip it, and install the APK.
