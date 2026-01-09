#!/bin/bash

echo "====================================="
echo "Host Monitor Plugin Verification"
echo "====================================="
echo ""

# Find JENKINS_HOME
if [ -z "$JENKINS_HOME" ]; then
    # Try common locations
    for path in /var/lib/jenkins /var/jenkins_home ~/.jenkins; do
        if [ -d "$path" ]; then
            JENKINS_HOME="$path"
            break
        fi
    done
fi

if [ -z "$JENKINS_HOME" ]; then
    echo "❌ JENKINS_HOME not found. Please set it manually:"
    echo "   export JENKINS_HOME=/path/to/jenkins"
    exit 1
fi

echo "JENKINS_HOME: $JENKINS_HOME"
echo ""

# Check plugin file
echo "1. Checking plugin file..."
if [ -f "$JENKINS_HOME/plugins/host-monitor.hpi" ]; then
    echo "   ✅ Plugin file exists"
    ls -lh "$JENKINS_HOME/plugins/host-monitor.hpi"
else
    echo "   ❌ Plugin file NOT found at: $JENKINS_HOME/plugins/host-monitor.hpi"
fi
echo ""

# Check expanded plugin directory
echo "2. Checking expanded plugin..."
if [ -d "$JENKINS_HOME/plugins/host-monitor" ]; then
    echo "   ✅ Plugin expanded directory exists"
    
    # Check for SocketIOConfig in JAR
    JAR_FILE="$JENKINS_HOME/plugins/host-monitor/WEB-INF/lib/host-monitor.jar"
    if [ -f "$JAR_FILE" ]; then
        echo "   ✅ Plugin JAR found"
        
        # Check for SocketIOConfig class and jelly
        if unzip -l "$JAR_FILE" | grep -q "SocketIOConfig.class"; then
            echo "   ✅ SocketIOConfig.class present in JAR"
        else
            echo "   ❌ SocketIOConfig.class NOT found in JAR"
        fi
        
        if unzip -l "$JAR_FILE" | grep -q "SocketIOConfig/global.jelly"; then
            echo "   ✅ SocketIOConfig/global.jelly present in JAR"
        else
            echo "   ❌ SocketIOConfig/global.jelly NOT found in JAR"
        fi
    else
        echo "   ❌ Plugin JAR not found"
    fi
else
    echo "   ⚠️  Plugin not yet expanded (Jenkins may need restart)"
fi
echo ""

# Check plugin version
echo "3. Checking plugin metadata..."
MANIFEST="$JENKINS_HOME/plugins/host-monitor/META-INF/MANIFEST.MF"
if [ -f "$MANIFEST" ]; then
    VERSION=$(grep "Plugin-Version:" "$MANIFEST" | cut -d' ' -f2 | tr -d '\r')
    SHORT_NAME=$(grep "Short-Name:" "$MANIFEST" | cut -d' ' -f2 | tr -d '\r')
    echo "   Plugin Name: $SHORT_NAME"
    echo "   Plugin Version: $VERSION"
    
    if [ "$VERSION" == "1.2" ]; then
        echo "   ✅ Version is correct (1.2)"
    else
        echo "   ⚠️  Expected version 1.2, found: $VERSION"
    fi
else
    echo "   ⚠️  MANIFEST.MF not found (plugin not expanded)"
fi
echo ""

# Check Jenkins logs
echo "4. Checking Jenkins logs..."
LOG_FILE="$JENKINS_HOME/logs/jenkins.log"
if [ -f "$LOG_FILE" ]; then
    echo "   Searching for plugin loading messages..."
    
    if grep -q "Loaded plugin host-monitor" "$LOG_FILE"; then
        echo "   ✅ Plugin loaded successfully"
    else
        echo "   ❌ Plugin load message not found"
    fi
    
    if grep -q "SocketIOConfig indexed" "$LOG_FILE"; then
        echo "   ✅ SocketIOConfig extension registered"
    else
        echo "   ❌ SocketIOConfig extension registration not found"
    fi
    
    # Check for errors
    if grep -i "host-monitor" "$LOG_FILE" | grep -qi "error\|exception"; then
        echo "   ⚠️  Errors found in logs:"
        grep -i "host-monitor" "$LOG_FILE" | grep -i "error\|exception" | tail -5
    fi
else
    echo "   ⚠️  Jenkins log file not found at: $LOG_FILE"
fi
echo ""

# Summary
echo "====================================="
echo "Summary"
echo "====================================="
echo ""
echo "If all checks pass (✅), the Socket.IO configuration should"
echo "appear in: Manage Jenkins → System"
echo ""
echo "If checks fail (❌), follow these steps:"
echo "1. Stop Jenkins completely"
echo "2. Remove old plugin: rm -rf $JENKINS_HOME/plugins/host-monitor*"
echo "3. Copy new plugin: cp target/host-monitor.hpi $JENKINS_HOME/plugins/"
echo "4. Start Jenkins"
echo "5. Wait for full startup, then check Manage Jenkins → System"
echo ""
