package com.stark.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SessionService sessionService;

    @InjectMocks
    private DocumentRestController documentRestController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(documentRestController).build();
    }

    @Test
    void testCreateDocument() throws Exception {
        // Arrange
        String expectedResponse = "session-id,E-12345678,V-87654321";
        when(sessionService.createDocument()).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedResponse));
    }

    @Test
    void testGetDocumentState_ValidEditorCode() throws Exception {
        // Arrange
        String code = "E-12345678";
        String sessionId = "session-id";
        
        // Create sample operations
        Operation op1 = new Operation("insert", "Hello", 1, Timestamp.from(Instant.now()), 0);
        Operation op2 = new Operation("insert", " World", 1, Timestamp.from(Instant.now()), 5);
        List<Operation> operations = Arrays.asList(op1, op2);
        
        // Expected response format
        List<String> expectedResponse = Arrays.asList(
            "insert,0,Hello,1," + op1.getTimestamp().toString(),
            "insert,5, World,1," + op2.getTimestamp().toString()
        );
        
        when(sessionService.validateCode(code)).thenReturn(SessionService.CodeType.EDITOR);
        when(sessionService.getSessionIdByCode(code)).thenReturn(sessionId);
        when(sessionService.getDocumentState(sessionId)).thenReturn(operations);

        // Act & Assert
        mockMvc.perform(get("/api/documents/{code}/state", code))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testGetDocumentState_ValidViewerCode() throws Exception {
        // Arrange
        String code = "V-87654321";
        String sessionId = "session-id";
        
        // Create sample operations
        List<Operation> operations = Arrays.asList(
            new Operation("insert", "Hello", 1, Timestamp.from(Instant.now()), 0)
        );
        
        when(sessionService.validateCode(code)).thenReturn(SessionService.CodeType.VIEWER);
        when(sessionService.getSessionIdByCode(code)).thenReturn(sessionId);
        when(sessionService.getDocumentState(sessionId)).thenReturn(operations);

        // Act & Assert
        mockMvc.perform(get("/api/documents/{code}/state", code))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testGetDocumentState_InvalidCode() throws Exception {
        // Arrange
        String code = "invalid-code";
        
        when(sessionService.validateCode(code)).thenReturn(SessionService.CodeType.INVALID);

        // Act & Assert
        mockMvc.perform(get("/api/documents/{code}/state", code))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetDocumentState_SessionNotFound() throws Exception {
        // Arrange
        String code = "E-12345678";
        String sessionId = "session-id";
        
        when(sessionService.validateCode(code)).thenReturn(SessionService.CodeType.EDITOR);
        when(sessionService.getSessionIdByCode(code)).thenReturn(sessionId);
        when(sessionService.getDocumentState(sessionId)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/documents/{code}/state", code))
                .andExpect(status().isNotFound());
    }
}