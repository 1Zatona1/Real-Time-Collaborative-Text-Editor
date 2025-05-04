package com.stark.server;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.DestinationVariable;


@Controller
public class WebSocketController {
    private final SessionService sessionService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SessionService sessionService, SimpMessagingTemplate messagingTemplate) {
        this.sessionService = sessionService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/update/{sessionId}")
    public void updateDocument(@DestinationVariable String sessionId, @Payload Operation operation) {
        // Add the operation to the session history
        boolean success = sessionService.addOperation(sessionId, operation);
        if (success) {
            // Broadcast the operation to all clients subscribed to this session
            messagingTemplate.convertAndSend("/topic/document/" + sessionId, operation);

            System.out.println("Broadcast operation: " + operation.getType() +
                    " at position " + operation.getPosition() +
                    " for session " + sessionId);
        } else {
            System.err.println("Failed to add operation - session not found: " + sessionId);
        }
    }


}
