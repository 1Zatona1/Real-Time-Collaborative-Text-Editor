package com.stark.server;

import com.google.gson.Gson;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SessionService {
    // Store sessions by ID for quick lookup
    private Map<String, Session> sessions = new ConcurrentHashMap<>();

    // Store operations for each session ID
    private Map<String, List<Operation>> sessionOperations = new ConcurrentHashMap<>();

    private final Gson gson = new Gson();

    AtomicInteger sessionCounts=new AtomicInteger(0);

    public Session createDocument() {
        String sessionId = UUID.randomUUID().toString();
        String editCode = UUID.randomUUID().toString().substring(0, 8);  // short random code
        String viewCode = UUID.randomUUID().toString().substring(0, 8);

        Session session = new Session(sessionId, editCode, viewCode);

        sessions.put(sessionId, session);

        List <Operation> operations = new ArrayList<>();
        sessionCounts.getAndIncrement();

        sessionOperations.put(sessionId, operations);
        return session;
    }

    public boolean addOperation(String sessionId, Operation operation) {
        List<Operation> operations = sessionOperations.get(sessionId);
        if (operations != null) {
            operations.add(operation);
            return true;
        }
        return false;
    }

//    public boolean addUser(String sessionId, WebSocketSession session, String role) {
//        Map<String, WebSocketSession> sessionUsers = sessions.getOrDefault(sessionId, new ConcurrentHashMap<>());
//        if (sessionUsers.size() >= 4) {
//            return false;
//        }
//        sessionUsers.put(session.getId(), session);
//        sessions.put(sessionId, sessionUsers);
//        return true;
//    }
//
//    public void removeUser(String sessionId, WebSocketSession session) {
//        Map<String, WebSocketSession> sessionUsers = sessions.get(sessionId);
//        if (sessionUsers != null) {
//            sessionUsers.remove(session.getId());
//            if (sessionUsers.isEmpty()) {
//                sessions.remove(sessionId);
//            }
//        }
//    }
    public List<Operation> getDocumentState(String sessionId) {
        return sessionOperations.getOrDefault(sessionId, new ArrayList<>());
    }
//    public void broadcastOperation(String sessionId, Operation operation) {
//        Map<String, WebSocketSession> sessionUsers = sessions.get(sessionId);
//        if (sessionUsers != null) {
//            String json = gson.toJson(operation);
//            for (WebSocketSession userSession : sessionUsers.values()) {
//                try {
//                    if (userSession.isOpen()) {
//                        userSession.sendMessage(new TextMessage(json));
//                    }
//                } catch (IOException e) {
//                    System.out.println("Error broadcasting to " + userSession.getId() + ": " + e.getMessage());
//                }
//            }
//        }
//    }
}