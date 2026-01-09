#!/bin/bash

echo "Running full Maven build with detailed output..."
echo "================================================="
echo ""

# Run the full package goal and capture output
mvn clean package -e 2>&1 | tee build-output.log

echo ""
echo "================================================="
echo "Build log saved to: build-output.log"
echo ""

# Check for success
if [ -f target/host-monitor.hpi ]; then
    echo "✓ SUCCESS! Plugin built successfully!"
    echo "  File: target/host-monitor.hpi"
    ls -lh target/host-monitor.hpi
else
    echo "✗ BUILD FAILED"
    echo ""
    echo "Checking for errors in output..."
    echo ""
    grep -i "error" build-output.log | head -20
    echo ""
    echo "Full log is in build-output.log"
fi
