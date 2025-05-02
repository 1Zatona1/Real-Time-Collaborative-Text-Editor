package com.example.apt_project;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class TextEditorWebSocketClient extends WebSocketClient {


    int id=0;

    public interface MessageHandler {
        void onMessageReceived(String message);
    }

    private MessageHandler handler;

    public TextEditorWebSocketClient(URI serverUri, MessageHandler handler) {
        super(serverUri);
        this.handler = handler;
    }

    @Override
    public void onOpen(ServerHandshake handshakeData) {
        id++;
        System.out.println("Connected to WebSocket Server " + this.id );
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
        System.out.println(this.id +"Disconnected from WebSocket Server");
        id--;
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}
