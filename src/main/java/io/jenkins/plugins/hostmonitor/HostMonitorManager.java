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

    // Expected counts for base hosts (hostname -> count)
    // If not specified, default is 1
    private ConcurrentHashMap<String, Integer> baseHostCounts = new ConcurrentHashMap<>();

    // Display configuration
    private boolean showAllDuplicates = true; // Default: show all duplicates with counts

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

                // Initialize baseHostCounts if null (backwards compatibility)
                if (baseHostCounts == null) {
                    baseHostCounts = new ConcurrentHashMap<>();
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
     * Update or add a host with status (for pipeline step - backward compatibility)
     */
    public MonitoredHost updateHost(String hostname, String status, String message) {
        // For pipeline step, use hostname as both key and id
        MonitoredHost host = hosts.computeIfAbsent(hostname, MonitoredHost::new);
        host.setStatus(status);
        host.setStatusMessage(message != null ? message : "");
        try {
            save();
        } catch (IOException e) {
            LOGGER.warning("Failed to save host monitor data: " + e.getMessage());
        }
        return host;
    }

    /**
     * Update or add a host with status using unique id (for WebSocket messages)
     */
    public MonitoredHost updateHost(String hostname, String id, String status, String message) {
        // Use id as the unique key
        String key = id != null && !id.isEmpty() ? id : hostname;
        MonitoredHost host = hosts.computeIfAbsent(key, k -> new MonitoredHost(hostname, id));
        host.setId(id);
        host.setStatus(status);
        host.setStatusMessage(message != null ? message : "");
        try {
            save();
        } catch (IOException e) {
            LOGGER.warning("Failed to save host monitor data: " + e.getMessage());
        }
        return host;
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
     * Get all monitored hosts with display names calculated for duplicates
     */
    public List<MonitoredHost> getHosts() {
        List<MonitoredHost> hostList = new ArrayList<>(hosts.values());

        if (!showAllDuplicates) {
            // Aggregate duplicates - show only one entry per hostname
            return aggregateDuplicateHosts(hostList);
        }

        // Show all duplicates with counts
        // Count occurrences of each hostname
        java.util.Map<String, Integer> hostnameCounts = new java.util.HashMap<>();
        java.util.Map<String, Integer> hostnameIndices = new java.util.HashMap<>();

        for (MonitoredHost host : hostList) {
            String hostname = host.getHostname();
            hostnameCounts.put(hostname, hostnameCounts.getOrDefault(hostname, 0) + 1);
        }

        // Set display names with counts for duplicates
        for (MonitoredHost host : hostList) {
            String hostname = host.getHostname();
            int totalCount = hostnameCounts.get(hostname);

            if (totalCount > 1) {
                // Multiple hosts with same name - add count
                int currentIndex = hostnameIndices.getOrDefault(hostname, 0) + 1;
                hostnameIndices.put(hostname, currentIndex);
                host.setDisplayName(hostname + " (" + currentIndex + "/" + totalCount + ")");
            } else {
                // Single host with this name - no count needed
                host.setDisplayName(hostname);
            }
        }

        return hostList;
    }

    /**
     * Aggregate duplicate hosts into single entries
     * Shows the "best" status for each hostname:
     * - IDLE/BUSY > WAIT > ERROR/OFFLINE > UNKNOWN
     */
    private List<MonitoredHost> aggregateDuplicateHosts(List<MonitoredHost> hostList) {
        java.util.Map<String, List<MonitoredHost>> groupedByHostname = new java.util.HashMap<>();

        // Group hosts by hostname
        for (MonitoredHost host : hostList) {
            String hostname = host.getHostname();
            groupedByHostname.computeIfAbsent(hostname, k -> new ArrayList<>()).add(host);
        }

        // Create aggregated list with one entry per hostname
        List<MonitoredHost> aggregated = new ArrayList<>();

        for (java.util.Map.Entry<String, List<MonitoredHost>> entry : groupedByHostname.entrySet()) {
            String hostname = entry.getKey();
            List<MonitoredHost> instances = entry.getValue();

            if (instances.size() == 1) {
                // Single instance - use as is
                MonitoredHost host = instances.get(0);
                host.setDisplayName(hostname);
                aggregated.add(host);
            } else {
                // Multiple instances - pick the best one
                MonitoredHost best = selectBestHost(instances);
                best.setDisplayName(hostname);
                aggregated.add(best);
            }
        }

        return aggregated;
    }

    /**
     * Select the "best" host from multiple instances
     * Priority: IDLE/BUSY > WAIT > ERROR/OFFLINE > UNKNOWN
     */
    private MonitoredHost selectBestHost(List<MonitoredHost> instances) {
        MonitoredHost best = instances.get(0);
        int bestPriority = getStatusPriority(best.getStatus());

        for (int i = 1; i < instances.size(); i++) {
            MonitoredHost candidate = instances.get(i);
            int candidatePriority = getStatusPriority(candidate.getStatus());

            if (candidatePriority < bestPriority) {
                best = candidate;
                bestPriority = candidatePriority;
            }
        }

        return best;
    }

    /**
     * Get priority for status (lower is better)
     */
    private int getStatusPriority(String status) {
        if (status == null) return 100;

        switch (status.toUpperCase()) {
            case "IDLE":
            case "BUSY":
                return 1;
            case "WAIT":
                return 2;
            case "ERROR":
            case "OFFLINE":
            case "DOWN":
                return 3;
            default:
                return 4; // UNKNOWN and others
        }
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

    // Display configuration getters/setters
    public boolean isShowAllDuplicates() {
        return showAllDuplicates;
    }

    public void setShowAllDuplicates(boolean showAllDuplicates) {
        this.showAllDuplicates = showAllDuplicates;
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
        StringBuilder sb = new StringBuilder();
        for (String hostname : baseHosts) {
            sb.append(hostname);
            Integer count = baseHostCounts.get(hostname);
            if (count != null && count > 1) {
                sb.append(" ").append(count);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public void setBaseHostsFromString(String hostsString) {
        baseHosts.clear();
        baseHostCounts.clear();

        if (hostsString != null && !hostsString.trim().isEmpty()) {
            String[] hostArray = hostsString.split("[,\\n]+");
            for (String hostLine : hostArray) {
                String trimmed = hostLine.trim();
                if (!trimmed.isEmpty()) {
                    // Parse "hostname" or "hostname count"
                    String[] parts = trimmed.split("\\s+");
                    String hostname = parts[0];
                    int count = 1; // Default count

                    if (parts.length > 1) {
                        try {
                            count = Integer.parseInt(parts[1]);
                            if (count < 1) {
                                count = 1; // Ensure at least 1
                            }
                        } catch (NumberFormatException e) {
                            LOGGER.warning("Invalid count for host " + hostname + ": " + parts[1] + ", using 1");
                        }
                    }

                    baseHosts.add(hostname);
                    if (count > 1) {
                        baseHostCounts.put(hostname, count);
                    }
                }
            }
        }
    }

    /**
     * Get expected count for a base host (default 1 if not specified)
     */
    public int getExpectedCount(String hostname) {
        return baseHostCounts.getOrDefault(hostname, 1);
    }

    /**
     * Getter for XML serialization
     */
    public ConcurrentHashMap<String, Integer> getBaseHostCountsMap() {
        return baseHostCounts;
    }

    /**
     * Setter for XML deserialization
     */
    public void setBaseHostCountsMap(ConcurrentHashMap<String, Integer> counts) {
        this.baseHostCounts = counts;
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
            hostJson.put("hostname", host.getDisplayName());  // Use displayName with count
            hostJson.put("status", host.getStatus());
            hostJson.put("statusMessage", host.getStatusMessage());
            hostJson.put("displayMessage", host.getDisplayMessage());
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

        // Get display configuration
        showAllDuplicates = form.optBoolean("showAllDuplicates", true);
        LOGGER.info("Show all duplicates: " + showAllDuplicates);

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
