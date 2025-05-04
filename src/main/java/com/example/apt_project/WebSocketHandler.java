package com.example.apt_project;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.Collections;
import java.util.List;

public class WebSocketHandler {
    StompSession stompSession;

    public void connectToWebSocket() {
        try {
            List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
            SockJsClient sockJsClient = new SockJsClient(transports);

            WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

            MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
            converter.getObjectMapper().findAndRegisterModules();
            stompClient.setMessageConverter(converter);

            String url = "ws://localhost:8080/ws";
            MyStompSessionHandler sessionHandler = new MyStompSessionHandler();
            stompSession = stompClient.connectAsync(url, sessionHandler).get();

            System.out.println("Connected to WebSocket server");
        } catch (Exception e) {
            System.out.println("Websocket connection failed: " + e.getMessage());
        }
    }


}

class MyStompSessionHandler extends StompSessionHandlerAdapter {
    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        System.err.println("An error occurred: " + exception.getMessage());
    }
}
