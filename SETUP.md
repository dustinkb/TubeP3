# TubeP3 — Setup & Installation Guide

**Version:** 0.1  
**Architecture:** Android UI (TubeP3) + Termux Local Execution Engine (yt-dlp & FFmpeg)

---

## 1. Overview

TubeP3 is a lightweight, AMOLED-first Android frontend for downloading YouTube audio in high-quality MP3 format. Rather than bundling heavy binaries inside the APK, TubeP3 delegates execution to a local **Termux** environment via Android's `RUN_COMMAND` Intent API.

- **Frontend (TubeP3 APK):** Simple graphical interface to validate URLs, dispatch background jobs, and report progress.
- **Backend (Termux):** Runs `yt-dlp` and `FFmpeg` to extract the best audio stream, convert it to 320 kbps MP3 with embedded metadata and artwork, and save it directly into the Android device's **Downloads** folder.
- **User Experience:** The user does not need to type terminal commands or keep Termux in the foreground during downloads.

---

## 2. Prerequisites

1. **Android Device:** Android 8.0 (API 26) or newer.
2. **Termux App:** Must be installed from **F-Droid** or **GitHub Releases** (the version on Google Play is deprecated and will not work).
   - [F-Droid Termux Download](https://f-droid.org/packages/com.termux/)
   - [Termux GitHub Releases](https://github.com/termux/termux-app/releases)
3. **TubeP3 APK:** Installed on the same device.

---

## 3. Permissions Setup

### A. Run Command Permission (`com.termux.permission.RUN_COMMAND`)
TubeP3 requires permission to send background commands to Termux:
- When opening TubeP3 for the first time, tap **Check Setup** or **Grant Permission** when prompted.
- Alternatively, on Android: Go to **Settings → Apps → TubeP3 → Permissions** (or **Additional Permissions**) and allow **Run commands in Termux environment**.

### B. Termux Shared Storage Access
Termux must have permission to write files to your device's shared Downloads folder:
- This is automatically granted during the setup command when Termux prompts: *"Allow Termux to access photos, media, and files on your device"*. Tap **Allow**.

---

## 4. One-Step Quick Setup

Open **Termux** on your device, paste the following single command, and press **Enter**:

```bash
pkg update -y && pkg install -y python ffmpeg && (pip install -U yt-dlp || pkg install -y yt-dlp) && termux-setup-storage && mkdir -p ~/.termux/tubep3 && cat << 'EOF' > ~/.termux/tubep3/tubep3-audio.sh
#!/data/data/com.termux/files/usr/bin/bash

# TubeP3 Backend Audio Downloader Script
# Version 0.1

URL="$1"

if [ -z "$URL" ]; then
    echo "ERROR: Missing URL parameter" >&2
    exit 13
fi

# 1. Locate yt-dlp dynamically
YTDLP_BIN=""
if command -v yt-dlp >/dev/null 2>&1; then
    YTDLP_BIN="$(command -v yt-dlp)"
elif [ -x "$PREFIX/bin/yt-dlp" ]; then
    YTDLP_BIN="$PREFIX/bin/yt-dlp"
elif [ -x "$HOME/.local/bin/yt-dlp" ]; then
    YTDLP_BIN="$HOME/.local/bin/yt-dlp"
fi

if [ -z "$YTDLP_BIN" ]; then
    echo "ERROR: yt-dlp executable not found in PATH or standard Termux locations." >&2
    exit 11
fi

# 2. Locate ffmpeg dynamically
FFMPEG_BIN=""
if command -v ffmpeg >/dev/null 2>&1; then
    FFMPEG_BIN="$(command -v ffmpeg)"
elif [ -x "$PREFIX/bin/ffmpeg" ]; then
    FFMPEG_BIN="$PREFIX/bin/ffmpeg"
fi

if [ -z "$FFMPEG_BIN" ]; then
    echo "ERROR: ffmpeg executable not found in PATH or standard Termux locations." >&2
    exit 12
fi

# 3. Locate Android Downloads directory
DOWNLOAD_DIR=""
if [ -d "$HOME/storage/downloads" ] && [ -w "$HOME/storage/downloads" ]; then
    DOWNLOAD_DIR="$HOME/storage/downloads"
elif [ -d "/sdcard/Download" ] && [ -w "/sdcard/Download" ]; then
    DOWNLOAD_DIR="/sdcard/Download"
elif [ -d "/storage/emulated/0/Download" ] && [ -w "/storage/emulated/0/Download" ]; then
    DOWNLOAD_DIR="/storage/emulated/0/Download"
else
    echo "ERROR: Android Downloads folder is not accessible. Run termux-setup-storage." >&2
    exit 10
fi

echo "TUBEP3_STAGE:DOWNLOADING"
echo "TubeP3: Target output: $DOWNLOAD_DIR"

# Execute yt-dlp with required audio conversion flags
"$YTDLP_BIN" \
  --ffmpeg-location "$FFMPEG_BIN" \
  --no-playlist \
  -f bestaudio \
  -x \
  --audio-format mp3 \
  --audio-quality 0 \
  --embed-thumbnail \
  --embed-metadata \
  -P "$DOWNLOAD_DIR" \
  -o "%(title)s.%(ext)s" \
  "$URL"

EXIT_STATUS=$?
if [ $EXIT_STATUS -eq 0 ]; then
    echo "TUBEP3_STAGE:FINISHED"
    echo "TubeP3: Successfully downloaded and converted to MP3 in Downloads."
else
    echo "TUBEP3_STAGE:FAILED"
    echo "TubeP3: Operation failed with exit code $EXIT_STATUS" >&2
fi
exit $EXIT_STATUS
EOF
chmod +x ~/.termux/tubep3/tubep3-audio.sh && mkdir -p ~/.termux && touch ~/.termux/termux.properties && grep -v -E '^[# ]*allow-external-apps[ =]' ~/.termux/termux.properties > ~/.termux/termux.properties.tmp 2>/dev/null || true && echo "allow-external-apps=true" >> ~/.termux/termux.properties.tmp && mv ~/.termux/termux.properties.tmp ~/.termux/termux.properties && (command -v termux-reload-settings >/dev/null 2>&1 && termux-reload-settings || true) && grep -q '^allow-external-apps=true$' ~/.termux/termux.properties && echo "TubeP3: External app execution enabled." && echo "=== TubeP3 backend ready! ==="
```

*(Note: This setup command can also be copied directly from inside TubeP3 by tapping the **Termux Setup** banner at the top of the main screen.)*

---

## 5. What the Setup Command Does

1. **Package Updates & Dependencies:** Updates repository listings and installs `python` and `ffmpeg`.
2. **yt-dlp Installation:** Installs or upgrades `yt-dlp` using `pip` or Termux `pkg`.
3. **Storage Setup:** Runs `termux-setup-storage` to create symlinks in `~/storage/` (specifically `~/storage/downloads`).
4. **Backend Script Installation:** Places `tubep3-audio.sh` in `~/.termux/tubep3/` and marks it executable (`chmod +x`).
5. **External App Execution (`allow-external-apps=true`):**
   - Safely creates or updates `~/.termux/termux.properties`.
   - Ensures `allow-external-apps=true` is enabled without duplicate entries.
   - Reloads Termux configuration via `termux-reload-settings`.
   - Confirms activation by reporting `"TubeP3: External app execution enabled."`.

---

## 6. Verification & Testing

### Verify Termux Configuration
In Termux, check that the property is active:
```bash
grep "allow-external-apps" ~/.termux/termux.properties
```
*Expected output:* `allow-external-apps=true`

### Test Downloader Script Directly in Termux
To test the script independently of the Android app:
```bash
~/.termux/tubep3/tubep3-audio.sh "https://youtu.be/dQw4w9WgXcQ"
```
*Expected outcome:*
- Extracts stream
- Converts to MP3 using FFmpeg
- Saves `<title>.mp3` into your device's `Downloads` folder
- Prints `TUBEP3_STAGE:FINISHED`

---

## 7. Daily Usage

1. Open **TubeP3**.
2. Paste any YouTube video URL into the input field (or tap **Paste**).
3. Tap **Download MP3**.
4. TubeP3 dispatches the command in the background. You do not need to keep Termux open.
5. Once complete, status changes to **Finished** and your MP3 is available in your device's standard **Downloads** folder (accessible via any music player or file manager).

---

## 8. Troubleshooting

| Issue / Error Message | Probable Cause | Resolution |
| :--- | :--- | :--- |
| **"Termux is not installed"** | Termux app missing or installed from incompatible source | Install Termux from F-Droid or GitHub Releases. |
| **"RUN_COMMAND permission denied"** | Android permission not granted | Tap "Grant Permission" in TubeP3 or enable it in Android Settings → Apps → TubeP3 → Permissions. |
| **"Execution of external commands disabled"** | `allow-external-apps` is false or missing | Run: `echo "allow-external-apps=true" >> ~/.termux/termux.properties && termux-reload-settings` |
| **"Android Downloads folder is not accessible"** | Storage permission not granted in Termux | Run `termux-setup-storage` in Termux and accept the Android permission popup. |
| **"yt-dlp executable not found"** | Python/yt-dlp not installed | In Termux run: `pip install -U yt-dlp` or `pkg install yt-dlp`. |
| **"FFmpeg executable not found"** | FFmpeg package missing | In Termux run: `pkg install ffmpeg`. |
| **Network / Extractor Error** | Outdated yt-dlp version or YouTube API change | In Termux run: `pip install -U yt-dlp` to update yt-dlp to the latest version. |
