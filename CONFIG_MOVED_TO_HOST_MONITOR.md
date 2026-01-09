# Socket.IO Configuration - Now in Host Monitor Page

## What Changed

The Socket.IO configuration has been **moved from Jenkins System settings to the Host Monitor management page**. This makes it easier to find and configure without hunting through the System settings.

## Why This Change?

- Jenkins 2.545 GlobalConfiguration wasn't showing in System settings
- Easier to access: all Host Monitor settings in one place
- No dependency on GlobalConfiguration mechanism

## New Location

**Navigate to:** `Manage Jenkins → Host Monitor`

The Socket.IO configuration is now at the **top of the Host Monitor page** with a configuration form.

## What You'll See

```
┌─────────────────────────────────────────────────────────────┐
│ Host Monitor Configuration                                   │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│ Socket.IO Integration                                        │
│ ┌───────────────────────────────────────────────────────┐   │
│ │ Connection Status: Disabled                           │   │
│ └───────────────────────────────────────────────────────┘   │
│                                                               │
│ ☐ Enable Socket.IO                                          │
│                                                               │
│ Server Host         [localhost              ]                │
│ Server Port         [3000                   ]                │
│ Namespace           [/                      ]                │
│ Event Name          [hostStatus             ]                │
│                                                               │
│ [Socket.IO Message Format example box]                       │
│                                                               │
│ [Save Configuration]                                          │
│                                                               │
├─────────────────────────────────────────────────────────────┤
│ Monitored Hosts                                              │
│ ...                                                           │
└─────────────────────────────────────────────────────────────┘
```

## How to Configure

### Step 1: Navigate to Host Monitor
1. Go to **Manage Jenkins**
2. Click **Host Monitor** (in the management links)

### Step 2: Configure Socket.IO
At the top of the page, you'll see the Socket.IO Integration section:

1. **Check "Enable Socket.IO"** to enable real-time updates
2. **Server Host**: Enter your Socket.IO server hostname (default: localhost)
3. **Server Port**: Enter the port (default: 3000)
4. **Namespace**: Socket.IO namespace (default: /)
5. **Event Name**: Event to listen for (default: hostStatus)
6. Click **"Save Configuration"**

### Step 3: Verify Connection
After saving, the page will reload and show:
- **Connection Status**: Should show "✓ Connected" (green) if successful
- **Server URL**: Shows the full connection URL
- **Event Name**: Shows what event the plugin is listening for

## Installation Instructions

### Remove Old Plugin
```bash
# Stop Jenkins
sudo systemctl stop jenkins

# Remove old plugin completely
rm -rf $JENKINS_HOME/plugins/host-monitor*

# Install new version
cp target/host-monitor.hpi $JENKINS_HOME/plugins/

# Start Jenkins
sudo systemctl start jenkins
```

### Verify Installation
1. Wait for Jenkins to fully start
2. Go to **Manage Jenkins → Host Monitor**
3. You should see the Socket.IO Integration section at the top

## Configuration Fields

| Field | Description | Default | Example |
|-------|-------------|---------|---------|
| **Enable Socket.IO** | Enable/disable integration | Unchecked | Check to enable |
| **Server Host** | Socket.IO server hostname or IP | `localhost` | `monitor.example.com` |
| **Server Port** | Socket.IO server port | `3000` | `3000` |
| **Namespace** | Socket.IO namespace | `/` | `/jenkins` |
| **Event Name** | Event name to listen for | `hostStatus` | `hostUpdate` |

## Message Format

Your Socket.IO server should emit events with this JSON format:

```json
{
  "hostname": "server1.example.com",
  "status": "ONLINE",
  "message": "All services running"
}
```

## Connection Status Indicators

| Status | Color | Meaning |
|--------|-------|---------|
| **✓ Connected** | Green | Successfully connected to Socket.IO server |
| **Disconnected** | Yellow | Not currently connected (check server) |
| **Disabled** | Grey | Socket.IO not enabled in configuration |

## Testing the Configuration

### 1. Start Your Socket.IO Server

Example Node.js server:
```javascript
const io = require('socket.io')(3000);

setInterval(() => {
  io.emit('hostStatus', {
    hostname: 'test-server.example.com',
    status: 'ONLINE',
    message: 'Test message from Socket.IO'
  });
}, 5000);
```

### 2. Configure in Jenkins
1. Go to **Manage Jenkins → Host Monitor**
2. Check **Enable Socket.IO**
3. Enter server details (localhost:3000)
4. Click **Save Configuration**

### 3. Verify Connection
- Status should show **"✓ Connected"** (green)
- Server URL should show: `http://localhost:3000`

### 4. Check Sidebar Widget
- Navigate to Jenkins homepage
- Look at the left sidebar
- You should see "Host Monitor Status" widget
- After a few seconds, "test-server.example.com" should appear

## Troubleshooting

### Configuration Not Saving
- Make sure you click "Save Configuration" button
- Check Jenkins logs for errors: `tail -f $JENKINS_HOME/logs/jenkins.log`

### Connection Status Shows "Disconnected"
1. Verify Socket.IO server is running:
   ```bash
   curl http://localhost:3000/socket.io/?EIO=4&transport=polling
   ```
2. Check server logs
3. Verify host/port/namespace are correct
4. Check firewall settings

### Hosts Not Appearing
1. Verify Socket.IO server is emitting events
2. Check event name matches configuration
3. Verify message format includes "hostname" field
4. Check Jenkins logs for parsing errors

## Benefits of New Location

✅ **Easier to find** - All Host Monitor settings in one place  
✅ **No dependency issues** - Doesn't rely on GlobalConfiguration  
✅ **Real-time status** - See connection status immediately  
✅ **Quick configuration** - Change settings and reconnect instantly  
✅ **Better UX** - Form validation and helpful hints  

## Summary

- ✅ Socket.IO config moved to **Manage Jenkins → Host Monitor**
- ✅ Configuration form at top of page
- ✅ Real-time connection status display
- ✅ Save button applies changes immediately
- ✅ Auto-reconnects when settings change
- ✅ Works with Jenkins 2.545+

---

**Plugin Version:** 1.2  
**Plugin File:** target/host-monitor.hpi (717KB)  
**Location:** Manage Jenkins → Host Monitor
