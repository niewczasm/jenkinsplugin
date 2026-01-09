package io.jenkins.plugins.hostmonitor;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import jenkins.model.Jenkins;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages Socket.IO client connection and message handling
 */
public class SocketIOClientManager {
    private static final Logger LOGGER = Logger.getLogger(SocketIOClientManager.class.getName());
    private static SocketIOClientManager instance;
    
    private Socket socket;
    private boolean isConnected = false;
    
    private SocketIOClientManager() {
        // Private constructor for singleton
    }
    
    public static synchronized SocketIOClientManager getInstance() {
        if (instance == null) {
            instance = new SocketIOClientManager();
        }
        return instance;
    }
    
    /**
     * Connect to Socket.IO server based on configuration
     */
    public synchronized void connect(HostMonitorManager manager) {
        // Disconnect if already connected
        if (socket != null && socket.connected()) {
            disconnect();
        }

        if (manager == null || !manager.isSocketIOEnabled()) {
            LOGGER.info("Socket.IO is not enabled in configuration");
            return;
        }

        try {
            String serverUrl = manager.getSocketIOServerUrl();
            LOGGER.info("Connecting to Socket.IO server: " + serverUrl);

            // Configure Socket.IO options
            IO.Options options = IO.Options.builder()
                .setReconnection(true)
                .setReconnectionDelay(1000)
                .setReconnectionDelayMax(5000)
                .setReconnectionAttempts(Integer.MAX_VALUE)
                .build();

            socket = IO.socket(serverUrl, options);

            // Set up event listeners
            setupEventListeners(manager.getSocketIOEventName());

            // Connect
            socket.connect();

            LOGGER.info("Socket.IO connection initiated");

        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Invalid Socket.IO server URL", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error connecting to Socket.IO server", e);
        }
    }
    
    /**
     * Disconnect from Socket.IO server
     */
    public synchronized void disconnect() {
        if (socket != null) {
            LOGGER.info("Disconnecting from Socket.IO server");
            socket.disconnect();
            socket.off();
            socket = null;
            isConnected = false;
        }
    }
    
    /**
     * Reconnect with current configuration
     */
    public void reconnect(HostMonitorManager manager) {
        disconnect();
        connect(manager);
    }
    
    /**
     * Set up Socket.IO event listeners
     */
    private void setupEventListeners(String eventName) {
        // Connection events
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                isConnected = true;
                LOGGER.info("Connected to Socket.IO server");
            }
        });
        
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                isConnected = false;
                LOGGER.info("Disconnected from Socket.IO server");
            }
        });
        
        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args.length > 0) {
                    LOGGER.warning("Socket.IO connection error: " + args[0]);
                }
            }
        });
        
        // Custom event listener for host status updates
        socket.on(eventName, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args.length > 0) {
                    handleHostStatusMessage(args[0]);
                }
            }
        });
        
        LOGGER.info("Listening for Socket.IO event: " + eventName);
    }
    
    /**
     * Handle incoming host status message
     */
    private void handleHostStatusMessage(Object data) {
        try {
            JSONObject json;
            
            // Handle different data types
            if (data instanceof JSONObject) {
                json = (JSONObject) data;
            } else if (data instanceof String) {
                json = new JSONObject((String) data);
            } else {
                LOGGER.warning("Received unexpected data type: " + data.getClass().getName());
                return;
            }
            
            // Extract host information
            String hostname = json.optString("hostname", null);
            String status = json.optString("status", "UNKNOWN");
            String message = json.optString("message", "");
            
            if (hostname == null || hostname.trim().isEmpty()) {
                LOGGER.warning("Received message without hostname: " + json);
                return;
            }
            
            LOGGER.fine("Received host status update: " + hostname + " -> " + status);
            
            // Update host status
            HostMonitorManager manager = HostMonitorManager.getInstance();
            if (manager != null) {
                manager.updateHost(hostname, status, message);
            }
            
        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Error parsing Socket.IO message", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling Socket.IO message", e);
        }
    }
    
    /**
     * Check if connected to Socket.IO server
     */
    public boolean isConnected() {
        return isConnected && socket != null && socket.connected();
    }
    
    /**
     * Get current Socket.IO connection status
     */
    public String getConnectionStatus() {
        if (socket == null) {
            return "Not initialized";
        } else if (isConnected) {
            return "Connected";
        } else {
            return "Disconnected";
        }
    }
}
