# Host Monitor Widget - Sidebar Placement Update

## Changes Made

The Host Monitor widget has been updated to appear in the **Jenkins sidebar** (beneath "Build Queue" and "Build Executor Status") instead of at the page footer.

### New Files Created:
1. **HostMonitorSidebarWidget.java** - New Widget class that extends hudson.widgets.Widget
2. **HostMonitorSidebarWidget/index.jelly** - Sidebar widget view with compact styling
3. **HostMonitorWidget/header.jelly** - PageDecorator view that injects the widget into the sidebar

### Updated Files:
1. **HostMonitorWidget.java** - Updated to provide sidebar widget instance
2. Removed **footer.jelly** (replaced with header.jelly)

## How to Install

1. **Uninstall the old version:**
   ```bash
   rm $JENKINS_HOME/plugins/host-monitor.hpi
   rm -rf $JENKINS_HOME/plugins/host-monitor/
   ```

2. **Install the new version:**
   ```bash
   cp target/host-monitor.hpi $JENKINS_HOME/plugins/
   ```

3. **Restart Jenkins:**
   ```bash
   # Restart your Jenkins instance
   ```

4. **Run your monitoring pipeline** to populate hosts

## What You'll See

### In the Sidebar:
After running your monitoring pipeline, you'll see a new section in the left sidebar:

```
┌─────────────────────────────┐
│ Build Queue                 │
│ (empty)                     │
└─────────────────────────────┘

┌─────────────────────────────┐
│ Build Executor Status       │
│ ● computer (idle)           │
└─────────────────────────────┘

┌─────────────────────────────┐
│ Host Monitor Status        │
├─────────────────────────────┤
│ web-server-01.example.com   │
│ HTTP 200 - All services... │
│                     [ONLINE]│
├─────────────────────────────┤
│ db-server-01.example.com    │
│ High CPU usage detected     │
│                    [WARNING]│
├─────────────────────────────┤
│ api-server-01.example.com   │
│ Connection timeout          │
│                    [OFFLINE]│
└─────────────────────────────┘
```

### Features:
- **Compact display** - Fits in sidebar width
- **Color-coded badges** - Green (ONLINE/UP/HEALTHY), Yellow (WARNING/DEGRADED), Red (OFFLINE/DOWN/ERROR), Grey (UNKNOWN)
- **Tooltips** - Full hostname and message on hover (if truncated)
- **Real-time updates** - Refreshes with page
- **Auto-hides** - Widget only appears when hosts exist

## Test Pipeline

Use this pipeline to test the sidebar widget:

```groovy
pipeline {
    agent any
    stages {
        stage('Monitor Hosts') {
            steps {
                script {
                    updateHostStatus(
                        hostname: 'web-server-01.example.com',
                        status: 'ONLINE',
                        message: 'HTTP 200 - All services operational'
                    )
                    
                    updateHostStatus(
                        hostname: 'db-server-01.example.com',
                        status: 'WARNING',
                        message: 'High CPU usage detected'
                    )
                    
                    updateHostStatus(
                        hostname: 'api-server-01.example.com',
                        status: 'OFFLINE',
                        message: 'Connection timeout'
                    )
                }
            }
        }
    }
}
```

After running this pipeline:
1. Navigate to any Jenkins page (dashboard, job page, etc.)
2. Look at the left sidebar - below "Build Executor Status"
3. You should see "Host Monitor Status" with your 3 hosts

## Styling

The widget uses:
- **Jenkins standard pane layout** - Matches Build Queue/Executor Status appearance
- **Inline styles** - Ensures consistent rendering
- **Responsive badges** - Status indicators in green/yellow/red
- **Text truncation** - Long hostnames/messages truncate with ellipsis

## Troubleshooting

### Widget Not Appearing?

1. **Check hosts were added:**
   ```bash
   cat $JENKINS_HOME/host-monitor-config.xml
   ```
   You should see `<hosts>` entries.

2. **Check browser console:**
   - Open DevTools (F12)
   - Look for JavaScript errors
   - Check if `#side-panel` element exists

3. **Force refresh:**
   - Clear browser cache (Ctrl+Shift+R)
   - Or hard refresh (Ctrl+F5)

4. **Check Jenkins logs:**
   ```bash
   tail -f $JENKINS_HOME/logs/jenkins.log
   ```
   Look for errors related to "HostMonitor"

### Widget Appears in Wrong Location?

The JavaScript automatically finds the sidebar and places the widget after the Build Executor Status. If it appears elsewhere:
- Your Jenkins theme might have a custom layout
- Try refreshing the page
- Check if `#side-panel` ID exists in the page source

### Styling Issues?

If the styling doesn't match other sidebar widgets:
1. Check your Jenkins version (tested on Jenkins 2.539+)
2. Verify you're using a standard Jenkins theme
3. Custom themes may need CSS adjustments

## Reverting to Footer Display

If you prefer the widget at the page footer, rename:
```bash
mv src/main/resources/io/jenkins/plugins/hostmonitor/HostMonitorWidget/header.jelly \
   src/main/resources/io/jenkins/plugins/hostmonitor/HostMonitorWidget/footer.jelly
```

Then remove the JavaScript injection logic and rebuild.

## Summary

✅ Widget now appears in sidebar beneath Build Executor Status
✅ Compact styling matches Jenkins standard widgets
✅ Color-coded status badges
✅ Text truncation with tooltips
✅ Auto-hides when no hosts exist
✅ Data persists across refreshes and restarts

The new plugin file is: **target/host-monitor.hpi** (18KB)
