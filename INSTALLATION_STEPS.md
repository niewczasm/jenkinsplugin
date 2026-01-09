# Socket.IO Configuration Not Showing - Installation Fix

## Problem
The Socket.IO configuration section doesn't appear in Jenkins System settings after installing the plugin.

## Root Cause
This usually happens when:
1. Jenkins wasn't restarted after plugin installation
2. Old plugin version is cached
3. Plugin wasn't properly uninstalled before upgrading

## Solution: Proper Installation Steps

### Step 1: Stop Jenkins
```bash
# Stop Jenkins (method depends on your setup)
# Ubuntu/Debian with systemd:
sudo systemctl stop jenkins

# Or if running manually:
# Find Jenkins PID and kill it
ps aux | grep jenkins
kill <PID>
```

### Step 2: Remove Old Plugin Completely
```bash
# Remove old plugin files
rm -f $JENKINS_HOME/plugins/host-monitor.hpi
rm -f $JENKINS_HOME/plugins/host-monitor.jpi
rm -rf $JENKINS_HOME/plugins/host-monitor/

# Clear plugin cache
rm -rf $JENKINS_HOME/plugins/host-monitor.*.hpi.pinned
```

### Step 3: Install New Plugin
```bash
# Copy new plugin to Jenkins plugins directory
cp target/host-monitor.hpi $JENKINS_HOME/plugins/

# Set correct permissions
chown jenkins:jenkins $JENKINS_HOME/plugins/host-monitor.hpi
# (adjust user/group if different)
```

### Step 4: Start Jenkins
```bash
# Start Jenkins
sudo systemctl start jenkins

# Or if running manually, restart your Jenkins process
```

### Step 5: Verify Installation
1. Wait for Jenkins to fully start (check logs)
   ```bash
   tail -f $JENKINS_HOME/logs/jenkins.log
   ```
   
2. Look for these lines in the log:
   ```
   INFO: Loaded plugin host-monitor v1.2
   INFO: SocketIOConfig indexed under hudson.Extension
   ```

3. Navigate to: **Manage Jenkins → System**

4. Scroll down - you should see: **"Host Monitor - Socket.IO Integration"**

## Verification Checklist

✅ Jenkins was completely stopped before plugin replacement
✅ Old plugin files were removed
✅ New plugin copied to plugins directory
✅ Jenkins fully restarted
✅ Jenkins logs show plugin loaded successfully
✅ System configuration page loads without errors

## Still Not Working?

### Check 1: Verify Plugin is Installed
Navigate to: **Manage Jenkins → Plugins → Installed**

Search for "Host Monitor" - should show:
- Name: Host Monitor Plugin
- Version: 1.2

### Check 2: Check Jenkins Logs
```bash
grep -i "socketio\|host-monitor" $JENKINS_HOME/logs/jenkins.log
```

Look for:
- ✅ "Loaded plugin host-monitor"
- ✅ "SocketIOConfig indexed"
- ❌ Any errors or exceptions

### Check 3: Verify JAR Contents
```bash
unzip -l $JENKINS_HOME/plugins/host-monitor/WEB-INF/lib/host-monitor.jar | grep SocketIOConfig
```

Should show:
```
SocketIOConfig.class
SocketIOConfig/global.jelly
```

### Check 4: Test with Jenkins Plugin Manager

1. Go to **Manage Jenkins → Plugins**
2. Click **Advanced settings** tab
3. Under "Deploy Plugin", upload: `target/host-monitor.hpi`
4. Check "Restart Jenkins after install"
5. Wait for Jenkins to restart

## Alternative: Manual Plugin Upload via Web UI

If command-line installation isn't working:

1. **Navigate to Jenkins Web UI**
   - Go to `http://your-jenkins-url/manage/`

2. **Open Plugin Manager**
   - Click "Plugins" (or "Manage Plugins" in older Jenkins)

3. **Upload Plugin**
   - Click "Advanced settings" tab
   - Scroll to "Deploy Plugin"
   - Click "Choose File"
   - Select: `target/host-monitor.hpi`
   - Click "Deploy"

4. **Restart Jenkins**
   - Check "Restart Jenkins when installation is complete"
   - Or manually restart from: `http://your-jenkins-url/restart`

5. **Verify**
   - After restart, go to: Manage Jenkins → System
   - Look for "Host Monitor - Socket.IO Integration" section

## Expected Result

After proper installation, the System configuration page should show:

```
┌─────────────────────────────────────────────────┐
│ Host Monitor - Socket.IO Integration           │
├─────────────────────────────────────────────────┤
│ Enable Socket.IO          [ ] (unchecked)      │
│                                                 │
│ Advanced...                                     │
│   Server Host:            [localhost    ]      │
│   Server Port:            [3000         ]      │
│   Namespace:              [/            ]      │
│   Event Name:             [hostStatus   ]      │
│                                                 │
│ [Socket.IO Message Format box with example]    │
└─────────────────────────────────────────────────┘
```

## Debug Mode

Enable debug logging for the plugin:

1. Go to: **Manage Jenkins → System Log**
2. Click "Add new log recorder"
3. Name: `HostMonitor`
4. Add logger: `io.jenkins.plugins.hostmonitor`
5. Set level: `FINEST`
6. Save
7. Restart Jenkins and check logs

## Contact Info

If issue persists after following all steps:
1. Check Jenkins version (minimum: 2.539)
2. Check Java version (should be 21)
3. Review full Jenkins startup logs
4. Check for conflicting plugins

---

**Current Plugin Version:** 1.2  
**Plugin File:** target/host-monitor.hpi (719KB)  
**Last Built:** $(date)
