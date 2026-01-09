#!/bin/bash

# Minimal build - skips tests and extra checks
# Use this if the full build is having issues

echo "Running minimal Maven build..."
echo "=============================="
echo ""

mvn clean package \
    -DskipTests \
    -Dmaven.test.skip=true \
    -Dfindbugs.skip=true \
    -Dspotbugs.skip=true \
    -Dmaven.javadoc.skip=true

if [ $? -eq 0 ] && [ -f target/host-monitor.hpi ]; then
    echo ""
    echo "=============================="
    echo "✓ BUILD SUCCESSFUL!"
    echo "=============================="
    echo ""
    echo "Plugin file created:"
    ls -lh target/host-monitor.hpi
    echo ""
    echo "You can now install this .hpi file in Jenkins"
else
    echo ""
    echo "=============================="
    echo "✗ BUILD FAILED"
    echo "=============================="
    echo ""
    echo "Try running with more detail:"
    echo "  mvn clean package -X"
fi
