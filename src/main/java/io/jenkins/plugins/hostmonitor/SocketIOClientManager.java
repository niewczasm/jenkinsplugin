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
     *
     * Expected format:
     * {
     *   "resources": [
     *     [hostname, url, id, active, hash, tags, status, reservation, null],
     *     ...
     *   ],
     *   "resource_count": N,
     *   ...
     * }
     *
     * Where each resource array has:
     * - Index 0: hostname (String)
     * - Index 7: status (String) - e.g., "IDLE", "BUSY", "ERROR"
     * - Index 8: reservation (String) - e.g., "No", "Yes (user)"
     */
    private void handleHostStatusMessage(Object data) {
        try {
            // Debug logging - show what we received
            LOGGER.info("=== Socket.IO Message Received ===");
            LOGGER.info("Data type: " + (data != null ? data.getClass().getName() : "null"));

            if (data != null) {
                String dataStr = data.toString();
                // Log first 500 chars to avoid flooding logs
                String preview = dataStr.length() > 500 ? dataStr.substring(0, 500) + "..." : dataStr;
                LOGGER.info("Data preview: " + preview);
            }

            JSONObject json;

            // Handle different data types
            if (data instanceof JSONObject) {
                LOGGER.fine("Data is already JSONObject");
                json = (JSONObject) data;
            } else if (data instanceof org.json.JSONObject) {
                LOGGER.fine("Data is org.json.JSONObject");
                json = (org.json.JSONObject) data;
            } else if (data instanceof String) {
                LOGGER.fine("Data is String, parsing to JSONObject");
                String strData = (String) data;
                LOGGER.info("String data length: " + strData.length());
                json = new JSONObject(strData);
            } else {
                LOGGER.warning("Received unexpected data type: " + data.getClass().getName());
                LOGGER.warning("Data value: " + data);
                return;
            }

            LOGGER.info("Successfully parsed to JSONObject");
            LOGGER.info("Has 'resources' key: " + json.has("resources"));

            // Check if this is the array-based format (resources key)
            if (json.has("resources")) {
                LOGGER.info("Parsing as resources array format");
                parseResourcesArrayFormat(json);
            } else {
                LOGGER.info("Parsing as simple format");
                // Fallback to old format for backward compatibility
                parseSimpleFormat(json);
            }

        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Error parsing Socket.IO message: " + e.getMessage(), e);
            if (data != null) {
                LOGGER.warning("Failed to parse data: " + data.toString());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling Socket.IO message: " + e.getMessage(), e);
            if (data != null) {
                LOGGER.severe("Failed data: " + data.toString());
            }
        }
    }

    /**
     * Parse resources array format
     */
    private void parseResourcesArrayFormat(JSONObject json) {
        try {
            org.json.JSONArray resources = json.getJSONArray("resources");
            int resourceCount = resources.length();

            LOGGER.fine("Parsing " + resourceCount + " resources from Socket.IO message");

            HostMonitorManager manager = HostMonitorManager.getInstance();
            if (manager == null) {
                LOGGER.warning("HostMonitorManager instance is null");
                return;
            }

            // Process each resource
            for (int i = 0; i < resourceCount; i++) {
                try {
                    org.json.JSONArray resource = resources.getJSONArray(i);

                    // Validate array has enough elements
                    if (resource.length() < 9) {
                        LOGGER.warning("Resource array at index " + i + " has insufficient elements: " + resource.length());
                        continue;
                    }

                    // Extract data from array indices
                    String hostname = resource.getString(0);  // Index 0: hostname
                    String status = resource.getString(7);     // Index 7: status (IDLE, BUSY, etc.)
                    String reservation = resource.getString(8); // Index 8: reservation (No, Yes (user))

                    // Validate hostname
                    if (hostname == null || hostname.trim().isEmpty()) {
                        LOGGER.warning("Resource at index " + i + " has empty hostname");
                        continue;
                    }

                    // Map status to our status values
                    String mappedStatus = mapResourceStatus(status);

                    // Build message from reservation info
                    String message = buildReservationMessage(reservation);

                    LOGGER.fine("Updating host: " + hostname + " -> " + mappedStatus + " (" + message + ")");

                    // Update host status
                    manager.updateHost(hostname, mappedStatus, message);

                } catch (JSONException e) {
                    LOGGER.log(Level.WARNING, "Error parsing resource at index " + i, e);
                }
            }

            LOGGER.info("Successfully processed " + resourceCount + " resources");

        } catch (JSONException e) {
            LOGGER.log(Level.WARNING, "Error parsing resources array", e);
        }
    }

    /**
     * Parse simple format (backward compatibility)
     * Format: { "hostname": "...", "status": "...", "message": "..." }
     */
    private void parseSimpleFormat(JSONObject json) {
        String hostname = json.optString("hostname", null);
        String status = json.optString("status", "UNKNOWN");
        String message = json.optString("message", "");

        if (hostname == null || hostname.trim().isEmpty()) {
            LOGGER.warning("Received message without hostname: " + json);
            return;
        }

        LOGGER.fine("Received host status update (simple format): " + hostname + " -> " + status);

        HostMonitorManager manager = HostMonitorManager.getInstance();
        if (manager != null) {
            manager.updateHost(hostname, status, message);
        }
    }

    /**
     * Map resource status to our display status
     */
    private String mapResourceStatus(String resourceStatus) {
        if (resourceStatus == null) {
            return "UNKNOWN";
        }

        switch (resourceStatus.toUpperCase()) {
            case "IDLE":
                return "ONLINE";  // IDLE resources are available/online
            case "BUSY":
                return "BUSY";
            case "ERROR":
                return "ERROR";
            case "OFFLINE":
                return "OFFLINE";
            case "WAIT":
                return "WARNING";  // WAIT status shown as warning
            default:
                return resourceStatus.toUpperCase();
        }
    }

    /**
     * Build message from reservation info
     */
    private String buildReservationMessage(String reservation) {
        if (reservation == null || reservation.trim().isEmpty()) {
            return "Available";
        }

        String res = reservation.trim();

        if (res.equalsIgnoreCase("No")) {
            return "Available";
        } else if (res.toLowerCase().startsWith("yes")) {
            // Extract user from "Yes (username)" format
            int start = res.indexOf('(');
            int end = res.indexOf(')');
            if (start != -1 && end != -1 && end > start) {
                String user = res.substring(start + 1, end);
                return "Reserved by " + user;
            }
            return "Reserved";
        } else {
            return res;
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
