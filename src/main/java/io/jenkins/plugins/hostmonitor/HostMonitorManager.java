package io.jenkins.plugins.hostmonitor;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.ManagementLink;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.verb.GET;
import org.kohsuke.stapler.verb.POST;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Manages the list of monitored hosts and their statuses
 */
@Extension
public class HostMonitorManager extends ManagementLink implements Saveable {
    private static final Logger LOGGER = Logger.getLogger(HostMonitorManager.class.getName());

    private ConcurrentHashMap<String, MonitoredHost> hosts = new ConcurrentHashMap<>();

    // Base host list - hosts that should always be monitored
    private CopyOnWriteArrayList<String> baseHosts = new CopyOnWriteArrayList<>();

    // Socket.IO configuration
    private boolean socketIOEnabled = false;
    private String socketIOHost = "localhost";
    private int socketIOPort = 3000;
    private String socketIONamespace = "/";
    private String socketIOEventName = "hostStatus";

    public HostMonitorManager() {
        load();
        // Initialize Socket.IO client if enabled
        initializeSocketIO();
    }

    /**
     * Initialize Socket.IO client connection
     */
    private void initializeSocketIO() {
        if (socketIOEnabled) {
            LOGGER.info("Initializing Socket.IO client");
            SocketIOClientManager.getInstance().connect(this);
        }
    }

    public static HostMonitorManager getInstance() {
        return Jenkins.get().getExtensionList(HostMonitorManager.class).get(0);
    }

    /**
     * Get the configuration file
     */
    protected XmlFile getConfigFile() {
        return new XmlFile(new File(Jenkins.get().getRootDir(), "host-monitor-config.xml"));
    }

    /**
     * Load the configuration from disk
     */
    public synchronized void load() {
        XmlFile file = getConfigFile();
        if (file.exists()) {
            try {
                file.unmarshal(this);

                // Initialize baseHosts if null (backwards compatibility)
                if (baseHosts == null) {
                    baseHosts = new CopyOnWriteArrayList<>();
                }

                LOGGER.info("Loaded host monitor configuration with " + hosts.size() + " hosts and " +
                           baseHosts.size() + " base hosts");
            } catch (IOException e) {
                LOGGER.warning("Failed to load host monitor configuration: " + e.getMessage());
            }
        }
    }
    
    @Override
    public String getIconFileName() {
        return "symbol-server";
    }
    
    @Override
    public String getDisplayName() {
        return "Host Monitor";
    }
    
    @Override
    public String getUrlName() {
        return "host-monitor";
    }
    
    @Override
    public String getDescription() {
        return "Monitor external hosts status";
    }
    
    /**
     * Update or add a host with status
     */
    public void updateHost(String hostname, String status, String message) {
        MonitoredHost host = hosts.computeIfAbsent(hostname, MonitoredHost::new);
        host.setStatus(status);
        host.setStatusMessage(message != null ? message : "");
        try {
            save();
        } catch (IOException e) {
            LOGGER.warning("Failed to save host monitor data: " + e.getMessage());
        }
    }
    
    /**
     * Remove a host from monitoring
     */
    public void removeHost(String hostname) {
        hosts.remove(hostname);
        try {
            save();
        } catch (IOException e) {
            LOGGER.warning("Failed to save host monitor data: " + e.getMessage());
        }
    }
    
    /**
     * Get all monitored hosts
     */
    public List<MonitoredHost> getHosts() {
        return new ArrayList<>(hosts.values());
    }
    
    /**
     * Get a specific host
     */
    public MonitoredHost getHost(String hostname) {
        return hosts.get(hostname);
    }
    
    @Override
    public synchronized void save() throws IOException {
        getConfigFile().write(this);
        SaveableListener.fireOnChange(this, getConfigFile());
        LOGGER.fine("Saved host monitor configuration with " + hosts.size() + " hosts and " +
                   baseHosts.size() + " base hosts");
    }

    /**
     * Getter for XML serialization
     */
    public ConcurrentHashMap<String, MonitoredHost> getHostsMap() {
        return hosts;
    }

    /**
     * Setter for XML deserialization
     */
    public void setHostsMap(ConcurrentHashMap<String, MonitoredHost> hosts) {
        this.hosts = hosts;
    }

    // Socket.IO Getters
    public boolean isSocketIOEnabled() {
        return socketIOEnabled;
    }

    public String getSocketIOHost() {
        return socketIOHost;
    }

    public int getSocketIOPort() {
        return socketIOPort;
    }

    public String getSocketIONamespace() {
        return socketIONamespace;
    }

    public String getSocketIOEventName() {
        return socketIOEventName;
    }

