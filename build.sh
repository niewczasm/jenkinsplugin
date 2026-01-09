#!/bin/bash

# Jenkins Host Monitor Plugin Build Script

set -e

echo "=========================================="
echo "Jenkins Host Monitor Plugin Build Script"
echo "=========================================="
echo ""

# Check for Maven
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    echo "Please install Maven 3.8 or later"
    exit 1
fi

# Check for Java
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install JDK 21 or later"
    exit 1
fi

# Display versions
echo "Environment Information:"
echo "------------------------"
java -version 2>&1
echo ""
mvn --version
echo ""

# Ensure we're using the right Java version for Maven
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
echo "Using JAVA_HOME: $JAVA_HOME"
echo ""

# Clean and build
echo "Building plugin..."
echo "------------------------"
mvn clean package -Dmaven.compiler.release=21

# Check if build was successful
if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "Build Successful!"
    echo "=========================================="
    echo ""
    echo "Plugin file location:"
    echo "  $(pwd)/target/host-monitor.hpi"
    echo ""
    echo "Installation Instructions:"
    echo "1. Open Jenkins in your browser"
    echo "2. Go to Manage Jenkins → Manage Plugins"
    echo "3. Click on the 'Advanced' tab"
    echo "4. Under 'Upload Plugin', click 'Choose File'"
    echo "5. Select the .hpi file from the location above"
    echo "6. Click 'Upload'"
    echo "7. Restart Jenkins when prompted"
    echo ""
    echo "After installation, use the 'updateHostStatus' step"
    echo "in your Jenkinsfile to monitor hosts."
    echo ""
else
    echo ""
    echo "=========================================="
    echo "Build Failed!"
    echo "=========================================="
    echo ""
    echo "Please check the error messages above."
    exit 1
fi
