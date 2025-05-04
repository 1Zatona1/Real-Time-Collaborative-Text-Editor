package com.stark.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class StompWebSocketController {

    private final StompSessionService sessionService;
    private final DocumentService documentService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, String> sessionIds = new ConcurrentHashMap<>();

    @Autowired
    public StompWebSocketController(StompSessionService sessionService, DocumentService documentService, SimpMessagingTemplate messagingTemplate) {
        this.sessionService = sessionService;
        this.documentService = documentService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/join/{sessionId}")
    public void handleJoinSession(@DestinationVariable String sessionId, @Payload String role, SimpMessageHeaderAccessor headerAccessor) {
        String userId = headerAccessor.getSessionId();

        // Store the session ID for this WebSocket session
        sessionIds.put(userId, sessionId);

        // Add user to the session
        boolean added = sessionService.addUser(sessionId, userId, role);

        if (added) {
            // Send confirmation to the user
            Message joinConfirmation = new Message("joinConfirmed", sessionId, userId);
            messagingTemplate.convertAndSendToUser(userId, "/queue/reply", joinConfirmation);

            // Broadcast user joined message to other users
            Message userJoinedMessage = new Message("userJoined", sessionId, userId);
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, userJoinedMessage);
        }
    }

    @MessageMapping("/insert/{sessionId}")
    public void handleInsertCharacter(@DestinationVariable String sessionId, @Payload Map<String, Object> payload) {
        try {
            // Extract operation details
            int position = (int) Double.parseDouble(payload.get("position").toString());
            String character = (String) payload.get("character");

            if (character != null && !character.isEmpty()) {
                // Create an operation
                Operation operation = new Operation();
                operation.setType("insert");
                operation.setPosition(position);
                operation.setCharacter(character.charAt(0));

                // Create a message with the operation
                Message message = new Message("insert", sessionId, operation);

                // Broadcast the operation to all users in the session
                messagingTemplate.convertAndSend("/topic/session/" + sessionId, message);
            }
        } catch (Exception e) {
            System.out.println("Error handling insert: " + e.getMessage());
        }
    }

    @MessageMapping("/delete/{sessionId}")
    public void handleDeleteCharacter(@DestinationVariable String sessionId, @Payload Map<String, Object> payload) {
        try {
            // Extract operation details
            int position = (int) Double.parseDouble(payload.get("position").toString());

            // Create an operation
            Operation operation = new Operation();
            operation.setType("delete");
            operation.setPosition(position);

            // Create a message with the operation
            Message message = new Message("delete", sessionId, operation);

            // Broadcast the operation to all users in the session
            messagingTemplate.convertAndSend("/topic/session/" + sessionId, message);
        } catch (Exception e) {
            System.out.println("Error handling delete: " + e.getMessage());
        }
    }

    @MessageMapping("/leave/{sessionId}")
    public void handleLeaveSession(@DestinationVariable String sessionId, SimpMessageHeaderAccessor headerAccessor) {
        String userId = headerAccessor.getSessionId();

        // Remove the session ID for this WebSocket session
        sessionIds.remove(userId);

        // Remove the user from the session
        sessionService.removeUser(sessionId, userId);

        // Broadcast user left message
        Message userLeftMessage = new Message("userLeft", sessionId, userId);
        messagingTemplate.convertAndSend("/topic/session/" + sessionId, userLeftMessage);
    }
}
