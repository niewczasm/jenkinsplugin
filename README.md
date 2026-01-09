# Jenkins Host Monitor Plugin

A Jenkins plugin that adds a sidebar widget to display the status of external hosts (non-agent nodes) that you can update from Jenkins pipelines.

## Features

- **Sidebar Widget**: Similar to "Build Executor Status", displays monitored hosts in the Jenkins sidebar
- **Pipeline Integration**: Update host status directly from Jenkinsfile using the `updateHostStatus` step
- **Status Colors**: Visual indicators (green/yellow/red/grey) for different host states
- **Management Interface**: View all monitored hosts in Jenkins management section
- **Persistent Storage**: Host statuses persist across Jenkins restarts

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
| `ONLINE`, `UP`, `HEALTHY` | 🟢 Green | Host is functioning normally |
| `WARNING`, `DEGRADED` | 🟡 Yellow | Host has issues but is operational |
| `OFFLINE`, `DOWN`, `ERROR` | 🔴 Red | Host is not functioning |
| `UNKNOWN` or any other | ⚪ Grey | Status not determined |

Status values are case-insensitive.

## Viewing Host Status

### Sidebar Widget

The host monitor widget appears automatically in the Jenkins sidebar when there are monitored hosts. It shows:
- Hostname
- Current status (with color coding)
- Status message
- Last updated timestamp

### Management Page

Access the full list of monitored hosts:
1. Go to Jenkins → Manage Jenkins
2. Click on "Host Monitor"
3. View all hosts with their complete status information

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

## Changelog

### Version 1.0-SNAPSHOT
- Initial release
- Sidebar widget displaying host status
- Pipeline step for updating host status
- Management interface
- Color-coded status indicators
