#!/bin/bash
# Extract version from system.json
# This script is used by Maven to read the application version

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ ! -f "$PROJECT_DIR/config/system.json" ]; then
    echo "Error: config/system.json not found" >&2
    exit 1
fi

# Extract version using grep and sed, trim whitespace
VERSION=$(grep -o '"version"[[:space:]]*:[[:space:]]*"[^"]*"' "$PROJECT_DIR/config/system.json" | sed 's/.*"\(.*\)".*/\1/' | head -1 | tr -d '[:space:]')
echo -n "$VERSION"

