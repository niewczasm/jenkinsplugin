# Socket.IO Array-Based Message Format

## Overview

The plugin now supports parsing array-based Socket.IO messages from your WebSocket server. This format sends multiple resources in a single message.

## Message Format

### Structure
```json
{
  "resources": [
    [hostname, url, id, active, hash, tags, status, reservation, extra],
    [hostname, url, id, active, hash, tags, status, reservation, extra],
    ...
  ],
  "resource_count": N,
  "executor_count": M,
  "resource_state": {...}
}
```

### Array Indices
Each resource is an array with the following indices:
- **Index 0**: hostname (String) - e.g., "2400-eeabb-testwall-nuc1.T0"
- **Index 1**: url (String) - e.g., "http://eeabb-testwall-nuc1:60801"
- **Index 2**: id (String) - e.g., "NS25320108330"
- **Index 3**: active (Boolean) - e.g., true
- **Index 4**: hash (String) - e.g., "ac96dafb1c"
- **Index 5**: tags (Array) - e.g., ['cpu_type = cortexr5', ...]
- **Index 6**: (not used)
- **Index 7**: status (String) - e.g., "IDLE", "BUSY", "ERROR"
- **Index 8**: reservation (String) - e.g., "No", "Yes (rler)"
- **Index 9**: extra (can be null)

## Status Mapping

The plugin maps resource statuses to display statuses:

| Resource Status | Display Status | Color | Description |
|----------------|----------------|-------|-------------|
| `IDLE` | `ONLINE` | Green | Resource is idle/available |
| `BUSY` | `BUSY` | Blue | Resource is busy/in-use |
| `ERROR` | `ERROR` | Red | Resource has an error |
| `OFFLINE` | `OFFLINE` | Red | Resource is offline |
| `WAIT` | `WARNING` | Yellow | Resource is waiting |
| Other | Same value | Grey | Unknown status |

## Reservation Message Mapping

The plugin converts reservation info to readable messages:

| Reservation Value | Display Message |
|------------------|-----------------|
| `"No"` | "Available" |
| `"Yes (username)"` | "Reserved by username" |
| `"Yes"` | "Reserved" |
| Empty/null | "Available" |

## Example Real Message

### Input (from your WebSocket):
```python
{
  'resources': [
    ['2400-eeabb-testwall-nuc1.T0', 'http://eeabb-testwall-nuc1:60801', 
     'NS25320108330', True, 'ac96dafb1c', 
     ['cpu_type = cortexr5', 'id = NS25320108330@eeabb-testwall-nuc1', "module_type = ['T0']"], 
     'IDLE', 'No', None],
    ['ldb2-tritonrack-2', 'http://ldb2-tritonrack-2:56501', 
     '00277', True, 'BES-SW-2602-1.3', 
     ['cpu_type = nios2', 'id = 00277@ldb2-tritonrack-2', "module_type = ['T1', 'T0']"], 
     'IDLE', 'Yes (rler)', None]
  ],
  'resource_count': 2
}
```

### Parsed Output (in Jenkins):
```
Host: 2400-eeabb-testwall-nuc1.T0
Status: ONLINE (green)
Message: Available

Host: ldb2-tritonrack-2
Status: ONLINE (green)
Message: Reserved by rler
```

## Backward Compatibility

The plugin still supports the old simple format:
```json
{
  "hostname": "server1.example.com",
  "status": "ONLINE",
  "message": "All services running"
}
```

If the message doesn't have a `resources` key, it falls back to the simple format.

## Debugging

### Enable Debug Logging

The plugin now includes extensive debug logging. When a Socket.IO message arrives, you'll see:

```
INFO: === Socket.IO Message Received ===
INFO: Data type: org.json.JSONObject
INFO: Data preview: {"resources":[["2400-eeabb-testwall-nuc1.T0",...
INFO: Successfully parsed to JSONObject
INFO: Has 'resources' key: true
INFO: Parsing as resources array format
INFO: Parsing 22 resources from Socket.IO message
INFO: Updating host: 2400-eeabb-testwall-nuc1.T0 -> ONLINE (Available)
INFO: Successfully processed 22 resources
```

