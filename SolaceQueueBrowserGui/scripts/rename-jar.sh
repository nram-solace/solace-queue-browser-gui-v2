#!/bin/bash
# Rename JAR file to use version from system.json
# This script waits for the JAR file to be created, then renames it

SOURCE_JAR="$1"
TARGET_JAR="$2"

# Wait for source JAR to exist (max 10 seconds, check every 0.2 seconds)
for i in {1..50}; do
    if [ -f "$SOURCE_JAR" ]; then
        # Additional check: make sure file is not still being written (size stable)
        SIZE1=$(stat -f%z "$SOURCE_JAR" 2>/dev/null || stat -c%s "$SOURCE_JAR" 2>/dev/null || echo "0")
        sleep 0.2
        SIZE2=$(stat -f%z "$SOURCE_JAR" 2>/dev/null || stat -c%s "$SOURCE_JAR" 2>/dev/null || echo "0")
        if [ "$SIZE1" = "$SIZE2" ] && [ "$SIZE1" != "0" ]; then
            cp "$SOURCE_JAR" "$TARGET_JAR"
            echo "Renamed JAR to: $(basename "$TARGET_JAR")"
            exit 0
        fi
    fi
    sleep 0.2
done

echo "ERROR: JAR file not found or not ready: $SOURCE_JAR"
exit 1

