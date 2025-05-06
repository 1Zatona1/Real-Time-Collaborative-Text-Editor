package com.stark.server;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.DestinationVariable;

import java.sql.Timestamp;


@Controller
public class WebSocketController {
    private final SessionService sessionService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SessionService sessionService, SimpMessagingTemplate messagingTemplate) {
        this.sessionService = sessionService;
        this.messagingTemplate = messagingTemplate;
    }

    private Operation parseOperation(String str) {
        String[] parts = str.split(",!!", -1); // -1 to include trailing empty strings
        Operation op = new Operation();
        op.setType(parts[0]);
        op.setPosition(Integer.parseInt(parts[1]));
        op.setTextChanged(parts[2]);
        op.setUserId(Integer.parseInt(parts[3]));
        op.setTimestamp(Timestamp.valueOf(parts[4]));
        return op;
    }

    @MessageMapping("/update/{sessionId}")
    public void updateDocument(@DestinationVariable String sessionId, @Payload String operationStr) {
        // Add the operation to the session history
        System.out.println("ana geet ya basha");
        Operation operation = parseOperation(operationStr);
        boolean success = sessionService.addOperation(sessionId, operation);
        if (success) {
            // Broadcast the operation to all clients subscribed to this session
            messagingTemplate.convertAndSend("/topic/document/" + sessionId, operationStr);

            System.out.println("Broadcast operation: " + operationStr);
        } else {
            System.err.println("Failed to add operation - session not found: " + sessionId);
        }
    }

    @MessageMapping("/userEvent/{sessionId}")
    public void userEvent(@DestinationVariable String sessionId, @Payload String eventStr) {
        String[] parts = eventStr.split(",", -1);
        UserEvent event = new UserEvent(parts[0], Integer.parseInt(parts[1]), parts[2]);
        sessionService.handleUserEvent(sessionId, event);

        messagingTemplate.convertAndSend("/topic/userEvent/" + sessionId, eventStr);
    }

}
