package io.jenkins.plugins.hostmonitor;

import hudson.Extension;
import hudson.model.PageDecorator;
import jenkins.model.Jenkins;

import java.util.List;

/**
 * Adds the host monitor widget to Jenkins sidebar
 */
@Extension
public class HostMonitorWidget extends PageDecorator {

    private transient HostMonitorSidebarWidget sidebarWidget;

    public HostMonitorWidget() {
        super(HostMonitorWidget.class);
        load();
    }

    @Override
    public String getDisplayName() {
        return "Host Monitor Widget";
    }

    /**
     * Get the sidebar widget instance
     */
    public HostMonitorSidebarWidget getSidebarWidget() {
        if (sidebarWidget == null) {
            sidebarWidget = new HostMonitorSidebarWidget();
        }
        return sidebarWidget;
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
