#!/bin/bash

# SolaceQueueBrowserGui Distribution Build Script
# Builds a clean runtime distribution package without source code or development files

set -e  # Exit on any error

echo "=================================================="
echo "Building SolaceQueueBrowserGui Runtime Distribution"
echo "=================================================="

# Change to the project directory (parent of scripts/)
cd "$(dirname "$0")/.."

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven is not installed or not in PATH"
    echo "Please install Maven: https://maven.apache.org/install.html"
    exit 1
fi

# Clean and compile
echo "🧹 Cleaning previous builds..."
mvn clean

echo "🔨 Building application and distribution package..."
mvn compile package

# Check if build was successful
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Build completed successfully!"
    echo ""
    
    # Find the distribution archives (version is read from system.json)
    DIST_DIR="target"
    ZIP_FILE=$(ls ${DIST_DIR}/SolaceQueueBrowserGui-*-runtime-distribution.zip 2>/dev/null | head -n 1)
    TAR_FILE=$(ls ${DIST_DIR}/SolaceQueueBrowserGui-*-runtime-distribution.tar.gz 2>/dev/null | head -n 1)
    
    if [ -n "$ZIP_FILE" ]; then
        echo "📦 Distribution packages created:"
        ls -lh "$ZIP_FILE"
        if [ -n "$TAR_FILE" ]; then
            ls -lh "$TAR_FILE"
        fi
        echo ""
        echo "Distribution package contents:"
        echo "  - Application JAR (with all dependencies)"
        echo "  - Required config files (system.json, log4j2.properties, icons)"
        echo "  - Runtime scripts (run.sh, encrypt-password.sh, crypt-util.sh)"
        echo "  - Documentation (README.md, USER_GUIDE.md)"
        echo ""
        echo "To extract and use:"
        echo "  unzip $ZIP_FILE"
        echo "  cd SolaceQueueBrowserGui-*/"
        echo "  ./scripts/run.sh -c config/default.json"
        echo ""
    else
        echo "⚠️  Warning: Distribution package not found. Checking build output..."
        ls -la ${DIST_DIR}/*.zip ${DIST_DIR}/*.tar.gz 2>/dev/null || echo "  No distribution archives found"
    fi
else
    echo "❌ Build failed!"
    exit 1
fi

