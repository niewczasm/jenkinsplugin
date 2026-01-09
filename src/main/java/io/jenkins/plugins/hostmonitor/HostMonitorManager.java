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
    
    public HostMonitorManager() {
        load();
        // Initialize Socket.IO client if enabled
        initializeSocketIO();
    }

    /**
     * Initialize Socket.IO client connection
     */
    private void initializeSocketIO() {
        SocketIOConfig config = SocketIOConfig.get();
        if (config != null && config.isEnabled()) {
            LOGGER.info("Initializing Socket.IO client");
            SocketIOClientManager.getInstance().connect();
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
                LOGGER.info("Loaded host monitor configuration with " + hosts.size() + " hosts");
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
        LOGGER.fine("Saved host monitor configuration with " + hosts.size() + " hosts");
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

    /**
     * Get Socket.IO connection status
     */
    public String getSocketIOStatus() {
        SocketIOConfig config = SocketIOConfig.get();
        if (config == null || !config.isEnabled()) {
            return "Disabled";
        }
        return SocketIOClientManager.getInstance().getConnectionStatus();
    }

    /**
     * Get Socket.IO configuration
     */
    public SocketIOConfig getSocketIOConfig() {
        return SocketIOConfig.get();
    }

    /**
     * Handle form submission to manually update hosts
     */
    @POST
    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) 
            throws ServletException, IOException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        
        JSONObject form = req.getSubmittedForm();
        // Handle form data if needed
        
        save();
        rsp.sendRedirect(".");
    }
}
