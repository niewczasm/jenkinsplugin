package io.jenkins.plugins.hostmonitor;

import hudson.Extension;
import hudson.model.PageDecorator;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

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
     * Check if we're on the main Jenkins page
     */
    private boolean isMainPage() {
        StaplerRequest request = Stapler.getCurrentRequest();
        if (request == null) {
            return false;
        }

        // Get the request URI path
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        // Remove context path to get the actual page path
        String pagePath = requestUri;
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            pagePath = requestUri.substring(contextPath.length());
        }

        // Main page is root ("/") or just the context path
        return pagePath.equals("/") || pagePath.equals("") || pagePath.equals("/view/all/");
    }

    /**
     * Check if the widget should be displayed
     * Only show on main page and when there are hosts to monitor
     */
    public boolean isEnabled() {
        // Check if user has permission to view
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return false;
        }

        try {
            jenkins.checkPermission(Jenkins.READ);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return false;
        }

        return isMainPage() && !getHosts().isEmpty();
    }
}
