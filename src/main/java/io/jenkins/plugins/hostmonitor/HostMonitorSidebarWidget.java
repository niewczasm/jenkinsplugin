package io.jenkins.plugins.hostmonitor;

import hudson.widgets.Widget;
import jenkins.model.Jenkins;

import java.util.List;

/**
 * Sidebar widget that displays host monitor status
 */
public class HostMonitorSidebarWidget extends Widget {
    
    @Override
    public String getUrlName() {
        return "host-monitor-widget";
    }
    
    public Jenkins getOwner() {
        return Jenkins.get();
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
        return !getHosts().isEmpty();
    }
}
