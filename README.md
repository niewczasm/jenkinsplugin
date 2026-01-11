# Jenkins Host Monitor Plugin

A Jenkins plugin that adds a sidebar widget to display the status of external hosts (non-agent nodes) that you can update from Jenkins pipelines.

## Features

- **Sidebar Widget**: Similar to "Build Executor Status", displays monitored hosts in the Jenkins sidebar with auto-refresh
- **Pipeline Integration**: Update host status directly from Jenkinsfile using the `updateHostStatus` step
- **Socket.IO Integration**: Real-time host status updates from external monitoring systems
- **Duplicate Host Support**: Track multiple instances of the same hostname with automatic counting (e.g., host-1 (1/3), host-1 (2/3))
- **Base Host List**: Configure expected hosts with instance counts to track missing hosts as OFFLINE
- **Display Modes**: Choose between showing all duplicate instances or aggregated view with best status
- **Status Colors**: Visual indicators (green/yellow/red/grey) for different host states
- **Management Interface**: View all monitored hosts in Jenkins management section with clickable title
- **Persistent Storage**: Host statuses persist across Jenkins restarts
- **Smart Sorting**: OFFLINE hosts first, then base configured hosts, then others (alphabetically)

## Installation

### Building from Source

1. Prerequisites:
   - JDK 11 or later
   - Maven 3.8 or later

2. Build the plugin:
   ```bash
   cd host-monitor-plugin
   mvn clean package
   ```

3. The plugin file will be created at `target/host-monitor.hpi`

4. Install in Jenkins:
   - Go to Jenkins → Manage Jenkins → Manage Plugins → Advanced
   - Under "Upload Plugin", choose the `.hpi` file
   - Click "Upload"
   - Restart Jenkins when prompted

## Usage

### Pipeline Step

Use the `updateHostStatus` step in your Jenkinsfile to update host status:

#### Declarative Pipeline

```groovy
pipeline {
    agent any
    stages {
        stage('Monitor External Hosts') {
            steps {
                script {
                    // Check web server
                    def webStatus = sh(script: 'curl -s -o /dev/null -w "%{http_code}" http://web-server-01', returnStdout: true).trim()
                    if (webStatus == '200') {
                        updateHostStatus hostname: 'web-server-01', 
                                        status: 'ONLINE', 
                                        message: 'HTTP 200 OK'
                    } else {
                        updateHostStatus hostname: 'web-server-01', 
                                        status: 'ERROR', 
                                        message: "HTTP ${webStatus}"
                    }
                    
                    // Check database
                    updateHostStatus hostname: 'db-server-01', 
                                    status: 'HEALTHY', 
                                    message: 'All databases responding'
                    
                    // Check API endpoint
                    updateHostStatus hostname: 'api.example.com', 
                                    status: 'WARNING', 
                                    message: 'High latency detected'
                }
            }
        }
    }
}
```

#### Scripted Pipeline

```groovy
node {
    stage('Health Check') {
        // Simple status update
        updateHostStatus hostname: 'prod-server-01', 
                        status: 'UP'
        
        // With detailed message
        updateHostStatus hostname: 'staging-server-01', 
                        status: 'DEGRADED', 
                        message: 'CPU usage at 85%'
    }
}
```

#### Monitoring Multiple Hosts

```groovy
pipeline {
    agent any
    stages {
        stage('Monitor Infrastructure') {
            steps {
                script {
                    def hosts = [
                        'web-01.example.com',
                        'web-02.example.com',
                        'db-01.example.com',
                        'cache-01.example.com'
                    ]
                    
                    hosts.each { hostname ->
                        // Your health check logic here
                        def isAlive = sh(script: "ping -c 1 ${hostname}", returnStatus: true) == 0
                        
                        if (isAlive) {
                            updateHostStatus hostname: hostname, 
                                            status: 'ONLINE', 
                                            message: 'Ping successful'
                        } else {
                            updateHostStatus hostname: hostname, 
                                            status: 'OFFLINE', 
                                            message: 'Ping failed'
                        }
                    }
                }
            }
        }
    }
}
```

