package com.stark.server;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    // In-memory store of sessions (use a service or DB in production)
    private Map<String, DocumentSession> sessions = new ConcurrentHashMap<>();

    // Create a new document session (generate IDs and codes)
    @PostMapping
    public CreateSessionResponse createDocument() {
        String sessionId = UUID.randomUUID().toString();
        String editCode = UUID.randomUUID().toString().substring(0, 8);  // short random code
        String viewCode = UUID.randomUUID().toString().substring(0, 8);
        DocumentSession session = new DocumentSession(sessionId, "", editCode, viewCode);
        sessions.put(sessionId, session);
        return new CreateSessionResponse(sessionId, editCode, viewCode);
    }

    // Retrieve the current document state (CRDT content) for a session
    @GetMapping("/{sessionId}/state")
    public DocumentState getDocumentState(@PathVariable String sessionId) {
        DocumentSession session = sessions.get(sessionId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        // Return the current text (or CRDT state) to sync a new user
        return new DocumentState(sessionId, session.getContent());
    }
}

class DocumentState {
    private String sessionId, content;
    // constructor, getters...
}

class DocumentSession {
    private String sessionId;
    private String content;         // current document text/CRDT state
    private String editCode;        // code for editors
    private String viewCode;        // code for viewers
    private Set<Integer> activeEditors = new HashSet<>();
    // constructor, getters, setters...
}

