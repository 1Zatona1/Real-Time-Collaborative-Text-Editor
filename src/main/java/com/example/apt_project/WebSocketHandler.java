package com.example.apt_project;

import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import java.lang.reflect.Type;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WebSocketHandler {
    StompSession stompSession;

    public StompSession getStompSession() {
        return stompSession;
    }

    public void connectToWebSocket() {
        try {
            // WebSocket transport
            List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
            SockJsClient sockJsClient = new SockJsClient(transports);

            // WebSocket Stomp client
            WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);
            List<MessageConverter> converters = new ArrayList<>();
            converters.add(new StringMessageConverter());
            stompClient.setMessageConverter(new CompositeMessageConverter(converters));
            // Connect to WebSocket server
            String url = "ws://localhost:8080/ws"; // WebSocket endpoint
            StompSessionHandler sessionHandler = new MyStompSessionHandler();
            stompSession = stompClient.connectAsync(url, sessionHandler).get();

        } catch (Exception e) {
            System.out.println("WebSocket connection failed: " + e.getMessage());
        }
    }

    public void updateDocument(String sessionId, String Change) {
        if (stompSession != null && stompSession.isConnected()) {
            try {
                String destination = "/app/update/" + sessionId ;
                stompSession.send(destination, Change);
            } catch (Exception e) {
                System.out.println("Failed to send document update: " + e.getMessage());
            }
        } else {
            System.out.println("Not connected to WebSocket server");
        }
    }



    public void close(){
        if (this.stompSession != null) {
            this.stompSession.disconnect();
            System.out.println("Closed WebSocket connection.");
            System.exit(0);
        }
    }



}

class MyStompSessionHandler extends StompSessionHandlerAdapter {
    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        System.out.println("Connected Successfully");
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        System.err.println("An error occurred: " + exception.getMessage());
    }
}
