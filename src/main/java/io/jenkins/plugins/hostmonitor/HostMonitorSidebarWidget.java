package io.jenkins.plugins.hostmonitor;

import hudson.widgets.Widget;
import jenkins.model.Jenkins;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
     * Get all monitored hosts for display, sorted by:
     * 1. OFFLINE hosts first (ascending)
     * 2. Base hosts from configuration (ascending)
     * 3. Rest (ascending)
     */
    public List<MonitoredHost> getHosts() {
        HostMonitorManager manager = HostMonitorManager.getInstance();
        List<MonitoredHost> allHosts = manager.getHosts();
        List<String> baseHosts = manager.getBaseHosts();

        // Separate hosts into categories
        List<MonitoredHost> offlineHosts = new ArrayList<>();
        List<MonitoredHost> baseConfigHosts = new ArrayList<>();
        List<MonitoredHost> otherHosts = new ArrayList<>();

        for (MonitoredHost host : allHosts) {
            if ("OFFLINE".equalsIgnoreCase(host.getStatus())) {
                offlineHosts.add(host);
            } else if (baseHosts.contains(host.getHostname())) {
                baseConfigHosts.add(host);
            } else {
                otherHosts.add(host);
            }
        }

        // Sort each category alphabetically by hostname
        Comparator<MonitoredHost> byHostname = Comparator.comparing(MonitoredHost::getHostname, String.CASE_INSENSITIVE_ORDER);
        offlineHosts.sort(byHostname);
        baseConfigHosts.sort(byHostname);
        otherHosts.sort(byHostname);

        // Combine in the desired order
        List<MonitoredHost> result = new ArrayList<>();
        result.addAll(offlineHosts);
        result.addAll(baseConfigHosts);
        result.addAll(otherHosts);

        return result;
    }
    
    /**
     * Check if the widget should be displayed
     */
    public boolean isEnabled() {
        return !getHosts().isEmpty();
    }
}
