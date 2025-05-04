package com.stark.server;

import com.stark.server.Operation;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionService {
    private final Map<String, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionRoles = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        String editorCode = "E-" + UUID.randomUUID().toString().substring(0, 8);
        String viewerCode = "V-" + UUID.randomUUID().toString().substring(0, 8);
        sessions.put(sessionId, new ConcurrentHashMap<>());
        sessionRoles.put(editorCode, sessionId);
        sessionRoles.put(viewerCode, sessionId);
        return editorCode + "," + viewerCode + "," + sessionId;
    }

    public boolean addUser(String sessionId, WebSocketSession session, String role) {
        Map<String, WebSocketSession> sessionUsers = sessions.getOrDefault(sessionId, new ConcurrentHashMap<>());
        if (sessionUsers.size() >= 4) {
            return false;
        }
        sessionUsers.put(session.getId(), session);
        sessions.put(sessionId, sessionUsers);
        return true;
    }

    public void removeUser(String sessionId, WebSocketSession session) {
        Map<String, WebSocketSession> sessionUsers = sessions.get(sessionId);
        if (sessionUsers != null) {
            sessionUsers.remove(session.getId());
            if (sessionUsers.isEmpty()) {
                sessions.remove(sessionId);
            }
        }
    }

    public void broadcastOperation(String sessionId, Operation operation) {
        Map<String, WebSocketSession> sessionUsers = sessions.get(sessionId);
        if (sessionUsers != null) {
            String json = gson.toJson(operation);
            for (WebSocketSession userSession : sessionUsers.values()) {
                try {
                    if (userSession.isOpen()) {
                        userSession.sendMessage(new TextMessage(json));
                    }
                } catch (IOException e) {
                    System.out.println("Error broadcasting to " + userSession.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    public String validateSessionCode(String code) {
        return sessionRoles.getOrDefault(code, null);
    }

    public void broadcastText(String sessionId, String text, WebSocketSession excludeSession) {
        Map<String, WebSocketSession> sessionUsers = sessions.get(sessionId);
        if (sessionUsers != null) {
            for (WebSocketSession userSession : sessionUsers.values()) {
                try {
                    if (userSession.isOpen() && !userSession.getId().equals(excludeSession.getId())) {
                        userSession.sendMessage(new TextMessage(text));
                    }
                } catch (IOException e) {
                    System.out.println("Error broadcasting to " + userSession.getId() + ": " + e.getMessage());
                }
            }
        }
    }
}
