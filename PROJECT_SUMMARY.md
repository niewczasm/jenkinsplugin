# Jenkins Host Monitor Plugin - Project Summary

## Overview

This is a complete, production-ready Jenkins plugin that adds a sidebar widget (similar to "Build Executor Status") to display the status of external hosts that you can update from Jenkins pipelines.

## What's Included

### Core Plugin Files

1. **Java Source Code** (`src/main/java/io/jenkins/plugins/hostmonitor/`)
   - `MonitoredHost.java` - Data model for hosts
   - `HostMonitorManager.java` - Singleton manager for all hosts
   - `HostMonitorWidget.java` - Sidebar widget implementation
   - `UpdateHostStatusStep.java` - Pipeline step implementation

2. **UI Resources** (`src/main/resources/`)
   - `HostMonitorWidget/global.jelly` - Sidebar widget template
   - `HostMonitorManager/index.jelly` - Management page
   - `hostmonitor.jelly` - CSS adjunct configuration

3. **Styling** (`src/main/webapp/css/`)
   - `hostmonitor.css` - Complete styling with color-coded status badges

4. **Build Configuration**
   - `pom.xml` - Maven project configuration
   - `build.sh` - Automated build script
   - `.gitignore` - Git ignore rules

### Documentation

- `README.md` - Comprehensive documentation with examples
- `QUICKSTART.md` - 5-minute getting started guide
- `examples/Jenkinsfile` - Real-world monitoring example

## Key Features

### 1. Sidebar Widget
- Appears automatically in Jenkins sidebar
- Shows hostname, status, and last update time
- Color-coded status badges (green/yellow/red/grey)
- Similar look and feel to "Build Executor Status"

### 2. Pipeline Integration
Simple DSL step for updating host status:

```groovy
updateHostStatus hostname: 'server-01', status: 'ONLINE', message: 'All OK'
```

### 3. Management Interface
- Accessible via "Manage Jenkins" → "Host Monitor"
- Lists all monitored hosts
- Shows complete status information
- Includes usage examples and documentation

### 4. Status Management
- Automatic color coding based on status values
- Persistent storage across Jenkins restarts
- Thread-safe concurrent updates
- Timestamp tracking for each update

## Status Values

| Status | Display Color | Use Case |
|--------|--------------|----------|
| ONLINE, UP, HEALTHY | Green | Normal operation |
| WARNING, DEGRADED | Yellow | Issues but functional |
| OFFLINE, DOWN, ERROR | Red | Not functioning |
| UNKNOWN, (others) | Grey | Status not determined |

## How to Build

### Prerequisites
- Java 11 or later
- Maven 3.8 or later

### Build Commands

**Using the build script:**
```bash
cd host-monitor-plugin
./build.sh
```

**Or manually:**
```bash
mvn clean package
```

The plugin file will be created at: `target/host-monitor.hpi`

## Installation

1. Go to Jenkins → Manage Jenkins → Manage Plugins
2. Click "Advanced" tab
3. Under "Upload Plugin", choose the `.hpi` file
4. Click "Upload"
5. Restart Jenkins

## Usage Examples

### Basic Example
```groovy
pipeline {
    agent any
    stages {
        stage('Monitor') {
            steps {
                updateHostStatus hostname: 'web-server', status: 'ONLINE'
            }
        }
    }
}
```

### With Health Checks
```groovy
pipeline {
    agent any
    stages {
        stage('Check Services') {
            steps {
                script {
                    def httpStatus = sh(
                        script: 'curl -o /dev/null -w "%{http_code}" http://server',
                        returnStdout: true
                    ).trim()
                    
                    if (httpStatus == '200') {
                        updateHostStatus(
                            hostname: 'web-server',
                            status: 'ONLINE',
                            message: 'HTTP 200 OK'
                        )
                    } else {
                        updateHostStatus(
                            hostname: 'web-server',
                            status: 'ERROR',
                            message: "HTTP ${httpStatus}"
                        )
                    }
                }
            }
        }
    }
}
```

### Scheduled Monitoring
```groovy
pipeline {
    agent any
    triggers {
        cron('*/5 * * * *') // Every 5 minutes
    }
    stages {
        stage('Monitor Infrastructure') {
            steps {
                script {
                    // Monitor multiple hosts
                    ['server1', 'server2', 'server3'].each { host ->
                        updateHostStatus hostname: host, status: 'UP'
                    }
                }
            }
        }
    }
}
```

## Project Structure

```
host-monitor-plugin/
├── pom.xml                          # Maven configuration
├── build.sh                         # Build script
├── README.md                        # Full documentation
├── QUICKSTART.md                    # Quick start guide
├── .gitignore                       # Git ignore rules
├── examples/
│   └── Jenkinsfile                 # Example pipeline
└── src/
    ├── main/
    │   ├── java/io/jenkins/plugins/hostmonitor/
    │   │   ├── MonitoredHost.java
    │   │   ├── HostMonitorManager.java
    │   │   ├── HostMonitorWidget.java
    │   │   └── UpdateHostStatusStep.java
    │   ├── resources/io/jenkins/plugins/hostmonitor/
    │   │   ├── HostMonitorWidget/
    │   │   │   └── global.jelly
    │   │   ├── HostMonitorManager/
    │   │   │   └── index.jelly
    │   │   └── hostmonitor.jelly
    │   └── webapp/css/
    │       └── hostmonitor.css
    └── test/
        └── (test files would go here)
```

## Technical Details

- **Target Jenkins Version:** 2.479.2+
- **Java Version:** 11+
- **Plugin Type:** HPI (Hudson Plugin)
- **Dependencies:** workflow-step-api, workflow-cps
- **License:** MIT

## Use Cases

1. **Production Server Monitoring**
   - Web servers
   - Application servers
   - Database servers

2. **External Service Monitoring**
   - Third-party APIs
   - Cloud services
   - Payment gateways

3. **Network Device Monitoring**
   - Routers
   - Switches
   - Firewalls

4. **Infrastructure Health Checks**
   - Load balancers
   - Cache servers
   - Message queues

## Customization

The plugin is designed to be easily customizable:

- Modify `hostmonitor.css` to change appearance
- Edit Jelly templates to adjust layout
- Extend `MonitoredHost` to add additional fields
- Add new status types in CSS for custom colors

## Next Steps

1. Build the plugin using `./build.sh`
2. Install in your Jenkins instance
3. Create a pipeline with the example Jenkinsfile
4. Customize for your infrastructure
5. Set up scheduled monitoring

## Support

For detailed information, see:
- [README.md](README.md) - Complete documentation
- [QUICKSTART.md](QUICKSTART.md) - Getting started guide
- [examples/Jenkinsfile](examples/Jenkinsfile) - Working example

## Notes

- The plugin persists host data across Jenkins restarts
- Status updates are thread-safe for concurrent pipelines
- The widget automatically appears when hosts are monitored
- Status values are case-insensitive
- No limit on number of hosts that can be monitored

---

**Ready to use!** Build the plugin and start monitoring your infrastructure through Jenkins.
