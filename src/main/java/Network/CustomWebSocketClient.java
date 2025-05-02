package Network;

import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.function.Consumer;

public class CustomWebSocketClient extends WebSocketClient {
    private final Gson gson = new Gson();
    private Consumer<Operation> operationHandler;
    private Consumer<String> errorHandler;
    private String lastMessage;

    public CustomWebSocketClient(String serverUri, Consumer<Operation> operationHandler, Consumer<String> errorHandler) {
        super(URI.create(serverUri));
        this.operationHandler = operationHandler;
        this.errorHandler = errorHandler;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        System.out.println("Connected to server");
    }

    @Override
    public void onMessage(String message) {
        lastMessage = message;
        if (message.startsWith("SESSION_CREATED:") || message.startsWith("VALID_SESSION:") || message.equals("INVALID_CODE")) {
            return; // Handled by getLastMessage
        }
        try {
            Operation operation = gson.fromJson(message, Operation.class);
            operationHandler.accept(operation);
        } catch (Exception e) {
            errorHandler.accept("Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected: " + reason);
        errorHandler.accept("Connection closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        errorHandler.accept("WebSocket error: " + ex.getMessage());
    }

    public void sendOperation(Operation operation) {
        if (isOpen()) {
            try {
                String json = gson.toJson(operation);
                send(json);
            } catch (Exception e) {
                errorHandler.accept("Error sending operation: " + e.getMessage());
            }
        }
    }

    public String getLastMessage() {
        return lastMessage;
    }
}