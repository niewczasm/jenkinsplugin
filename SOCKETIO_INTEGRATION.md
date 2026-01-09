# Socket.IO Integration Guide

## Overview

The Host Monitor Plugin now supports **real-time updates via Socket.IO**. Instead of manually updating host status through Jenkins pipelines, you can configure the plugin to connect to a Socket.IO server that broadcasts host status updates in real-time.

## What's New

### New Components:
1. **SocketIOConfig** - Global configuration for Socket.IO connection settings
2. **SocketIOClientManager** - Manages the Socket.IO client connection
3. **Configuration UI** - Configure Socket.IO in Jenkins System settings
4. **Connection Status** - View connection status in Host Monitor management page

### Features:
- ✅ Real-time host status updates
- ✅ Automatic reconnection with exponential backoff
- ✅ Configurable server host, port, namespace, and event name
- ✅ Connection status monitoring
- ✅ Backward compatible - Pipeline step still works

## Configuration

### 1. Access Configuration Page

Navigate to: **Manage Jenkins → System**

Scroll down to find: **Host Monitor - Socket.IO Integration**

### 2. Configuration Fields

| Field | Description | Default | Example |
|-------|-------------|---------|---------|
| **Enable Socket.IO** | Enable/disable Socket.IO integration | ❌ Unchecked | ✅ Check to enable |
| **Server Host** | Socket.IO server hostname or IP | `localhost` | `monitor.example.com` |
| **Server Port** | Socket.IO server port | `3000` | `3000` |
| **Namespace** | Socket.IO namespace | `/` | `/jenkins` |
| **Event Name** | Event name to listen for | `hostStatus` | `hostUpdate` |

### 3. Example Configuration

```
Enable Socket.IO: ✓ (checked)
Server Host: monitor.example.com
Server Port: 3000
Namespace: /jenkins
Event Name: hostStatus
```

This connects to: `http://monitor.example.com:3000/jenkins` and listens for `hostStatus` events.

## Socket.IO Server Requirements

### Message Format

Your Socket.IO server must emit events with the following JSON format:

```json
{
  "hostname": "server1.example.com",
  "status": "ONLINE",
  "message": "All services running"
}
```

### Field Descriptions

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `hostname` | ✅ Yes | String | Unique identifier for the host |
| `status` | ❌ No | String | Status value (see below) | 
| `message` | ❌ No | String | Additional status message |

### Status Values

| Status | Display Color | Use Case |
|--------|--------------|----------|
| `ONLINE`, `UP`, `HEALTHY` | 🟢 Green | Host is operational |
| `WARNING`, `DEGRADED` | 🟡 Yellow | Host has issues but running |
| `OFFLINE`, `DOWN`, `ERROR` | 🔴 Red | Host is down or failed |
| Any other value | ⚪ Grey | Unknown status |

## Example Socket.IO Server

### Node.js Example

```javascript
const express = require('express');
const app = express();
const http = require('http').createServer(app);
const io = require('socket.io')(http, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

// Namespace (optional)
const jenkinsNamespace = io.of('/jenkins');

jenkinsNamespace.on('connection', (socket) => {
  console.log('Jenkins plugin connected');
  
  socket.on('disconnect', () => {
    console.log('Jenkins plugin disconnected');
  });
});

// Example: Send host status updates every 5 seconds
setInterval(() => {
  jenkinsNamespace.emit('hostStatus', {
    hostname: 'web-server-01.example.com',
    status: 'ONLINE',
    message: 'HTTP 200 - All services operational'
  });
  
  jenkinsNamespace.emit('hostStatus', {
    hostname: 'db-server-01.example.com',
    status: 'WARNING',
    message: 'High CPU usage: 85%'
  });
  
  jenkinsNamespace.emit('hostStatus', {
    hostname: 'api-server-01.example.com',
    status: 'OFFLINE',
    message: 'Connection timeout'
  });
}, 5000);

http.listen(3000, () => {
  console.log('Socket.IO server listening on *:3000');
});
```

### Python Example

```python
import socketio
import eventlet
from flask import Flask

app = Flask(__name__)
sio = socketio.Server(cors_allowed_origins='*')
app.wsgi_app = socketio.WSGIApp(sio, app.wsgi_app)

# Namespace (optional)
namespace = '/jenkins'

@sio.on('connect', namespace=namespace)
def connect(sid, environ):
    print('Jenkins plugin connected:', sid)

@sio.on('disconnect', namespace=namespace)
def disconnect(sid):
    print('Jenkins plugin disconnected:', sid)

# Send updates every 5 seconds
def send_status_updates():
    while True:
        sio.emit('hostStatus', {
            'hostname': 'web-server-01.example.com',
            'status': 'ONLINE',
            'message': 'HTTP 200 - All services operational'
        }, namespace=namespace)
        
        sio.emit('hostStatus', {
            'hostname': 'db-server-01.example.com',
            'status': 'WARNING',
            'message': 'High CPU usage: 85%'
        }, namespace=namespace)
        
        eventlet.sleep(5)

if __name__ == '__main__':
    # Start background task
    eventlet.spawn(send_status_updates)
    
    # Run server
    eventlet.wsgi.server(eventlet.listen(('', 3000)), app)
```

