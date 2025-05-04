package Network;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

/**
 * Handler for WebSocket communication with the document server using STOMP
 */
public class DocumentWebSocketHandler {
    private StompWebSocketClient client;
    private String documentId;
    private Consumer<String> messageHandler;
    private Consumer<String> errorHandler;
    private boolean connected = false;

    /**
     * Creates a new DocumentWebSocketHandler
     * @param messageHandler Handler for incoming messages
     * @param errorHandler Handler for errors
     */
    public DocumentWebSocketHandler(Consumer<String> messageHandler, Consumer<String> errorHandler) {
        this.messageHandler = messageHandler;
        this.errorHandler = errorHandler;
    }

    /**
     * Connects to the WebSocket server and establishes a STOMP session
     * @param documentId The ID of the document to connect to
     * @return true if connection was successful, false otherwise
     */
    public boolean connect(String documentId) {
        try {
            this.documentId = documentId;
            URI serverUri = new URI(NetworkConfig.SERVER_URL);

            // Create and connect the STOMP client
            client = new StompWebSocketClient(serverUri, this::handleMessage, this::handleError);
            client.connect();

            // Wait for connection to be established
            int attempts = 0;
            while (!client.isOpen() && attempts < 10) {
                try {
                    Thread.sleep(500);
                    attempts++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            if (!client.isOpen()) {
                errorHandler.accept("Failed to connect to WebSocket server");
                return false;
            }

            // Wait for STOMP connection to be established
            attempts = 0;
            while (!client.isStompConnected() && attempts < 10) {
                try {
                    Thread.sleep(500);
                    attempts++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            if (!client.isStompConnected()) {
                errorHandler.accept("Failed to establish STOMP connection");
                return false;
            }

            // Subscribe to document updates
            client.subscribe("/topic/session/" + documentId);

            connected = true;
            return true;
        } catch (URISyntaxException e) {
            errorHandler.accept("Invalid server URI: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inserts text into the document
     * @param text The text to insert
     * @param position The position to insert at
     * @return true if insertion was successful, false otherwise
     */
    public boolean insertText(String text, int position) {
        if (!connected || client == null) {
            errorHandler.accept("Not connected to server");
            return false;
        }

        try {
            client.insertText(documentId, text, position);
            return true;
        } catch (Exception e) {
            errorHandler.accept("Failed to insert text: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes text from the document
     * @param start The start position of the text to delete
     * @param end The end position of the text to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteText(int start, int end) {
        if (!connected || client == null) {
            errorHandler.accept("Not connected to server");
            return false;
        }

        try {
            client.deleteText(documentId, start, end);
            return true;
        } catch (Exception e) {
            errorHandler.accept("Failed to delete text: " + e.getMessage());
            return false;
        }
    }

    /**
     * Disconnects from the WebSocket server
     */
    public void disconnect() {
        if (client != null) {
            if (client.isStompConnected()) {
                client.disconnectStomp();
            }
            client.close();
            connected = false;
        }
    }

    /**
     * Handles incoming messages from the server
     * @param message The message received
     */
    private void handleMessage(String message) {
        messageHandler.accept(message);
    }

    /**
     * Handles errors
     * @param error The error message
     */
    private void handleError(String error) {
        errorHandler.accept(error);
    }

    /**
     * Checks if the client is connected to the server
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected && client != null && client.isOpen() && client.isStompConnected();
    }
}
