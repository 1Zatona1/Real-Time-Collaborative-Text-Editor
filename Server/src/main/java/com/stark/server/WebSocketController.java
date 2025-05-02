package com.stark.server;

import com.stark.server.Operation;
import com.stark.server.SessionService;
import com.google.gson.Gson;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketController extends TextWebSocketHandler {
    private final SessionService sessionService;
    private final Gson gson = new Gson();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public WebSocketController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String uri = session.getUri().toString();

        // Check if this is a connection for creating a new session or validating a code
        if (uri.contains("?") && uri.substring(uri.indexOf("?")).startsWith("?code=")) {
            // This is a connection with a session code, proceed normally
            String sessionId = extractSessionId(session);
            String role = extractRole(session);

            if (sessionId.equals("default")) {
                // This is likely a connection for creating a new session or validating a code
                // Just store the session for now, we'll handle the actual session creation/validation in handleTextMessage
                sessions.put(session.getId(), session);
                System.out.println("User connected for session creation/validation: " + session.getId());
            } else if (sessionService.addUser(sessionId, session, role)) {
                sessions.put(session.getId(), session);
                System.out.println("User connected: " + session.getId() + " to session " + sessionId);
            } else {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Session full or invalid"));
            }
        } else {
            // This is a connection without a session code, just store the session
            sessions.put(session.getId(), session);
            System.out.println("User connected without session code: " + session.getId());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        if (payload.equals("CREATE_SESSION")) {
            String sessionInfo = sessionService.createSession();
            session.sendMessage(new TextMessage("SESSION_CREATED:" + sessionInfo));
            return;
        }

        if (payload.startsWith("VALIDATE_CODE:")) {
            String code = payload.substring("VALIDATE_CODE:".length());
            String sessionId = sessionService.validateSessionCode(code);
            if (sessionId != null) {
                session.sendMessage(new TextMessage("VALID_SESSION:" + sessionId));
            } else {
                session.sendMessage(new TextMessage("INVALID_CODE"));
            }
            return;
        }

        try {
            String sessionId = extractSessionId(session);
            Operation operation = gson.fromJson(payload, Operation.class);
            sessionService.broadcastOperation(sessionId, operation);
        } catch (Exception e) {
            System.out.println("Error processing message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = extractSessionId(session);
        sessionService.removeUser(sessionId, session);
        sessions.remove(session.getId());
        System.out.println("User disconnected: " + session.getId());
    }

    private String extractSessionId(WebSocketSession session) {
        try {
            String uri = session.getUri().toString();
            String query = uri.contains("?") ? uri.substring(uri.indexOf("?") + 1) : "";

            // Check if there's a code parameter in the query string
            if (query.startsWith("code=")) {
                String code = query.substring("code=".length());
                String sessionId = sessionService.validateSessionCode(code);
                if (sessionId != null) {
                    return sessionId;
                }
            }

            // If we couldn't extract a valid session ID from the code, return default
            // Don't fallback to extracting from path as it can cause issues with multiple clients
            return "default"; // Temporary session ID for initial connection
        } catch (Exception e) {
            System.out.println("Error extracting session ID: " + e.getMessage());
            return "default"; // Temporary session ID for initial connection
        }
    }

    private String extractRole(WebSocketSession session) {
        String uri = session.getUri().toString();
        String[] parts = uri.split("/");
        return parts.length > 2 ? parts[parts.length - 2] : "editor"; // e.g., /ws/editor/sessionId
    }
}
