//package com.stark.server;
//
//import com.google.gson.Gson;
//import com.stark.server.Operation;
//import com.stark.server.SessionService;
//import com.stark.server.DocumentService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.CloseStatus;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.io.IOException;
//import java.util.Map;
//import java.util.HashMap;
//import java.util.concurrent.ConcurrentHashMap;
//
//
//@Component
//public class WebSocketController extends TextWebSocketHandler
//{
//    private final SessionService sessionService;
//    private final DocumentService documentService;
//    private final Gson gson = new Gson();
//    private final Map<String, String> sessionIds = new ConcurrentHashMap<>();
//
//    @Autowired
//    public WebSocketController(SessionService sessionService, DocumentService documentService) {
//        this.sessionService = sessionService;
//        this.documentService = documentService;
//    }
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        // Connection established, but we'll wait for the join message to associate with a session
//    }
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        String payload = message.getPayload();
//
//        try {
//            Map<String, Object> jsonMap = gson.fromJson(payload, Map.class);
//            String type = (String) jsonMap.get("type");
//            String sessionId = (String) jsonMap.get("sessionId");
//
//            // Store the session ID for this WebSocket session
//            if (sessionId != null) {
//                sessionIds.put(session.getId(), sessionId);
//            }
//
//            switch (type) {
//                case "join":
//                    handleJoinSession(session, sessionId, (String) jsonMap.get("role"));
//                    break;
//                case "insert":
//                    handleInsertCharacter(session, sessionId, jsonMap);
//                    break;
//                case "delete":
//                    handleDeleteCharacter(session, sessionId, jsonMap);
//                    break;
//                default:
//                    // Unknown message type
//                    break;
//            }
//        } catch (Exception e) {
//            System.out.println("Error handling message: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
//        // Get the session ID for this WebSocket session
//        String sessionId = sessionIds.remove(session.getId());
//
//        if (sessionId != null) {
//            // Remove the user from the session
//            sessionService.removeUser(sessionId, session);
//
//            // Broadcast user left message
//            Map<String, Object> userLeftMessage = new HashMap<>();
//            userLeftMessage.put("type", "userLeft");
//            userLeftMessage.put("sessionId", sessionId);
//            userLeftMessage.put("userId", session.getId());
//
//            String json = gson.toJson(userLeftMessage);
//            sessionService.broadcastText(sessionId, json, session);
//        }
//    }
//
//    private void handleJoinSession(WebSocketSession session, String sessionId, String role) {
//        // Add user to the session
//        boolean added = sessionService.addUser(sessionId, session, role);
//
//        if (added) {
//            try {
//                // Send confirmation to the user
//                Map<String, Object> joinConfirmation = new HashMap<>();
//                joinConfirmation.put("type", "joinConfirmed");
//                joinConfirmation.put("sessionId", sessionId);
//                joinConfirmation.put("userId", session.getId());
//
//                session.sendMessage(new TextMessage(gson.toJson(joinConfirmation)));
//
//                // Broadcast user joined message to other users
//                Map<String, Object> userJoinedMessage = new HashMap<>();
//                userJoinedMessage.put("type", "userJoined");
//                userJoinedMessage.put("sessionId", sessionId);
//                userJoinedMessage.put("userId", session.getId());
//
//                String json = gson.toJson(userJoinedMessage);
//                sessionService.broadcastText(sessionId, json, session);
//            } catch (IOException e) {
//                System.out.println("Error sending join confirmation: " + e.getMessage());
//            }
//        }
//    }
//
//    private void handleInsertCharacter(WebSocketSession session, String sessionId, Map<String, Object> jsonMap) {
//        try {
//            // Extract operation details
//            double positionDouble = (double) jsonMap.get("position");
//            int position = (int) positionDouble;
//            String character = (String) jsonMap.get("character");
//
//            if (character != null && !character.isEmpty()) {
//                // Create an operation
//                Operation operation = new Operation();
//                operation.setType("insert");
//                operation.setPosition(position);
//                operation.setCharacter(character.charAt(0));
//
//                // Broadcast the operation to all users in the session
//                sessionService.broadcastOperation(sessionId, operation);
//            }
//        } catch (Exception e) {
//            System.out.println("Error handling insert: " + e.getMessage());
//        }
//    }
//
//    private void handleDeleteCharacter(WebSocketSession session, String sessionId, Map<String, Object> jsonMap) {
//        try {
//            // Extract operation details
//            double positionDouble = (double) jsonMap.get("position");
//            int position = (int) positionDouble;
//
//            // Create an operation
//            Operation operation = new Operation();
//            operation.setType("delete");
//            operation.setPosition(position);
//
//            // Broadcast the operation to all users in the session
//            sessionService.broadcastOperation(sessionId, operation);
//        } catch (Exception e) {
//            System.out.println("Error handling delete: " + e.getMessage());
//        }
//    }
//}