### Parameters

The `updateHostStatus` step accepts the following parameters:

| Parameter | Required | Description | Example |
|-----------|----------|-------------|---------|
| `hostname` | Yes | The hostname or identifier of the host to monitor | `'web-server-01'` |
| `status` | No | The status of the host (default: `'UNKNOWN'`) | `'ONLINE'`, `'WARNING'`, `'ERROR'` |
| `message` | No | Additional information about the status (default: `''`) | `'All services running'` |

### Status Values and Colors

The plugin automatically applies colors based on the status value:

| Status | Color | Use Case |
|--------|-------|----------|
| `IDLE`, `BUSY`, `ONLINE`, `UP`, `HEALTHY` | 🟢 Green | Host is functioning normally |
| `WARNING`, `DEGRADED` | 🟡 Yellow | Host has issues but is operational |
| `WAIT`, `ERROR`, `OFFLINE`, `DOWN` | 🔴 Red | Host is not functioning or waiting |
| `UNKNOWN` or any other | ⚪ Grey | Status not determined |

Status values are case-insensitive.

**Note**: When using Socket.IO integration, the plugin recognizes `IDLE` (available) and `BUSY` (in use) states from external systems like Autotest.

## Configuration

### Base Host List

Configure which hosts should always be monitored:

1. Go to Jenkins → Manage Jenkins → Host Monitor
2. In the "Base Host List" section, enter hostnames (one per line)
3. Optionally specify expected instance count after hostname: `hostname count`

**Example:**
```
web-server-01
db-server-01 2
cache-server-01 3
api-server-01
```

This configuration:
- Expects 1 instance of `web-server-01` and `api-server-01`
- Expects 2 instances of `db-server-01`
- Expects 3 instances of `cache-server-01`
- If fewer instances are reported, missing ones will show as OFFLINE

### Display Mode

Choose how duplicate hosts are displayed:

- **Show all duplicates** (checked): Display each instance separately with counts
  ```
  db-server-01 (1/2) - IDLE
  db-server-01 (2/2) - BUSY
  ```

- **Aggregate view** (unchecked): Show one entry per hostname with best status
  ```
  db-server-01 - IDLE
  ```

  Priority: IDLE/BUSY > WAIT > ERROR/OFFLINE > UNKNOWN

### Socket.IO Integration

For real-time monitoring from external systems:

1. Enable Socket.IO in configuration
2. Configure server host, port, namespace, and event name
3. External systems send host status updates via Socket.IO

See [SOCKETIO_INTEGRATION.md](SOCKETIO_INTEGRATION.md) for details.

## Viewing Host Status

### Sidebar Widget

The host monitor widget appears automatically in the Jenkins sidebar when there are monitored hosts. It shows:
- Hostname (with count for duplicates)
- Current status (with color coding)
- Status message (reservation info or test path)
- Auto-refreshes every 5 seconds
- Clickable title to access configuration

### Management Page

Access the full list of monitored hosts:
1. Go to Jenkins → Manage Jenkins
2. Click on "Host Monitor"
3. View all hosts with their complete status information
4. Configure base hosts and display settings

## Example Use Cases

### 1. Production Server Health Checks

```groovy
pipeline {
    agent any
    triggers {
        cron('*/5 * * * *') // Run every 5 minutes
    }
    stages {
        stage('Check Production') {
            steps {
                script {
                    def servers = [
                        'prod-web-01': 'https://web01.prod.example.com/health',
                        'prod-api-01': 'https://api01.prod.example.com/health',
                        'prod-db-01': 'db01.prod.example.com:5432'
                    ]
                    
                    servers.each { hostname, endpoint ->
                        // Perform your health check
                        // Update status accordingly
                        updateHostStatus hostname: hostname, 
                                        status: 'ONLINE', 
                                        message: 'Health check passed'
                    }
                }
            }
        }
    }
}
```

### 2. External Service Monitoring

