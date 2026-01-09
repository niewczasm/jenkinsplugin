package io.jenkins.plugins.hostmonitor;

import hudson.Extension;
import hudson.model.PersistentDescriptor;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Global configuration for Socket.IO connection settings
 */
@Extension
public class SocketIOConfig extends GlobalConfiguration implements PersistentDescriptor, Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SocketIOConfig.class.getName());
    
    private boolean enabled;
    private String serverHost;
    private int serverPort;
    private String namespace;
    private String eventName;
    
    public SocketIOConfig() {
        load();
    }
    
    public static SocketIOConfig get() {
        return GlobalConfiguration.all().get(SocketIOConfig.class);
    }
    
    @NonNull
    @Override
    public String getDisplayName() {
        return "Host Monitor Socket.IO Configuration";
    }
    
    // Getters
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getServerHost() {
        return serverHost != null ? serverHost : "localhost";
    }
    
    public int getServerPort() {
        return serverPort > 0 ? serverPort : 3000;
    }
    
    public String getNamespace() {
        return namespace != null ? namespace : "/";
    }
    
    public String getEventName() {
        return eventName != null ? eventName : "hostStatus";
    }
    
    // Setters
    @DataBoundSetter
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
        
        // Restart Socket.IO client when enabled status changes
        if (enabled) {
            SocketIOClientManager.getInstance().connect();
        } else {
            SocketIOClientManager.getInstance().disconnect();
        }
    }
    
    @DataBoundSetter
    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
        save();
    }
    
    @DataBoundSetter
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
        save();
    }
    
    @DataBoundSetter
    public void setNamespace(String namespace) {
        this.namespace = namespace;
        save();
    }
    
    @DataBoundSetter
    public void setEventName(String eventName) {
        this.eventName = eventName;
        save();
    }
    
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        
        // Reconnect with new settings
        if (enabled) {
            SocketIOClientManager.getInstance().reconnect();
        }
        
        return true;
    }
    
    /**
     * Get the full Socket.IO server URL
     */
    public String getServerUrl() {
        String host = getServerHost();
        int port = getServerPort();
        String ns = getNamespace();
        
        // Build URL
        String url = "http://" + host + ":" + port;
        if (!ns.equals("/")) {
            url += ns;
        }
        return url;
    }
}
