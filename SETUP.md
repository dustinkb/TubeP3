# TubeP3 — Setup & Installation Guide

**Version:** 0.2  
**Architecture:** Android UI (TubeP3) + Termux Local Execution Engine (yt-dlp & FFmpeg)

---

## 1. Overview

TubeP3 is a lightweight, AMOLED-first Android frontend for downloading YouTube audio in high-quality MP3 format. Rather than bundling heavy binaries inside the APK, TubeP3 delegates execution to a local **Termux** environment via Android's `RUN_COMMAND` Intent API.

- **Frontend (TubeP3 APK):** Graphical interface to validate URLs, dispatch background jobs, and monitor execution.
- **Backend (Termux):** Runs `yt-dlp` and `FFmpeg` to extract the best audio stream, convert it to 320 kbps MP3 with embedded metadata and artwork, and save it directly into the Android device's **Downloads** folder.
- **Setup & Repair Tool:** An idempotent 9-stage installer and diagnostic repair script that automates package installation, executable verification (`ffmpeg -version`, `yt-dlp --version`), storage checks, and non-destructive backend self-tests.

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
- This is automatically configured during the setup command when Termux prompts: *"Allow Termux to access photos, media, and files on your device"*. Tap **Allow**.

---

## 4. One-Step Quick Setup & Repair Command (v0.2)

Open **Termux** on your device, paste the following single command, and press **Enter**:

