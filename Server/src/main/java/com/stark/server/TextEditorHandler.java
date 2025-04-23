package com.stark.server;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TextEditorHandler extends TextWebSocketHandler {

    // Group users by document code
    private final Map<String, Set<WebSocketSession>> sessionGroups = new ConcurrentHashMap<>();

    // Store shared document text by code
    private final Map<String, String> documentStates = new ConcurrentHashMap<>();

    // Track which code a session belongs to
    private final Map<String, String> sessionToCode = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String code = getCodeFromURI(session.getUri());
        if (code == null) {
            try {
                session.close(CloseStatus.BAD_DATA);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Add to session group
        sessionGroups.computeIfAbsent(code, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToCode.put(session.getId(), code);

        // Send current document state to the new user
        String currentText = documentStates.getOrDefault(code, "");
        try {
            session.sendMessage(new TextMessage(currentText));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String code = sessionToCode.get(session.getId());
        if (code == null) return;

        // Update document state
        documentStates.put(code, message.getPayload());

        // Broadcast to others in the same session
        for (WebSocketSession s : sessionGroups.getOrDefault(code, Collections.emptySet())) {
            if (!s.getId().equals(session.getId()) && s.isOpen()) {
                try {
                    s.sendMessage(message);
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
