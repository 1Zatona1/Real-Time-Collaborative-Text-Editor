package com.stark.server;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TextEditorHandler extends TextWebSocketHandler {

    // Map of document code → all sessions editing that document
    private final Map<String, Set<WebSocketSession>> sessionGroups = new ConcurrentHashMap<>();

    // Map of document code → current document content
    private final Map<String, String> documentStates = new ConcurrentHashMap<>();

    // Map of session ID → which code it's in
    private final Map<String, String> sessionToCode = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String code = getCodeFromURI(session.getUri());
        if (code == null || code.isEmpty()) {
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Add session to its group
        sessionGroups.computeIfAbsent(code, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToCode.put(session.getId(), code);

        // Send the current document state to the new user
        String currentText = documentStates.getOrDefault(code, "");
        try {
            session.sendMessage(new TextMessage(currentText));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("✅ User joined code: " + code + " | Sessions in room: " + sessionGroups.get(code).size());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String code = sessionToCode.get(session.getId());
        if (code == null) return;

        // Update the document content
        documentStates.put(code, message.getPayload());

        // Broadcast to all other users in the same code group
        for (WebSocketSession peer : sessionGroups.getOrDefault(code, Collections.emptySet())) {
            if (!peer.getId().equals(session.getId()) && peer.isOpen()) {
                try {
                    peer.sendMessage(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String code = sessionToCode.remove(session.getId());
        if (code != null) {
            Set<WebSocketSession> group = sessionGroups.get(code);
            if (group != null) {
                group.remove(session);
                if (group.isEmpty()) {
                    sessionGroups.remove(code);
                    documentStates.remove(code);
                    System.out.println("🗑 Session group deleted for code: " + code);
                }
            }
        }
    }

    private String getCodeFromURI(URI uri) {
        if (uri == null || uri.getQuery() == null) return null;
        String[] params = uri.getQuery().split("&");
        for (String param : params) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals("code")) {
                return pair[1];
            }
        }
        return null;
    }
}