### View Logs

```bash
# Watch Jenkins logs in real-time
tail -f $JENKINS_HOME/logs/jenkins.log | grep -i "socketio\|host.*monitor"

# Search for parsing errors
grep -i "error parsing" $JENKINS_HOME/logs/jenkins.log

# See received messages
grep -i "socket.io message received" $JENKINS_HOME/logs/jenkins.log
```

### Common Issues and Solutions

#### Error: "A JSONObject text must begin with '{'"

**Problem:** The data received is not valid JSON.

**Debug steps:**
1. Check the Socket.IO server is sending JSON (not Python dict string)
2. Look at the "Data preview" in logs to see what was received
3. Verify Content-Type headers

**Solution:** Make sure your Socket.IO server sends proper JSON:
```python
# WRONG - sends Python string representation
socket.emit('hostStatus', str(data))  # ❌

# CORRECT - sends JSON
import json
socket.emit('hostStatus', json.loads(json.dumps(data)))  # ✅
# or simply:
socket.emit('hostStatus', data)  # Socket.IO handles JSON automatically
```

#### Error: "Resource array has insufficient elements"

**Problem:** Some resource arrays have fewer than 9 elements.

**Solution:** Ensure all resource arrays have at least 9 elements (indices 0-8). Pad with null if needed.

#### No Hosts Appearing

**Causes:**
1. Socket.IO not connected - Check connection status in Host Monitor page
2. Wrong event name - Verify event name matches in config
3. Data format mismatch - Check logs for parsing errors
4. Empty resources array - Check `resource_count` > 0

## Socket.IO Server Example (Python)

```python
import socketio
import json

sio = socketio.Server(cors_allowed_origins='*')
app = socketio.WSGIApp(sio)

@sio.event
def connect(sid, environ):
    print(f'Jenkins connected: {sid}')

@sio.event  
def disconnect(sid):
    print(f'Jenkins disconnected: {sid}')

def send_resources():
    """Send resource updates to Jenkins"""
    data = {
        'resources': [
            ['host1.example.com', 'http://host1:8080', 'id1', True, 'hash1', 
             ['tag1'], None, 'IDLE', 'No', None],
            ['host2.example.com', 'http://host2:8080', 'id2', True, 'hash2', 
             ['tag2'], None, 'BUSY', 'Yes (john)', None],
        ],
        'resource_count': 2
    }
    
    # Socket.IO automatically handles JSON serialization
    sio.emit('hostStatus', data)
    print(f'Sent {len(data["resources"])} resources to Jenkins')

# Send updates every 10 seconds
import eventlet
def background_task():
    while True:
        send_resources()
        eventlet.sleep(10)

if __name__ == '__main__':
    eventlet.spawn(background_task)
    eventlet.wsgi.server(eventlet.listen(('0.0.0.0', 3000)), app)
```

## Testing

### 1. Start Socket.IO Server

```bash
python socketio_server.py
```

### 2. Configure Jenkins Plugin

1. Go to **Manage Jenkins → Host Monitor**
2. Enable Socket.IO
3. Server: `localhost`
4. Port: `3000`
5. Event: `hostStatus`
6. Click **Save Configuration**

### 3. Verify

- Connection status should be **"✓ Connected"**
- Check Jenkins logs for "Successfully processed N resources"
- Navigate to Jenkins homepage
- Check sidebar for "Host Monitor Status" widget
- Hosts should appear with their statuses

## Summary

✅ Supports array-based message format  
✅ Extracts hostname (index 0), status (index 7), reservation (index 8)  
✅ Maps resource statuses (IDLE → ONLINE, etc.)  
✅ Formats reservation messages (Yes (user) → Reserved by user)  
✅ Backward compatible with simple format  
✅ Extensive debug logging  
✅ Handles large message batches (22+ resources)  

---

**Plugin Version:** 1.2  
**Plugin File:** target/host-monitor.hpi (717KB)