```bash
mkdir -p ~/.termux/tubep3 && cat << 'TUBEP3_SETUP_EOF' > ~/.termux/tubep3/setup-repair.sh
#!/data/data/com.termux/files/usr/bin/bash

# TubeP3 Setup & Repair Tool (Version 0.2)
# Safe, idempotent installer and repair tool for Termux

echo "TUBEP3_SETUP:STARTING"
echo "=== TubeP3 Setup & Repair Tool (v0.2) ==="

# 1. Package Updates & Environment Repair
echo "TUBEP3_SETUP:UPDATING"
echo "-> Checking and repairing package environment..."
export DEBIAN_FRONTEND=noninteractive

# Resolve any interrupted package installations non-destructively
dpkg --configure -a 2>/dev/null || true

# Update package lists
pkg update -y 2>&1 || apt-get update -y 2>&1 || true

# Ensure python is available
if ! command -v python3 >/dev/null 2>&1 && ! command -v python >/dev/null 2>&1; then
    echo "Installing python..."
    pkg install -y python 2>&1 || apt-get install -y python 2>&1 || true
fi

if command -v python3 >/dev/null 2>&1 || command -v python >/dev/null 2>&1; then
    echo "✓ Python package verified."
else
    echo "TUBEP3_SETUP:FAILED"
    echo "Component: Python"
    echo "Reason: Python executable could not be found or installed."
    echo "Suggested action: Run 'pkg install -y python' in Termux."
    exit 1
fi

# 2. Storage Setup & Verification
echo "TUBEP3_SETUP:STORAGE"
echo "-> Verifying Android shared storage access..."

check_downloads_dir() {
    if [ -d "$HOME/storage/downloads" ] && [ -w "$HOME/storage/downloads" ]; then
        return 0
    elif [ -d "/sdcard/Download" ] && [ -w "/sdcard/Download" ]; then
        return 0
    elif [ -d "/storage/emulated/0/Download" ] && [ -w "/storage/emulated/0/Download" ]; then
        return 0
    fi
    return 1
}

if ! check_downloads_dir; then
    echo "Configuring Termux storage..."
    if command -v termux-setup-storage >/dev/null 2>&1; then
        termux-setup-storage || true
        sleep 2
    fi
    if [ -d "/sdcard/Download" ]; then
        mkdir -p "$HOME/storage"
        ln -s /sdcard/Download "$HOME/storage/downloads" 2>/dev/null || true
    fi
fi

if check_downloads_dir; then
    echo "✓ Storage verified: Android Downloads directory is accessible and writable."
else
    echo "TUBEP3_SETUP:FAILED"
    echo "Component: Android Storage"
    echo "Reason: Android Downloads directory is not accessible or writable."
    echo "Suggested action: Grant Termux storage permission when prompted, or run 'termux-setup-storage' and tap 'Allow'."
    exit 1
fi

# 3. FFmpeg Installation & Functional Verification
echo "TUBEP3_SETUP:FFMPEG"
echo "-> Verifying FFmpeg installation..."

FFMPEG_OUT="$(ffmpeg -version 2>&1)"
FFMPEG_EXIT=$?

if [ $FFMPEG_EXIT -ne 0 ]; then
    echo "Installing/repairing ffmpeg..."
    pkg install -y ffmpeg 2>&1 || apt-get install -y ffmpeg 2>&1 || true
    FFMPEG_OUT="$(ffmpeg -version 2>&1)"
    FFMPEG_EXIT=$?
fi

if [ $FFMPEG_EXIT -eq 0 ]; then
    FFMPEG_VER="$(echo "$FFMPEG_OUT" | head -n 1)"
    echo "✓ FFmpeg verified: $FFMPEG_VER"
else
    echo "TUBEP3_SETUP:FAILED"
    echo "Component: FFmpeg"
    echo "Reason: FFmpeg executable failed to run (exit code $FFMPEG_EXIT)."
    echo "Diagnostic output:"
    echo "$FFMPEG_OUT" | head -n 5
    echo "Suggested action: Run 'pkg upgrade -y' to repair broken Termux libraries, then re-run setup."
    exit 1
fi

# 4. yt-dlp Installation & Functional Verification
echo "TUBEP3_SETUP:YTDLP"
echo "-> Verifying yt-dlp installation..."

find_ytdlp() {
    if command -v yt-dlp >/dev/null 2>&1; then
        command -v yt-dlp
    elif [ -x "$PREFIX/bin/yt-dlp" ]; then
        echo "$PREFIX/bin/yt-dlp"
    elif [ -x "$HOME/.local/bin/yt-dlp" ]; then
        echo "$HOME/.local/bin/yt-dlp"
    fi
}

YTDLP_BIN="$(find_ytdlp)"
YTDLP_EXIT=1
if [ -n "$YTDLP_BIN" ]; then
    YTDLP_OUT="$("$YTDLP_BIN" --version 2>&1)"
    YTDLP_EXIT=$?
fi

if [ $YTDLP_EXIT -ne 0 ]; then
    echo "Installing/updating yt-dlp..."
    (pip install -U --no-cache-dir yt-dlp 2>&1 || pip install -U yt-dlp 2>&1 || pkg install -y yt-dlp 2>&1) || true
    YTDLP_BIN="$(find_ytdlp)"
    if [ -n "$YTDLP_BIN" ]; then
        YTDLP_OUT="$("$YTDLP_BIN" --version 2>&1)"
        YTDLP_EXIT=$?
    else
        YTDLP_EXIT=127
        YTDLP_OUT="yt-dlp executable not found after install attempt"
    fi
fi

if [ $YTDLP_EXIT -eq 0 ]; then
    echo "✓ yt-dlp verified: version $YTDLP_OUT"
else
    echo "TUBEP3_SETUP:FAILED"
    echo "Component: yt-dlp"
    echo "Reason: yt-dlp executable failed to run (exit code $YTDLP_EXIT)."
    echo "Diagnostic output:"
    echo "$YTDLP_OUT" | head -n 5
    echo "Suggested action: Run 'pip install -U yt-dlp' or 'pkg install -y yt-dlp' in Termux."
    exit 1
fi

# 5. Backend Script Installation
echo "TUBEP3_SETUP:BACKEND"
echo "-> Installing TubeP3 backend script..."

mkdir -p "$HOME/.termux/tubep3"
cat << 'TUBEP3_AUDIO_EOF' > "$HOME/.termux/tubep3/tubep3-audio.sh"
#!/data/data/com.termux/files/usr/bin/bash

# TubeP3 Backend Audio Downloader Script
# Version 0.2

# 0. Handle non-destructive self-test mode
if [ "$1" = "--self-test" ] || [ "$1" = "--test" ]; then
    echo "TUBEP3_STAGE:SELF_TEST"
    # 1. Check yt-dlp
    YTDLP_CHECK=""
    if command -v yt-dlp >/dev/null 2>&1; then
        YTDLP_CHECK="$(command -v yt-dlp)"
    elif [ -x "$PREFIX/bin/yt-dlp" ]; then
        YTDLP_CHECK="$PREFIX/bin/yt-dlp"
    elif [ -x "$HOME/.local/bin/yt-dlp" ]; then
        YTDLP_CHECK="$HOME/.local/bin/yt-dlp"
    fi
    if [ -z "$YTDLP_CHECK" ]; then
        echo "ERROR: yt-dlp executable not found in PATH or standard Termux locations." >&2
        exit 11
    fi

    # 2. Check ffmpeg
    FFMPEG_CHECK=""
    if command -v ffmpeg >/dev/null 2>&1; then
        FFMPEG_CHECK="$(command -v ffmpeg)"
    elif [ -x "$PREFIX/bin/ffmpeg" ]; then
        FFMPEG_CHECK="$PREFIX/bin/ffmpeg"
    fi
    if [ -z "$FFMPEG_CHECK" ]; then
        echo "ERROR: ffmpeg executable not found in PATH or standard Termux locations." >&2
        exit 12
    fi

    # 3. Check storage
    if [ -d "$HOME/storage/downloads" ] && [ -w "$HOME/storage/downloads" ]; then
        :
    elif [ -d "/sdcard/Download" ] && [ -w "/sdcard/Download" ]; then
        :
    elif [ -d "/storage/emulated/0/Download" ] && [ -w "/storage/emulated/0/Download" ]; then
        :
    else
        echo "ERROR: Android Downloads folder is not accessible. Run termux-setup-storage." >&2
        exit 10
    fi

    echo "TUBEP3_STAGE:SELF_TEST_OK"
    echo "TubeP3: Backend dependencies and storage verified successfully."
    exit 0
fi

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
TUBEP3_AUDIO_EOF

chmod +x "$HOME/.termux/tubep3/tubep3-audio.sh"

if [ -f "$HOME/.termux/tubep3/tubep3-audio.sh" ] && [ -x "$HOME/.termux/tubep3/tubep3-audio.sh" ]; then
    echo "✓ Backend script installed: ~/.termux/tubep3/tubep3-audio.sh"
else
    echo "TUBEP3_SETUP:FAILED"
    echo "Component: TubeP3 Backend"
    echo "Reason: Backend script could not be created or set as executable."
    echo "Suggested action: Check file permissions in ~/.termux/tubep3/."
    exit 1
fi

# 6. External Command Configuration (Idempotent)
echo "TUBEP3_SETUP:EXTERNAL"
echo "-> Configuring allow-external-apps in ~/.termux/termux.properties..."

mkdir -p "$HOME/.termux"
touch "$HOME/.termux/termux.properties"

# Remove existing active or commented entries to prevent duplicate accumulation
grep -v -E '^[# ]*allow-external-apps[ =]' "$HOME/.termux/termux.properties" > "$HOME/.termux/termux.properties.tmp" 2>/dev/null || true
echo "allow-external-apps=true" >> "$HOME/.termux/termux.properties.tmp"
mv "$HOME/.termux/termux.properties.tmp" "$HOME/.termux/termux.properties"

if grep -q '^allow-external-apps=true$' "$HOME/.termux/termux.properties"; then
    echo "TubeP3: External app execution enabled."
else
    echo "TUBEP3_SETUP:FAILED"
    echo "Component: External Command Configuration"
    echo "Reason: Failed to configure allow-external-apps=true in ~/.termux/termux.properties."
    echo "Suggested action: Check permissions on ~/.termux/termux.properties."
    exit 1
fi

if command -v termux-reload-settings >/dev/null 2>&1; then
    termux-reload-settings || true
    echo "✓ Termux settings reloaded."
else
    echo "Notice: termux-reload-settings not available. Please restart the Termux app once to apply settings."
fi

# 7. Backend Self-Test Verification
echo "TUBEP3_SETUP:VERIFYING"
echo "-> Running TubeP3 backend self-test..."

SELF_TEST_OUT="$("$HOME/.termux/tubep3/tubep3-audio.sh" --self-test 2>&1)"
SELF_TEST_STATUS=$?

if [ $SELF_TEST_STATUS -eq 0 ]; then
    echo "✓ TubeP3 backend self-test passed successfully."
else
    echo "TUBEP3_SETUP:FAILED"
    echo "Component: Backend Self-Test"
    echo "Reason: Backend script self-test failed (exit code $SELF_TEST_STATUS)."
    echo "Diagnostic output:"
    echo "$SELF_TEST_OUT"
    echo "Suggested action: Check the diagnostic error above to resolve missing tools."
    exit 1
fi

# 8. All Checks Succeeded
echo "TUBEP3_SETUP:READY"
echo ""
echo "=== TubeP3 backend ready! ==="
echo "✓ Termux packages"
echo "✓ FFmpeg"
echo "✓ yt-dlp"
echo "✓ Android storage"
echo "✓ TubeP3 backend"
echo "✓ External command execution"

TUBEP3_SETUP_EOF
chmod +x ~/.termux/tubep3/setup-repair.sh && bash ~/.termux/tubep3/setup-repair.sh
```

