# Quick Start Guide - Jenkins Host Monitor Plugin

This guide will help you get started with the Host Monitor Plugin in 5 minutes.

## Step 1: Build the Plugin

```bash
cd host-monitor-plugin
./build.sh
```

Or manually:
```bash
mvn clean package
```

The plugin file will be created at `target/host-monitor.hpi`

## Step 2: Install in Jenkins

1. Open Jenkins in your browser
2. Navigate to **Manage Jenkins** → **Manage Plugins**
3. Click the **Advanced** tab
4. Under "Upload Plugin":
   - Click **Choose File**
   - Select `target/host-monitor.hpi`
   - Click **Upload**
5. Restart Jenkins when prompted

## Step 3: Create a Monitoring Pipeline

Create a new Pipeline job in Jenkins with this example:

```groovy
pipeline {
    agent any
    stages {
        stage('Monitor Hosts') {
            steps {
                script {
                    // Monitor a web server
                    updateHostStatus(
                        hostname: 'web-server-01',
                        status: 'ONLINE',
                        message: 'All services running'
                    )
                    
                    // Monitor a database
                    updateHostStatus(
                        hostname: 'db-server-01',
                        status: 'HEALTHY',
                        message: 'Connection successful'
                    )
                    
                    // Monitor an API
                    updateHostStatus(
                        hostname: 'api.example.com',
                        status: 'WARNING',
                        message: 'High latency detected'
                    )
                }
            }
        }
    }
}
```

## Step 4: Run the Pipeline

1. Click **Build Now** on your pipeline job
2. Wait for it to complete

## Step 5: View Host Status

The Host Monitor widget will now appear in the Jenkins sidebar showing:
- **web-server-01** - 🟢 ONLINE
- **db-server-01** - 🟢 HEALTHY
- **api.example.com** - 🟡 WARNING

You can also view all hosts at:
**Manage Jenkins** → **Host Monitor**

## Advanced Usage

### Scheduled Monitoring

Add a cron trigger to run monitoring every 5 minutes:

```groovy
pipeline {
    agent any
    triggers {
        cron('*/5 * * * *')
    }
    stages {
        stage('Monitor') {
            steps {
                script {
                    updateHostStatus hostname: 'server-01', status: 'ONLINE'
                }
            }
        }
    }
}
```

### Health Check with Error Handling

```groovy
stage('Check Server') {
    steps {
        script {
            try {
                def status = sh(
                    script: 'curl -s -o /dev/null -w "%{http_code}" http://server-01',
                    returnStdout: true
                ).trim()
                
                if (status == '200') {
                    updateHostStatus(
                        hostname: 'server-01',
                        status: 'ONLINE',
                        message: 'HTTP 200 OK'
                    )
                } else {
                    updateHostStatus(
                        hostname: 'server-01',
                        status: 'WARNING',
                        message: "HTTP ${status}"
                    )
                }
            } catch (Exception e) {
                updateHostStatus(
                    hostname: 'server-01',
                    status: 'OFFLINE',
                    message: 'Connection failed'
                )
            }
        }
    }
}
```

### Monitor Multiple Hosts

```groovy
stage('Monitor Fleet') {
    steps {
        script {
            def hosts = ['web-01', 'web-02', 'web-03']
            hosts.each { host ->
                updateHostStatus(
                    hostname: host,
                    status: 'ONLINE',
                    message: 'Service operational'
                )
            }
        }
    }
}
```

## Status Types

| Status | Color | When to Use |
|--------|-------|-------------|
| ONLINE, UP, HEALTHY | 🟢 Green | Host is working normally |
| WARNING, DEGRADED | 🟡 Yellow | Host has issues but is functional |
| OFFLINE, DOWN, ERROR | 🔴 Red | Host is not working |
| UNKNOWN | ⚪ Grey | Status not yet determined |

## Troubleshooting

**Widget not showing?**
- Make sure you've run the pipeline at least once
- Refresh your browser
- Check that the plugin is enabled in Manage Plugins

**Pipeline step not found?**
- Verify plugin installation
- Restart Jenkins
- Check Jenkins logs for errors

**Need Help?**
See the full [README.md](README.md) for detailed documentation.

## Next Steps

1. Set up scheduled monitoring for your infrastructure
2. Integrate with your existing health check scripts
3. Create monitoring dashboards by organizing hosts
4. Add notifications when host status changes

Enjoy monitoring your infrastructure with Jenkins! 🚀
