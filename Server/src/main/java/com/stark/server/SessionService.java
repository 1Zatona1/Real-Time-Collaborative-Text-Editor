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

    enum CodeType {
        VIEWER,
        EDITOR,
        INVALID
    };

    public String createDocument() {
        String sessionId = UUID.randomUUID().toString();
        String editCode = "E-" + UUID.randomUUID().toString().substring(0, 8);  // short random code
        String viewCode = "V-" + UUID.randomUUID().toString().substring(0, 8);

        Session session = new Session(sessionId, viewCode, editCode);

        sessions.put(sessionId, session);

        List <Operation> operations = new ArrayList<>();
        sessionCounts.getAndIncrement();

        sessionOperations.put(sessionId, operations);
        return session.getId() + "," + editCode + "," + viewCode;
    }

    public boolean addOperation(String sessionId, Operation operation) {
        List<Operation> operations = sessionOperations.get(sessionId);
        if (operations != null) {
            operations.add(operation);
            return true;
        }
        return false;
    }

    public CodeType validateCode(String code) {
        for (Session session : sessions.values()) {
            if (session.getEditor_code().equals(code)) {
                return CodeType.EDITOR;
            } else if (session.getViewer_code().equals(code)) {
                return CodeType.VIEWER;
            }
        }
        return CodeType.INVALID;
    }

    public String getSessionIdByCode(String code) {
        for (Session session : sessions.values()) {
            if (session.getEditor_code().equals(code) || session.getViewer_code().equals(code)) {
                return session.getId();
            }
        }
        return null;
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

    public void handleUserEvent(String sessionId, UserEvent event) {
        Session session = sessions.get(sessionId);
        if (session != null) {
            if ("join".equals(event.getType())) {
                if ("editor".equals(event.getUserType())) {
                    session.incrementEditorCount();
                } else if ("viewer".equals(event.getUserType())) {
                    session.incrementViewerCount();
                }
            } else if ("leave".equals(event.getType())) {
                if ("editor".equals(event.getUserType())) {
                    session.decrementEditorCount();
                } else if ("viewer".equals(event.getUserType())) {
                    session.decrementViewerCount();
                }
            }
        }
    }

    public int getEditorsCount(String sessionId) {
        Session session = sessions.get(sessionId);
        return session != null ? session.getEditor_count() : 0;
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
