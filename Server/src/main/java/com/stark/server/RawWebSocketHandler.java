//package com.stark.server;
//
//import com.google.gson.Gson;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//import treeCRDT.CrdtTree;
//
//import java.io.IOException;
//import java.util.Map;
//import Network.Message;
//
//@Component
//public class RawWebSocketHandler extends TextWebSocketHandler {
//    private final SessionService sessionService;
//    private final DocumentService documentService;
//    private final Gson gson = new Gson();
//
//    // Map to store CRDT trees for each session (for backward compatibility)
//    private final Map<String, CrdtTree> sessionCrdtTrees;
//
//    @Autowired
//    public RawWebSocketHandler(SessionService sessionService, DocumentService documentService, Map<String, CrdtTree> sessionCrdtTrees) {
//        this.sessionService = sessionService;
//        this.documentService = documentService;
//        this.sessionCrdtTrees = sessionCrdtTrees;
//    }
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        System.out.println("New WebSocket connection established: " + session.getId());
//
//        // Extract session code from URI query parameters
//        String uri = session.getUri().toString();
//        String sessionCode = null;
//        if (uri.contains("?code=")) {
//            sessionCode = uri.substring(uri.indexOf("?code=") + 6);
//            System.out.println("Session code from URI: " + sessionCode);
//
//            // Validate the session code
//            String sessionId = sessionService.validateSessionCode(sessionCode);
//            if (sessionId != null) {
//                // Add the user to the session
//                String role = sessionCode.startsWith("E-") ? "EDITOR" : "VIEWER";
//                boolean added = sessionService.addUser(sessionId, session, role);
//                if (added) {
//                    System.out.println("User added to session " + sessionId + " with role " + role);
//
//                    // Send the current document state to the new user
//                    if (documentService.documentExists(sessionId)) {
//                        String currentText = documentService.getDocumentText(sessionId);
//                        System.out.println("Sending current document state to new user: " + currentText.substring(0, Math.min(20, currentText.length())) + "...");
//                        session.sendMessage(new TextMessage(currentText));
//                    } else {
//                        // Get or create a Document instance for this session
//                        Document document = documentService.getDocument(sessionId);
//
//                        // Create a new CRDT tree for backward compatibility
//                        sessionCrdtTrees.put(sessionId, documentService.getCrdtTree(sessionId));
//                    }
//                } else {
//                    System.out.println("Failed to add user to session " + sessionId + " (session might be full)");
//                    session.close();
//                }
//            } else {
//                System.out.println("Invalid session code: " + sessionCode);
//                session.close();
//            }
//        } else {
//            System.out.println("No session code provided in URI");
//            // Allow connection without session code for CREATE_SESSION requests
//        }
//    }
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
//        System.out.println("WebSocket connection closed: " + session.getId() + " with status: " + status);
//
//        // Extract session code from URI query parameters
//        String uri = session.getUri().toString();
//        String sessionCode = null;
//        if (uri.contains("?code=")) {
//            sessionCode = uri.substring(uri.indexOf("?code=") + 6);
//
//            // Get the session ID from the code
//            String sessionId = sessionService.validateSessionCode(sessionCode);
//            if (sessionId != null) {
//                // Remove the user from the session
//                sessionService.removeUser(sessionId, session);
//                System.out.println("User removed from session " + sessionId);
//            }
//        }
//    }
//
//    @Override
//    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
//        System.err.println("Transport error for session " + session.getId() + ": " + exception.getMessage());
//        exception.printStackTrace();
//
//        // Close the session if it's still open
//        if (session.isOpen()) {
//            session.close(org.springframework.web.socket.CloseStatus.SERVER_ERROR);
//        }
//
//        // Extract session code from URI query parameters
//        String uri = session.getUri().toString();
//        String sessionCode = null;
//        if (uri.contains("?code=")) {
//            sessionCode = uri.substring(uri.indexOf("?code=") + 6);
//
//            // Get the session ID from the code
//            String sessionId = sessionService.validateSessionCode(sessionCode);
//            if (sessionId != null) {
//                // Remove the user from the session
//                sessionService.removeUser(sessionId, session);
//                System.out.println("User removed from session " + sessionId + " due to transport error");
//            }
//        }
//    }
//
//    @Override
//    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
//        String payload = message.getPayload();
//        System.out.println("Received message: " + payload);
//
//        // Try to parse as JSON Message
//        try {
//            Message jsonMessage = gson.fromJson(payload, Message.class);
//            if (jsonMessage != null && jsonMessage.getType() != null) {
//                handleStructuredMessage(session, jsonMessage);
//                return;
//            }
//        } catch (Exception e) {
//            // Not a JSON message, continue with legacy handling
//        }
//
//        // Legacy message handling
//        if (payload.equals("CREATE_SESSION")) {
//            String sessionInfo = sessionService.createSession();
//            session.sendMessage(new TextMessage("SESSION_CREATED:" + sessionInfo));
//        } else if (payload.startsWith("VALIDATE_CODE:")) {
//            String code = payload.substring("VALIDATE_CODE:".length());
//            String sessionId = sessionService.validateSessionCode(code);
//            if (sessionId != null) {
//                session.sendMessage(new TextMessage("VALID_SESSION:" + sessionId));
//                sessionService.addUser(sessionId, session, code.startsWith("E-") ? "EDITOR" : "VIEWER");
//            } else {
//                session.sendMessage(new TextMessage("INVALID_CODE"));
//            }
//        } else {
//            try {
//                // Extract session code from URI query parameters
//                String uri = session.getUri().toString();
//                String sessionCode = null;
//                if (uri.contains("?code=")) {
//                    sessionCode = uri.substring(uri.indexOf("?code=") + 6);
//                }
//
//                if (sessionCode != null) {
//                    String sessionId = sessionService.validateSessionCode(sessionCode);
//                    if (sessionId != null) {
//                        // Broadcast the text message to all clients in the session, excluding the sender
//                        sessionService.broadcastText(sessionId, payload, session);
//
//                        // Handle text change using the DocumentService
//                        documentService.handleTextChange(sessionId, 0, payload); // Using default user ID 0 for now
//
//                        // Update the CRDT tree for backward compatibility
//                        sessionCrdtTrees.put(sessionId, documentService.getCrdtTree(sessionId));
//
//                        System.out.println("Updated Document for session " + sessionId + " with text: " + payload.substring(0, Math.min(20, payload.length())) + "...");
//                    }
//                }
//            } catch (Exception e) {
//                System.err.println("Error processing message: " + e.getMessage());
//                e.printStackTrace();
//            }
//        }
//    }
//
//
//    private void handleStructuredMessage(WebSocketSession session, Message message) throws IOException {
//        String type = message.getType();
//        String sessionId = message.getSessionId();
//        String content = message.getContent();
//
//        if ("TEXT_UPDATE".equals(type) && sessionId != null && content != null) {
//            // Extract session code from URI query parameters
//            String uri = session.getUri().toString();
//            String sessionCode = null;
//            if (uri.contains("?code=")) {
//                sessionCode = uri.substring(uri.indexOf("?code=") + 6);
//            }
//
//            if (sessionCode != null) {
//                String validatedSessionId = sessionService.validateSessionCode(sessionCode);
//                if (validatedSessionId != null && validatedSessionId.equals(sessionId)) {
//                    // Create a structured message response
//                    Message response = new Message("TEXT_UPDATE", sessionId, content);
//                    String jsonResponse = gson.toJson(response);
//
//                    // Broadcast the structured message to all clients in the session, excluding the sender
//                    sessionService.broadcastText(sessionId, jsonResponse, session);
//
//                    // Handle text change using the DocumentService
//                    documentService.handleTextChange(sessionId, 0, content); // Using default user ID 0 for now
//
//                    // Update the CRDT tree for backward compatibility
//                    sessionCrdtTrees.put(sessionId, documentService.getCrdtTree(sessionId));
//
//                    System.out.println("Updated Document for session " + sessionId + " with text: " + content.substring(0, Math.min(20, content.length())) + "...");
//                }
//            }
//        } else if ("CREATE_SESSION".equals(type)) {
//            String sessionInfo = sessionService.createSession();
//            Message response = new Message("SESSION_CREATED", "", sessionInfo);
//            session.sendMessage(new TextMessage(gson.toJson(response)));
//        } else if ("VALIDATE_CODE".equals(type) && content != null) {
//            String code = content;
//            String validatedSessionId = sessionService.validateSessionCode(code);
//            if (validatedSessionId != null) {
//                Message response = new Message("VALID_SESSION", validatedSessionId, "");
//                session.sendMessage(new TextMessage(gson.toJson(response)));
//                sessionService.addUser(validatedSessionId, session, code.startsWith("E-") ? "EDITOR" : "VIEWER");
//            } else {
//                Message response = new Message("INVALID_CODE", "", "");
//                session.sendMessage(new TextMessage(gson.toJson(response)));
//            }
//        }
//    }
//}