```groovy
pipeline {
    agent any
    stages {
        stage('Monitor Third-Party Services') {
            steps {
                script {
                    // Check AWS S3
                    updateHostStatus hostname: 'AWS S3 (us-east-1)', 
                                    status: 'ONLINE', 
                                    message: 'All buckets accessible'
                    
                    // Check External API
                    updateHostStatus hostname: 'payment-gateway.example.com', 
                                    status: 'HEALTHY', 
                                    message: 'Response time: 120ms'
                }
            }
        }
    }
}
```

### 3. Network Device Monitoring

```groovy
pipeline {
    agent any
    stages {
        stage('Monitor Network Devices') {
            steps {
                script {
                    def devices = [
                        'router-01.network.local',
                        'switch-01.network.local',
                        'firewall-01.network.local'
                    ]
                    
                    devices.each { device ->
                        // SNMP check or ping
                        updateHostStatus hostname: device, 
                                        status: 'UP', 
                                        message: 'Device responding'
                    }
                }
            }
        }
    }
}
```

## Development

### Project Structure

```
host-monitor-plugin/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/io/jenkins/plugins/hostmonitor/
│   │   │   ├── MonitoredHost.java              # Host model
│   │   │   ├── HostMonitorManager.java         # Manager singleton
│   │   │   ├── HostMonitorWidget.java          # Sidebar widget
│   │   │   └── UpdateHostStatusStep.java       # Pipeline step
│   │   ├── resources/io/jenkins/plugins/hostmonitor/
│   │   │   ├── HostMonitorWidget/
│   │   │   │   └── global.jelly               # Widget UI
│   │   │   ├── HostMonitorManager/
│   │   │   │   └── index.jelly                # Management page
│   │   │   └── hostmonitor.jelly              # CSS adjunct
│   │   └── webapp/css/
│   │       └── hostmonitor.css                 # Styles
└── README.md
```

### Building for Development

```bash
# Build and run Jenkins with the plugin
mvn hpi:run

# Access at http://localhost:8080/jenkins
```

### Running Tests

```bash
mvn test
```

## Troubleshooting

### Widget Not Showing

1. Verify hosts have been added using the `updateHostStatus` step
2. Check that the plugin is properly installed in Manage Plugins
3. Ensure no JavaScript errors in browser console

### Status Not Updating

1. Check Jenkins logs for any errors
2. Verify the pipeline step is being executed
3. Ensure proper permissions for the pipeline

### Configuration Issues

1. Check `$JENKINS_HOME/config.xml` for plugin configuration
2. Verify write permissions to Jenkins home directory
3. Review Jenkins system logs

## License

MIT License

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions:
- File an issue on the project repository
- Check Jenkins plugin documentation at https://jenkins.io/doc/developer/plugin-development/

## Advanced Features

### Duplicate Host Tracking

When using Socket.IO integration, the plugin can track multiple instances of the same hostname:

**Automatic Counting:**
```
host-1 (1/3) - IDLE
host-1 (2/3) - BUSY
host-1 (3/3) - OFFLINE
```

**Expected Count Validation:**
- Configure expected counts in base host list: `host-1 3`
- If fewer instances report, missing ones show as OFFLINE
- Helps ensure all expected instances are running

**Display Modes:**
- All duplicates: See status of each instance
- Aggregated: See overall best status per hostname

### Real-Time Updates

The sidebar widget auto-refreshes every 5 seconds to show latest status without page reload.

### Smart Host Sorting

Hosts are displayed in priority order:
1. OFFLINE hosts (highlighted first for attention)
2. Base configured hosts (your critical infrastructure)
3. Other hosts (alphabetically)

## Changelog

### Version 1.7+
- Added Socket.IO integration for real-time updates
- Duplicate hostname support with automatic counting
- Base host list with expected instance counts
- Display mode option (all duplicates vs aggregated)
- Auto-refresh sidebar widget
- Clickable widget title
- Smart host sorting
- Reservation and test path tracking

### Version 1.0-SNAPSHOT
- Initial release
- Sidebar widget displaying host status
- Pipeline step for updating host status
- Management interface
- Color-coded status indicators
