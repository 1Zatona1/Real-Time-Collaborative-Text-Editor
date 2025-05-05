package com.stark.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WebSocketControllerTest {

    @Mock
    private SessionService sessionService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketController webSocketController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUpdateDocument_Success() {
        // Arrange
        String sessionId = "test-session-id";
        String operationStr = "insert,0,Hello,1," + Timestamp.from(Instant.now());
        when(sessionService.addOperation(eq(sessionId), any(Operation.class))).thenReturn(true);

        // Act
        webSocketController.updateDocument(sessionId, operationStr);

        // Assert
        verify(sessionService).addOperation(eq(sessionId), any(Operation.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/document/" + sessionId), any(Operation.class));
    }

    @Test
    void testUpdateDocument_Failure() {
        // Arrange
        String sessionId = "invalid-session-id";
        String operationStr = "insert,0,Hello,1," + Timestamp.from(Instant.now());
        when(sessionService.addOperation(eq(sessionId), any(Operation.class))).thenReturn(false);

        // Act
        webSocketController.updateDocument(sessionId, operationStr);

        // Assert
        verify(sessionService).addOperation(eq(sessionId), any(Operation.class));
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/document/" + sessionId), any(Operation.class));
    }

    @Test
    void testUserEvent() {
        // Arrange
        String sessionId = "test-session-id";
        String eventStr = "join,1,editor";

        // Act
        webSocketController.userEvent(sessionId, eventStr);

        // Assert
        verify(sessionService).handleUserEvent(eq(sessionId), any(UserEvent.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/userEvent/" + sessionId), eq(eventStr));
    }
}