package com.stark.server;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StompSessionService {
    private final Map<String, Set<String>> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionRoles = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public StompSessionService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        String editorCode = "E-" + UUID.randomUUID().toString().substring(0, 8);
        String viewerCode = "V-" + UUID.randomUUID().toString().substring(0, 8);
        sessions.put(sessionId, ConcurrentHashMap.newKeySet());
        sessionRoles.put(editorCode, sessionId);
        sessionRoles.put(viewerCode, sessionId);
        return editorCode + "," + viewerCode + "," + sessionId;
    }

    public boolean addUser(String sessionId, String userId, String role) {
        Set<String> sessionUsers = sessions.getOrDefault(sessionId, ConcurrentHashMap.newKeySet());
        if (sessionUsers.size() >= 4) {
            return false;
        }
        sessionUsers.add(userId);
        sessions.put(sessionId, sessionUsers);
        return true;
    }

    public void removeUser(String sessionId, String userId) {
        Set<String> sessionUsers = sessions.get(sessionId);
        if (sessionUsers != null) {
            sessionUsers.remove(userId);
            if (sessionUsers.isEmpty()) {
                sessions.remove(sessionId);
            }
        }
    }

    public void broadcastOperation(String sessionId, Operation operation) {
        Message message = new Message("operation", sessionId, operation);
        messagingTemplate.convertAndSend("/topic/session/" + sessionId, message);
    }

    public String validateSessionCode(String code) {
        return sessionRoles.getOrDefault(code, null);
    }

    public void broadcastText(String sessionId, String text, String excludeUserId) {
        Message message = new Message(text, false);
        message.setType("text");
        message.setSessionId(sessionId);
        messagingTemplate.convertAndSend("/topic/session/" + sessionId, message);
    }

    public Set<String> getSessionUsers(String sessionId) {
        return sessions.getOrDefault(sessionId, ConcurrentHashMap.newKeySet());
    }

    public boolean isUserInSession(String sessionId, String userId) {
        Set<String> sessionUsers = sessions.get(sessionId);
        return sessionUsers != null && sessionUsers.contains(userId);
    }
}