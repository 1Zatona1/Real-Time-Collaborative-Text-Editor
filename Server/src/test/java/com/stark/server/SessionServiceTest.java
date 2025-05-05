package com.stark.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionServiceTest {

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService();
    }

    @Test
    void testCreateDocument() {
        // Act
        String result = sessionService.createDocument();

        // Assert
        assertNotNull(result);
        String[] parts = result.split(",");
        assertEquals(3, parts.length);

        // Verify format: sessionId,editCode,viewCode
        assertFalse(parts[0].isEmpty());
        assertTrue(parts[1].startsWith("E-"));
        assertTrue(parts[2].startsWith("V-"));
    }

    @Test
    void testAddOperation() {
        // Arrange
        String sessionId = sessionService.createDocument().split(",")[0];
        Operation operation = new Operation("insert", "Hello", 1, Timestamp.from(Instant.now()), 0);

        // Act
        boolean result = sessionService.addOperation(sessionId, operation);

        // Assert
        assertTrue(result);

        // Verify operation was added
        List<Operation> operations = sessionService.getDocumentState(sessionId);
        assertEquals(1, operations.size());
        assertEquals("insert", operations.get(0).getType());
        assertEquals("Hello", operations.get(0).getTextChanged());
    }

    @Test
    void testAddOperation_InvalidSessionId() {
        // Arrange
        String invalidSessionId = "invalid-session-id";
        Operation operation = new Operation("insert", "Hello", 1, Timestamp.from(Instant.now()), 0);

        // Act
        boolean result = sessionService.addOperation(invalidSessionId, operation);

        // Assert
        assertFalse(result);
    }

    @Test
    void testValidateCode_EditorCode() {
        // Arrange
        String[] parts = sessionService.createDocument().split(",");
        String editorCode = parts[1];

        // Act
        SessionService.CodeType result = sessionService.validateCode(editorCode);

        // Assert
        assertEquals(SessionService.CodeType.EDITOR, result);
    }

    @Test
    void testValidateCode_ViewerCode() {
        // Arrange
        String[] parts = sessionService.createDocument().split(",");
        String viewerCode = parts[2];

        // Act
        SessionService.CodeType result = sessionService.validateCode(viewerCode);

        // Assert
        assertEquals(SessionService.CodeType.VIEWER, result);
    }

    @Test
    void testValidateCode_InvalidCode() {
        // Arrange
        String invalidCode = "invalid-code";

        // Act
        SessionService.CodeType result = sessionService.validateCode(invalidCode);

        // Assert
        assertEquals(SessionService.CodeType.INVALID, result);
    }

    @Test
    void testGetSessionIdByCode() {
        // Arrange
        String[] parts = sessionService.createDocument().split(",");
        String sessionId = parts[0];
        String editorCode = parts[1];
        String viewerCode = parts[2];

        // Act & Assert
        assertEquals(sessionId, sessionService.getSessionIdByCode(editorCode));
        assertEquals(sessionId, sessionService.getSessionIdByCode(viewerCode));
        assertNull(sessionService.getSessionIdByCode("invalid-code"));
    }

    @Test
    void testGetDocumentState() {
        // Arrange
        String sessionId = sessionService.createDocument().split(",")[0];
        Operation operation1 = new Operation("insert", "Hello", 1, Timestamp.from(Instant.now()), 0);
        Operation operation2 = new Operation("insert", " World", 1, Timestamp.from(Instant.now()), 5);

        sessionService.addOperation(sessionId, operation1);
        sessionService.addOperation(sessionId, operation2);

        // Act
        List<Operation> result = sessionService.getDocumentState(sessionId);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Hello", result.get(0).getTextChanged());
        assertEquals(" World", result.get(1).getTextChanged());
    }

    @Test
    void testHandleUserEvent_Join() {
        // Arrange
        String[] parts = sessionService.createDocument().split(",");
        String sessionId = parts[0];

        // Create a join event for an editor
        UserEvent event = new UserEvent("join", 1, "editor");

        // Act
        sessionService.handleUserEvent(sessionId, event);

        // We can't directly verify the editor count since Session is private
        // But we can verify that the method doesn't throw exceptions
        // In a real application, we would verify the behavior through other public methods
        // or use reflection to access private fields for testing
    }

    @Test
    void testHandleUserEvent_Leave() {
        // Arrange
        String[] parts = sessionService.createDocument().split(",");
        String sessionId = parts[0];

        // Create join and leave events for a viewer
        UserEvent joinEvent = new UserEvent("join", 1, "viewer");
        UserEvent leaveEvent = new UserEvent("leave", 1, "viewer");

        // Act - First join, then leave
        sessionService.handleUserEvent(sessionId, joinEvent);
        sessionService.handleUserEvent(sessionId, leaveEvent);

        // We can't directly verify the viewer count since Session is private
        // But we can verify that the methods don't throw exceptions
        // In a real application, we would verify the behavior through other public methods
        // or use reflection to access private fields for testing
    }
}
