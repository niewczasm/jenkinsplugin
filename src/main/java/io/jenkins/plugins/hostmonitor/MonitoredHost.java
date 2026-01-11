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
    private String reservedBy;  // Username who reserved the host
    private String testPath;    // Test path being executed
    
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

    public String getReservedBy() {
        return reservedBy;
    }

    public void setReservedBy(String reservedBy) {
        this.reservedBy = reservedBy;
    }

    public String getTestPath() {
        return testPath;
    }

    public void setTestPath(String testPath) {
        this.testPath = testPath;
    }

    /**
     * Get the display message to show below hostname in sidebar
     * Priority:
     * 1. If reserved: "Reserved by (user)"
     * 2. If not reserved but BUSY: show test path
     * 3. Otherwise: show statusMessage or "Available"
     */
    public String getDisplayMessage() {
        // Priority 1: If reserved, show reservation info
        if (reservedBy != null && !reservedBy.isEmpty()) {
            return "Reserved by " + reservedBy;
        }

        // Priority 2: If BUSY and has test path, show test path
        if ("BUSY".equalsIgnoreCase(status) && testPath != null && !testPath.isEmpty()) {
            return testPath;
        }

        // Priority 3: Show statusMessage or "Available"
        if (statusMessage != null && !statusMessage.isEmpty()) {
            return statusMessage;
        }

        return "Available";
    }
}