## Testing the Integration

### 1. Start Your Socket.IO Server

```bash
node server.js
# or
python server.py
```

### 2. Configure Jenkins Plugin

1. Go to **Manage Jenkins → System**
2. Find **Host Monitor - Socket.IO Integration**
3. Check **Enable Socket.IO**
4. Enter your server details
5. Click **Save**

### 3. Verify Connection

1. Go to **Manage Jenkins → Host Monitor**
2. Check the **Socket.IO Integration** section
3. Status should show **✓ Connected** (green)

### 4. Verify Host Updates

- The sidebar widget should display hosts as they're received
- Hosts update automatically without page refresh
- Check Jenkins logs for connection messages:
  ```
  INFO: Connecting to Socket.IO server: http://localhost:3000
  INFO: Connected to Socket.IO server
  INFO: Listening for Socket.IO event: hostStatus
  ```

## Connection Status

The Host Monitor management page shows the current connection status:

| Status | Meaning | Action |
|--------|---------|--------|
| **✓ Connected** (Green) | Successfully connected to server | None needed |
| **Disconnected** (Yellow) | Not currently connected | Check server is running |
| **Disabled** (Grey) | Socket.IO not enabled | Enable in System configuration |
| **Not initialized** | Client hasn't started | Restart Jenkins |

## Troubleshooting

### Socket.IO Not Connecting

1. **Check server is running:**
   ```bash
   curl http://localhost:3000/socket.io/?EIO=4&transport=polling
   ```
   Should return a response (not 404)

2. **Check Jenkins logs:**
   ```bash
   tail -f $JENKINS_HOME/logs/jenkins.log | grep SocketIO
   ```

3. **Verify configuration:**
   - Host/Port correct?
   - Namespace matches server (default `/`)?
   - Event name matches server emission?

### Hosts Not Updating

1. **Check message format:**
   - Does the message include `hostname`?
   - Is the JSON valid?

2. **Check event name:**
   - Verify Jenkins is listening for the correct event
   - Default is `hostStatus`

3. **Check server is emitting:**
   - Add logging in your Socket.IO server
   - Verify events are being emitted

### Connection Keeps Dropping

1. **Check firewall:**
   - Ensure port is open
   - Check network between Jenkins and Socket.IO server

2. **Check server stability:**
   - Is the Socket.IO server crashing?
   - Check server logs

3. **Adjust reconnection settings:**
   - Plugin auto-reconnects with exponential backoff
   - Default: 1s, 2s, 4s, up to 5s max

## Hybrid Mode (Pipeline + Socket.IO)

You can use **both** Socket.IO and Pipeline updates simultaneously:

- **Socket.IO**: For real-time monitoring of external systems
- **Pipeline**: For Jenkins-initiated checks within jobs

Both methods update the same host list and persist to disk.

## Example: Complete Monitoring Setup

### 1. External Monitoring (Socket.IO)

Socket.IO server monitors external hosts 24/7 and pushes updates to Jenkins.

```javascript
// External monitoring system
setInterval(async () => {
  const hosts = await checkAllHosts(); // Your monitoring logic
  
  hosts.forEach(host => {
    jenkinsNamespace.emit('hostStatus', {
      hostname: host.hostname,
      status: host.status,
      message: host.message
    });
  });
}, 30000); // Every 30 seconds
```

### 2. Jenkins Pipeline Checks

Jenkins jobs can still update host status during builds:

```groovy
pipeline {
    agent any
    stages {
        stage('Deploy') {
            steps {
                script {
                    // Deploy app
                    sh './deploy.sh'
                    
                    // Update status after deployment
                    updateHostStatus(
                        hostname: 'app-server-01',
                        status: 'ONLINE',
                        message: "Deployed build ${env.BUILD_NUMBER}"
                    )
                }
            }
        }
    }
}
```

## Security Considerations

1. **Network Security:**
   - Use private network between Jenkins and Socket.IO server
   - Consider using SSL/TLS (wss://)
   - Implement authentication if Socket.IO server is public

2. **Access Control:**
   - Only administrators can configure Socket.IO settings
   - Configuration requires Jenkins ADMINISTER permission

3. **Input Validation:**
   - Plugin validates all incoming messages
   - Invalid messages are logged and ignored

## Performance

- **Reconnection**: Automatic with exponential backoff
- **Memory**: ~1KB per monitored host
- **CPU**: Minimal (event-driven updates)
- **Network**: Persistent WebSocket connection + periodic heartbeats

## Migration from Pipeline-Only

If you're currently using only pipeline updates:

1. Enable Socket.IO configuration
2. Existing hosts remain in the widget
3. Socket.IO updates existing hosts or adds new ones
4. Pipeline step continues to work as before

No data loss - all methods write to the same storage.

## Summary

✅ Real-time host monitoring via Socket.IO  
✅ Easy configuration in Jenkins System settings  
✅ Connection status monitoring  
✅ Automatic reconnection  
✅ Backward compatible with pipeline step  
✅ Flexible message format  
✅ Multiple status types with color coding  

---

**Plugin Version:** 1.1  
**Plugin File:** target/host-monitor.hpi (719KB)  
**Socket.IO Client:** v2.1.0