*(Note: This setup command can also be copied directly from inside TubeP3 by tapping **Check Setup** on the main screen.)*

---

## 5. What the Setup & Repair Script Does

1. **Package Environment Repair (`TUBEP3_SETUP:UPDATING`):** Runs `dpkg --configure -a` non-destructively to repair any interrupted package transactions, updates repositories, and ensures Python is functional.
2. **Storage Verification (`TUBEP3_SETUP:STORAGE`):** Checks read/write access to Android shared Downloads (`$HOME/storage/downloads`, `/sdcard/Download`, `/storage/emulated/0/Download`). Runs `termux-setup-storage` if needed.
3. **FFmpeg Verification (`TUBEP3_SETUP:FFMPEG`):** Verifies that `ffmpeg -version` actually runs and exits with status 0. If missing or damaged, installs or upgrades FFmpeg. If execution fails, aborts with specific diagnostic details.
4. **yt-dlp Verification (`TUBEP3_SETUP:YTDLP`):** Verifies that `yt-dlp --version` actually runs and exits with status 0. If missing or broken, installs or upgrades via `pip` or Termux `pkg`.
5. **Backend Script Installation (`TUBEP3_SETUP:BACKEND`):** Writes `~/.termux/tubep3/tubep3-audio.sh`, applies execute permissions (`chmod +x`), and verifies filesystem status.
6. **External Command Configuration (`TUBEP3_SETUP:EXTERNAL`):** Safely edits `~/.termux/termux.properties` to ensure exactly one active `allow-external-apps=true` entry (no duplicates accumulated on repeated runs), and reloads settings.
7. **Backend Self-Test (`TUBEP3_SETUP:VERIFYING`):** Executes `tubep3-audio.sh --self-test` to test dependency resolution in the actual script environment without downloading or writing files.
8. **Confirmation (`TUBEP3_SETUP:READY`):** Emits completion signal and summary checkmarks.

