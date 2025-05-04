package Network;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A WebSocket client that implements the STOMP protocol for real-time document editing
 */
public class StompWebSocketClient extends WebSocketClient {
    private final Gson gson = new Gson();
    private final Consumer<String> messageHandler;
    private final Consumer<String> errorHandler;
    private String sessionId;
    private boolean connected = false;
    private int messageCounter = 0;

    /**
     * Creates a new STOMP WebSocket client
     * @param serverUri The URI of the WebSocket server
     * @param messageHandler Handler for incoming messages
     * @param errorHandler Handler for errors
     */
    public StompWebSocketClient(URI serverUri, Consumer<String> messageHandler, Consumer<String> errorHandler) {
        super(serverUri);
        this.messageHandler = messageHandler;
        this.errorHandler = errorHandler;
        this.sessionId = UUID.randomUUID().toString();
    }

    /**
     * Called when the WebSocket connection is established
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to WebSocket server");
        // Send STOMP CONNECT frame
        connectStomp();
    }

    /**
     * Called when a message is received from the server
     */
    @Override
    public void onMessage(String message) {
        try {
            // Handle STOMP message
            if (message.startsWith("CONNECTED")) {
                connected = true;
                System.out.println("STOMP connection established");
            } else if (message.startsWith("MESSAGE")) {
                // Extract the message body (after the blank line)
                String[] parts = message.split("\n\n", 2);
                if (parts.length > 1) {
                    String body = parts[1];
                    messageHandler.accept(body);
                }
            } else if (message.startsWith("ERROR")) {
                errorHandler.accept("STOMP error: " + message);
            } else {
                // Handle other message types
                messageHandler.accept(message);
            }
        } catch (Exception e) {
            errorHandler.accept("Error processing message: " + e.getMessage());
        }
    }

    /**
     * Called when the WebSocket connection is closed
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        connected = false;
        System.out.println("Disconnected from WebSocket server: " + reason);
    }

    /**
     * Called when an error occurs
     */
    @Override
    public void onError(Exception ex) {
        errorHandler.accept("WebSocket error: " + ex.getMessage());
    }

    /**
     * Connects to the STOMP server
     */
    private void connectStomp() {
        StringBuilder frame = new StringBuilder();
        frame.append("CONNECT\n");
        frame.append("accept-version:1.2\n");
        frame.append("heart-beat:10000,10000\n");
        frame.append("\n\0");

        send(frame.toString());
    }

    /**
     * Subscribes to a STOMP destination
     * @param destination The destination to subscribe to
     */
    public void subscribe(String destination) {
        if (!connected) {
            errorHandler.accept("Not connected to STOMP server");
            return;
        }

        StringBuilder frame = new StringBuilder();
        frame.append("SUBSCRIBE\n");
        frame.append("id:sub-").append(messageCounter++).append("\n");
        frame.append("destination:").append(destination).append("\n");
        frame.append("\n\0");

        send(frame.toString());
    }

    /**
     * Sends a message to a STOMP destination
     * @param destination The destination to send to
     * @param body The message body
     */
    public void sendMessage(String destination, String body) {
        if (!connected) {
            errorHandler.accept("Not connected to STOMP server");
            return;
        }

        StringBuilder frame = new StringBuilder();
        frame.append("SEND\n");
        frame.append("destination:").append(destination).append("\n");
        frame.append("content-type:application/json\n");
        frame.append("content-length:").append(body.getBytes(StandardCharsets.UTF_8).length).append("\n");
        frame.append("\n");
        frame.append(body);
        frame.append("\0");

        send(frame.toString());
    }

    /**
     * Updates a document by inserting text
     * @param documentId The ID of the document to update
     * @param text The text to insert
     * @param position The position to insert at
     */
    public void insertText(String documentId, String text, int position) {
        if (!connected) {
            errorHandler.accept("Not connected to STOMP server");
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("character", text);
        message.put("position", position);

        String json = gson.toJson(message);
        sendMessage("/app/insert/" + documentId, json);
    }

    /**
     * Updates a document by deleting text
     * @param documentId The ID of the document to update
     * @param start The start position of the text to delete
     * @param end The end position of the text to delete
     */
    public void deleteText(String documentId, int start, int end) {
        if (!connected) {
            errorHandler.accept("Not connected to STOMP server");
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("position", start);

        String json = gson.toJson(message);
        sendMessage("/app/delete/" + documentId, json);
    }

    /**
     * Disconnects from the STOMP server
     */
    public void disconnectStomp() {
        if (!connected) {
            return;
        }

        StringBuilder frame = new StringBuilder();
        frame.append("DISCONNECT\n");
        frame.append("receipt:").append(messageCounter++).append("\n");
        frame.append("\n\0");

        send(frame.toString());
        connected = false;
    }

    /**
     * Checks if the client is connected to the STOMP server
     * @return true if connected, false otherwise
     */
    public boolean isStompConnected() {
        return connected;
    }

    /**
     * Sets the session ID
     * @param sessionId The session ID
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the session ID
     * @return The session ID
     */
    public String getSessionId() {
        return sessionId;
    }
}
