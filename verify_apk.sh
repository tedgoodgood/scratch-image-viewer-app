#!/bin/bash

# APK Verification Script
# This script verifies the generated APK files

echo "=== APK Verification Script ==="
echo

# Check if APK files exist
RELEASE_APK="app/build/outputs/apk/release/app-release.apk"
DEBUG_APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$RELEASE_APK" ]; then
    echo "❌ Release APK not found: $RELEASE_APK"
    exit 1
fi

if [ ! -f "$DEBUG_APK" ]; then
    echo "❌ Debug APK not found: $DEBUG_APK"
    exit 1
fi

echo "✅ APK files found"
echo

# Check APK sizes
RELEASE_SIZE=$(stat -c%s "$RELEASE_APK")
DEBUG_SIZE=$(stat -c%s "$DEBUG_APK")

echo "📏 APK Sizes:"
echo "  Release: $(numfmt --to=iec $RELEASE_SIZE) ($RELEASE_SIZE bytes)"
echo "  Debug:   $(numfmt --to=iec $DEBUG_SIZE) ($DEBUG_SIZE bytes)"
echo

# Verify APK signatures
echo "🔐 Verifying APK signatures..."

# Add build-tools to PATH
export PATH=$PATH:$ANDROID_HOME/build-tools/34.0.0

# Check release APK signature
if apksigner verify -v "$RELEASE_APK" > /dev/null 2>&1; then
    echo "  ✅ Release APK is properly signed"
else
    echo "  ❌ Release APK signature verification failed"
fi

# Check debug APK signature
if apksigner verify -v "$DEBUG_APK" > /dev/null 2>&1; then
    echo "  ✅ Debug APK is properly signed"
else
    echo "  ❌ Debug APK signature verification failed"
fi

echo

# Show APK package info
echo "📦 Package Information:"
aapt dump badging "$RELEASE_APK" | grep -E "package:|application:|launchable-activity:|uses-sdk:" | sed 's/^/  /'

echo
echo "✅ APK verification complete!"