---

## 6. How to Re-Run the Repair Tool in Termux

Because the script is saved at `~/.termux/tubep3/setup-repair.sh`, you can re-run diagnostics or repair at any time by simply entering:

```bash
bash ~/.termux/tubep3/setup-repair.sh
```

---

## 7. Troubleshooting

| Stage / Failure | Cause | Non-Destructive Resolution |
| :--- | :--- | :--- |
| **`TUBEP3_SETUP:FAILED` (Component: Android Storage)** | Storage permission not granted in Termux | Run `termux-setup-storage` in Termux and tap **Allow**, or grant Storage in Android Settings → Apps → Termux. |
| **`TUBEP3_SETUP:FAILED` (Component: FFmpeg)** | Broken Termux package or missing library dependency | In Termux run: `pkg upgrade -y` followed by `pkg install -y ffmpeg`. |
| **`TUBEP3_SETUP:FAILED` (Component: yt-dlp)** | yt-dlp missing or incomplete python environment | In Termux run: `pip install -U yt-dlp` or `pkg install -y yt-dlp`. |
| **`TUBEP3_SETUP:FAILED` (Component: Backend Self-Test)** | One of the executables or storage paths failed during test | Review the diagnostic output printed above the failure marker. |
| **`TUBEP3_SETUP:FAILED` (Component: External Command Configuration)** | Permission issue on `~/.termux/termux.properties` | Run `chmod 644 ~/.termux/termux.properties` in Termux. |
