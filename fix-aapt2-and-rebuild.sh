#!/bin/bash

echo "🔧 Stopping all Gradle daemons..."
./gradlew --stop

echo "🧹 Cleaning Gradle build..."
./gradlew clean

echo "🧹 Removing AAPT2 caches and binaries..."
rm -rf ~/.gradle/caches/transforms-*/transformed/*aapt2*
rm -rf ~/.gradle/caches/*/plugin-resolution/
rm -rf ~/.gradle/caches/modules-2/files-2.1/com.android.tools.build/

echo "🔓 Removing macOS quarantine attributes from aapt2..."
find ~/.gradle/caches/ -type f -name aapt2 -exec xattr -d com.apple.quarantine {} \; 2>/dev/null

echo "🔒 Ensuring AAPT2 is executable..."
find ~/.gradle/caches/ -type f -name aapt2 -exec chmod +x {} \;

echo "🛠️ Rebuilding project with stacktrace enabled..."
./gradlew assembleDebug --stacktrace

echo "✅ Done. Check output above for success or errors."