    public String getSocketIOServerUrl() {
        String url = "http://" + socketIOHost + ":" + socketIOPort;
        if (!socketIONamespace.equals("/")) {
            url += socketIONamespace;
        }
        return url;
    }

    /**
     * Get Socket.IO connection status
     */
    public String getSocketIOStatus() {
        if (!socketIOEnabled) {
            return "Disabled";
        }
        return SocketIOClientManager.getInstance().getConnectionStatus();
    }

    // Socket.IO Setters
    public void setSocketIOEnabled(boolean enabled) {
        this.socketIOEnabled = enabled;
    }

    public void setSocketIOHost(String host) {
        this.socketIOHost = host;
    }

    public void setSocketIOPort(int port) {
        this.socketIOPort = port;
    }

    public void setSocketIONamespace(String namespace) {
        this.socketIONamespace = namespace;
    }

    public void setSocketIOEventName(String eventName) {
        this.socketIOEventName = eventName;
    }

    // Base host list getters and setters

    /**
     * Get base hosts list (returns a copy for safety)
     */
    public List<String> getBaseHosts() {
        return new ArrayList<>(baseHosts);
    }

    /**
     * Getter for XML serialization - returns actual field
     */
    public CopyOnWriteArrayList<String> getBaseHostsList() {
        return baseHosts;
    }

    /**
     * Setter for XML deserialization
     */
    public void setBaseHostsList(CopyOnWriteArrayList<String> baseHosts) {
        this.baseHosts = baseHosts;
    }

    public String getBaseHostsAsString() {
        return String.join("\n", baseHosts);
    }

    public void setBaseHostsFromString(String hostsString) {
        baseHosts.clear();
        if (hostsString != null && !hostsString.trim().isEmpty()) {
            String[] hostArray = hostsString.split("[,\\n]+");
            for (String host : hostArray) {
                String trimmed = host.trim();
                if (!trimmed.isEmpty()) {
                    baseHosts.add(trimmed);
                }
            }
        }
    }

    /**
     * AJAX endpoint to get current host status as JSON
     */
    @GET
    public void doHostsJson(StaplerRequest req, StaplerResponse rsp) throws IOException {
        // Get hosts from sidebar widget (already sorted)
        HostMonitorSidebarWidget widget = new HostMonitorSidebarWidget();
        List<MonitoredHost> sortedHosts = widget.getHosts();

        // Build JSON response
        JSONObject response = new JSONObject();
        net.sf.json.JSONArray hostsArray = new net.sf.json.JSONArray();

        for (MonitoredHost host : sortedHosts) {
            JSONObject hostJson = new JSONObject();
            hostJson.put("hostname", host.getHostname());
            hostJson.put("status", host.getStatus());
            hostJson.put("statusMessage", host.getStatusMessage());
            hostJson.put("statusClass", host.getStatusClass());
            hostsArray.add(hostJson);
        }

        response.put("hosts", hostsArray);

        rsp.setContentType("application/json");
        rsp.getWriter().print(response.toString());
    }

    /**
     * Handle Socket.IO configuration form submission
     */
    @POST
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp)
            throws ServletException, IOException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        JSONObject form = req.getSubmittedForm();

        // Get base hosts configuration from form
        String baseHostsInput = form.optString("baseHosts", "");
        setBaseHostsFromString(baseHostsInput);
        LOGGER.info("Base hosts updated: " + baseHosts.size() + " hosts configured");

        // Get Socket.IO configuration from form
        boolean wasEnabled = socketIOEnabled;
        socketIOEnabled = form.optBoolean("socketIOEnabled", false);
        socketIOHost = form.optString("socketIOHost", "localhost");
        socketIOPort = form.optInt("socketIOPort", 3000);
        socketIONamespace = form.optString("socketIONamespace", "/");
        socketIOEventName = form.optString("socketIOEventName", "hostStatus");

        LOGGER.info("Socket.IO configuration updated - Enabled: " + socketIOEnabled);

        // Save configuration
        save();

        // Handle connection changes
        if (socketIOEnabled && !wasEnabled) {
            // Enabled - connect
            LOGGER.info("Socket.IO enabled, connecting...");
            SocketIOClientManager.getInstance().connect(this);
        } else if (!socketIOEnabled && wasEnabled) {
            // Disabled - disconnect
            LOGGER.info("Socket.IO disabled, disconnecting...");
            SocketIOClientManager.getInstance().disconnect();
        } else if (socketIOEnabled) {
            // Still enabled but config changed - reconnect
            LOGGER.info("Socket.IO configuration changed, reconnecting...");
            SocketIOClientManager.getInstance().reconnect(this);
        }

        rsp.sendRedirect(".");
    }
}
