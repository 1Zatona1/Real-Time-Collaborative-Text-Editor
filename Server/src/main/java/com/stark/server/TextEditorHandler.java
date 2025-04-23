package com.stark.server;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

public class TextEditorHandler extends TextWebSocketHandler 
{

    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        for (WebSocketSession s : sessions) 
        {
            if (!s.getId().equals(session.getId()) && s.isOpen()) 
            {
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
    sessions.remove(session);
}
}
