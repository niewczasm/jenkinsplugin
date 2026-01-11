package io.jenkins.plugins.hostmonitor;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import jenkins.model.Jenkins;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
     * - Index 6: status (String) - e.g., "IDLE", "BUSY", "ERROR"
     * - Index 7: reservation (String) - e.g., "No", "Yes (user)"
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
                String strData = (String) data;
                LOGGER.info("String data length: " + strData.length());

                // Check if string is actually JSON/Python dict (starts with { or [)
                String trimmed = strData.trim();
                if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                    LOGGER.fine("Received non-JSON string message: " + strData);
                    // This is a control/event message, not data - ignore it
                    return;
                }

                LOGGER.fine("Data is String, converting Python dict to JSON and parsing");
                // Convert Python dict syntax to JSON
                String jsonString = convertPythonDictToJson(strData);
                json = new JSONObject(jsonString);
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

            List<String> baseHosts = manager.getBaseHosts();

            // Track which host IDs were reported in this message
            Set<String> reportedHostIds = new HashSet<>();
            // Also track hostnames to check base hosts
            Set<String> reportedHostnames = new HashSet<>();

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
                    String id = resource.getString(2);         // Index 2: unique identifier
                    String status = resource.getString(6);     // Index 6: status (IDLE, BUSY, etc.)
                    String reservation = resource.getString(7); // Index 7: reservation (No, Yes (user))

                    // Get the last element from the array (test path)
                    String testPath = null;
                    int lastIndex = resource.length() - 1;
                    if (lastIndex >= 0 && !resource.isNull(lastIndex)) {
                        testPath = resource.optString(lastIndex, null);
                        // Process test path to skip everything until 5th "/"
                        if (testPath != null && !testPath.isEmpty()) {
                            testPath = processTestPath(testPath);
                        }
                    }

                    // Validate hostname and id
                    if (hostname == null || hostname.trim().isEmpty()) {
                        LOGGER.warning("Resource at index " + i + " has empty hostname");
                        continue;
                    }
                    if (id == null || id.trim().isEmpty()) {
                        LOGGER.warning("Resource at index " + i + " has empty id");
                        continue;
                    }

                    // Track this host ID and hostname as reported
                    reportedHostIds.add(id);
                    reportedHostnames.add(hostname);

                    // Map status to our status values
                    String mappedStatus = mapResourceStatus(status);

                    // Extract username from reservation field
                    String reservedBy = extractReservedBy(reservation);

                    // Build message from reservation info (for backward compatibility)
                    String message = buildReservationMessage(reservation);

                    LOGGER.fine("Updating host: " + hostname + " (id: " + id + ") -> " + mappedStatus + " (reserved by: " + reservedBy + ", test path: " + testPath + ")");

                    // Update host status with id
                    MonitoredHost host = manager.updateHost(hostname, id, mappedStatus, message);
                    if (host != null) {
                        host.setReservedBy(reservedBy);
                        host.setTestPath(testPath);
                    }

                } catch (JSONException e) {
                    LOGGER.log(Level.WARNING, "Error parsing resource at index " + i, e);
                }
            }

            LOGGER.info("Successfully processed " + resourceCount + " resources");

            // Check for missing base hosts and mark them as OFFLINE
            // This also checks if we have the expected count for each hostname
            if (!baseHosts.isEmpty()) {
                LOGGER.fine("Checking " + baseHosts.size() + " base hosts for missing hosts");

                // Count how many instances of each hostname we received
                java.util.Map<String, Integer> receivedCounts = new java.util.HashMap<>();
                for (String hostname : reportedHostnames) {
                    receivedCounts.put(hostname, receivedCounts.getOrDefault(hostname, 0) + 1);
                }

                for (String baseHost : baseHosts) {
                    int expectedCount = manager.getExpectedCount(baseHost);
                    int receivedCount = receivedCounts.getOrDefault(baseHost, 0);

                    if (receivedCount == 0) {
                        // No instances received - mark as OFFLINE
                        LOGGER.fine("Base host not reported, marking as OFFLINE: " + baseHost);
                        manager.updateHost(baseHost, "OFFLINE", "Not found in Autotest Registry");
                    } else if (receivedCount < expectedCount) {
                        // Fewer instances than expected - create OFFLINE placeholders for missing ones
                        LOGGER.fine("Base host " + baseHost + " has " + receivedCount + " instances, expected " + expectedCount);

                        for (int i = receivedCount; i < expectedCount; i++) {
                            // Create placeholder OFFLINE host with synthetic id
                            String syntheticId = baseHost + "-missing-" + (i + 1);
                            manager.updateHost(baseHost, syntheticId, "OFFLINE", "Instance not found in Autotest Registry");
                        }
                    }
                }
            }

            // Clean up old WS hosts that weren't in this message
            // This happens at the END to avoid showing incomplete list during processing
            List<MonitoredHost> currentHosts = manager.getHosts();
            for (MonitoredHost host : currentHosts) {
                String id = host.getId();
                String hostname = host.getHostname();

                // Remove if it has an ID (from WS), is not in base list, and was NOT in this message
                if (id != null && !id.isEmpty() &&
                    !baseHosts.contains(hostname) &&
                    !reportedHostIds.contains(id)) {
                    String key = id;
                    manager.removeHost(key);
                    LOGGER.fine("Removed old WS host not in current message: " + hostname + " (id: " + id + ")");
                }
            }

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
     * Convert Python dict string to valid JSON string
     * Handles: single quotes -> double quotes, True -> true, False -> false, None -> null
     */
    private String convertPythonDictToJson(String pythonDict) {
        String result = pythonDict;

        // Replace Python literals with JSON equivalents
        // Use word boundaries to avoid replacing inside strings
        result = result.replaceAll("\\bNone\\b", "null");
        result = result.replaceAll("\\bTrue\\b", "true");
        result = result.replaceAll("\\bFalse\\b", "false");

        // Replace single quotes with double quotes
        // This works for most cases where strings don't contain quotes
        result = result.replace("'", "\"");

        return result;
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
                return "IDLE";
            case "BUSY":
                return "BUSY";
            case "ERROR":
                return "ERROR";
            case "OFFLINE":
                return "OFFLINE";
            case "WAIT":
                return "WAIT";
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
     * Extract username from reservation field
     * Format: "Yes (username)" -> "username"
     *         "No" -> null
     */
    private String extractReservedBy(String reservation) {
        if (reservation == null || reservation.trim().isEmpty()) {
            return null;
        }

        String res = reservation.trim();

        if (res.equalsIgnoreCase("No")) {
            return null;
        } else if (res.toLowerCase().startsWith("yes")) {
            // Extract user from "Yes (username)" format
            int start = res.indexOf('(');
            int end = res.indexOf(')');
            if (start != -1 && end != -1 && end > start) {
                return res.substring(start + 1, end);
            }
        }

        return null;
    }

    /**
     * Process test path to show only the last two path segments
     * Example: /jenkins/tmp/ldb2-server-8/workspace/Maintenance/Check_test_pool/build-debug/test/net/link/_nios/test_net_link.exe
     *       -> _nios/test_net_link.exe
     */
    private String processTestPath(String testPath) {
        if (testPath == null || testPath.isEmpty()) {
            return null;
        }

        // Find the last slash
        int lastSlash = testPath.lastIndexOf('/');
        if (lastSlash == -1) {
            // No slash found, return original
            return testPath;
        }

        // Find the second-to-last slash
        int secondLastSlash = testPath.lastIndexOf('/', lastSlash - 1);
        if (secondLastSlash == -1) {
            // Only one slash found, return from the beginning
            return testPath;
        }

        // Return everything from second-to-last slash + 1 to the end
        return testPath.substring(secondLastSlash + 1);
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
