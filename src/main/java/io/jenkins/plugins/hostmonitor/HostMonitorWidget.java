package io.jenkins.plugins.hostmonitor;

import hudson.Extension;
import hudson.model.PageDecorator;
import hudson.model.User;
import jenkins.model.Jenkins;

import java.util.List;

/**
 * Adds the host monitor widget to Jenkins sidebar
 */
@Extension
public class HostMonitorWidget extends PageDecorator {
    
    public HostMonitorWidget() {
        super(HostMonitorWidget.class);
        load();
    }
    
    @Override
    public String getDisplayName() {
        return "Host Monitor Widget";
    }
    
    /**
     * Get all monitored hosts for display
     */
    public List<MonitoredHost> getHosts() {
        HostMonitorManager manager = HostMonitorManager.getInstance();
        return manager.getHosts();
    }
    
    /**
     * Check if the widget should be displayed
     */
    public boolean isEnabled() {
        // You can add logic here to show/hide the widget based on permissions
        return true;
    }
}
