package io.jenkins.plugins.hostmonitor;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a monitored host with its current status
 */
public class MonitoredHost implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String hostname;
    private String status;
    private String statusMessage;
    private long lastUpdated;
    
    public MonitoredHost(String hostname) {
        this.hostname = hostname;
        this.status = "UNKNOWN";
        this.statusMessage = "";
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public Date getLastUpdatedDate() {
        return new Date(lastUpdated);
    }
    
    public String getStatusClass() {
        switch (status.toUpperCase()) {
            case "IDLE":
            case "BUSY":
                return "status-green";
            case "ERROR":
            case "WAIT":
            case "OFFLINE":
                return "status-red";
            default:
                return "status-grey";
        }
    }
}
