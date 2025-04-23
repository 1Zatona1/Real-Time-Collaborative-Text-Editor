package com.example.apt_project;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class TextEditorWebsokcet extends WebSocketClient {

    public interface MessageHandler {
        void onMessageReceived(String message);
    }

    private MessageHandler handler;

    public TextEditorWebsokcet(URI serverUri, MessageHandler handler) {
        super(serverUri);
        this.handler = handler;
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        System.out.println("Connected to WebSocket Server");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received: " + message);
        if (handler != null) {
            handler.onMessageReceived(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected from WebSocket Server");
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}
