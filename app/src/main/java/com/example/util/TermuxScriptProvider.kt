package com.example.util

object TermuxScriptProvider {

    const val SCRIPT_RELATIVE_PATH = ".termux/tubep3/tubep3-audio.sh"
    const val SCRIPT_ABSOLUTE_PATH = "/data/data/com.termux/files/home/$SCRIPT_RELATIVE_PATH"
    const val BASH_ABSOLUTE_PATH = "/data/data/com.termux/files/usr/bin/bash"

    /**
     * The robust backend script installed in Termux.
     */
    val BACKEND_SCRIPT_CONTENT: String = """
        #!/data/data/com.termux/files/usr/bin/bash

        # TubeP3 Backend Audio Downloader Script
        # Version 0.1

        URL="${'$'}1"

        if [ -z "${'$'}URL" ]; then
            echo "ERROR: Missing URL parameter" >&2
            exit 13
        fi

        # 1. Locate yt-dlp dynamically
        YTDLP_BIN=""
        if command -v yt-dlp >/dev/null 2>&1; then
            YTDLP_BIN="${'$'}(command -v yt-dlp)"
        elif [ -x "${'$'}PREFIX/bin/yt-dlp" ]; then
            YTDLP_BIN="${'$'}PREFIX/bin/yt-dlp"
        elif [ -x "${'$'}HOME/.local/bin/yt-dlp" ]; then
            YTDLP_BIN="${'$'}HOME/.local/bin/yt-dlp"
        fi

        if [ -z "${'$'}YTDLP_BIN" ]; then
            echo "ERROR: yt-dlp executable not found in PATH or standard Termux locations." >&2
            exit 11
        fi

        # 2. Locate ffmpeg dynamically
        FFMPEG_BIN=""
        if command -v ffmpeg >/dev/null 2>&1; then
            FFMPEG_BIN="${'$'}(command -v ffmpeg)"
        elif [ -x "${'$'}PREFIX/bin/ffmpeg" ]; then
            FFMPEG_BIN="${'$'}PREFIX/bin/ffmpeg"
        fi

        if [ -z "${'$'}FFMPEG_BIN" ]; then
            echo "ERROR: ffmpeg executable not found in PATH or standard Termux locations." >&2
            exit 12
        fi

        # 3. Locate Android Downloads directory
        DOWNLOAD_DIR=""
        if [ -d "${'$'}HOME/storage/downloads" ] && [ -w "${'$'}HOME/storage/downloads" ]; then
            DOWNLOAD_DIR="${'$'}HOME/storage/downloads"
        elif [ -d "/sdcard/Download" ] && [ -w "/sdcard/Download" ]; then
            DOWNLOAD_DIR="/sdcard/Download"
        elif [ -d "/storage/emulated/0/Download" ] && [ -w "/storage/emulated/0/Download" ]; then
            DOWNLOAD_DIR="/storage/emulated/0/Download"
        else
            echo "ERROR: Android Downloads folder is not accessible. Run termux-setup-storage." >&2
            exit 10
        fi

        echo "TUBEP3_STAGE:DOWNLOADING"
        echo "TubeP3: Target output: ${'$'}DOWNLOAD_DIR"

        # Execute yt-dlp with required audio conversion flags
        "${'$'}YTDLP_BIN" \
          --ffmpeg-location "${'$'}FFMPEG_BIN" \
          --no-playlist \
          -f bestaudio \
          -x \
          --audio-format mp3 \
          --audio-quality 0 \
          --embed-thumbnail \
          --embed-metadata \
          -P "${'$'}DOWNLOAD_DIR" \
          -o "%(title)s.%(ext)s" \
          "${'$'}URL"

        EXIT_STATUS=${'$'}?
        if [ ${'$'}EXIT_STATUS -eq 0 ]; then
            echo "TUBEP3_STAGE:FINISHED"
            echo "TubeP3: Successfully downloaded and converted to MP3 in Downloads."
        else
            echo "TUBEP3_STAGE:FAILED"
            echo "TubeP3: Operation failed with exit code ${'$'}EXIT_STATUS" >&2
        fi
        exit ${'$'}EXIT_STATUS
    """.trimIndent()

    /**
     * Single-line copyable setup command for Termux terminal.
     * Automatically configures allow-external-apps=true idempotently,
     * installs python, ffmpeg, yt-dlp, prepares storage, reloads settings,
     * and installs the TubeP3 backend script.
     */
    val ONE_STEP_SETUP_COMMAND: String = """
pkg update -y && pkg install -y python ffmpeg && (pip install -U yt-dlp || pkg install -y yt-dlp) && termux-setup-storage && mkdir -p ~/.termux/tubep3 && cat << 'EOF' > ~/.termux/tubep3/tubep3-audio.sh
$BACKEND_SCRIPT_CONTENT
EOF
chmod +x ~/.termux/tubep3/tubep3-audio.sh && mkdir -p ~/.termux && touch ~/.termux/termux.properties && grep -v -E '^[# ]*allow-external-apps[ =]' ~/.termux/termux.properties > ~/.termux/termux.properties.tmp 2>/dev/null || true && echo "allow-external-apps=true" >> ~/.termux/termux.properties.tmp && mv ~/.termux/termux.properties.tmp ~/.termux/termux.properties && (command -v termux-reload-settings >/dev/null 2>&1 && termux-reload-settings || true) && grep -q '^allow-external-apps=true$' ~/.termux/termux.properties && echo "TubeP3: External app execution enabled." && echo "=== TubeP3 backend ready! ==="
    """.trimIndent()
}
