package com.stark.server;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentRestController {
    private final SessionService sessionService;
    public DocumentRestController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    // Create a new document session (generate IDs and codes)
    @PostMapping
    public Session createDocument() {
        return sessionService.createDocument();
    }

    // Retrieve the current document state (CRDT content) for a session
    @GetMapping("/{sessionId}/state")
    public List<Operation> getDocumentState(@PathVariable String sessionId) {
        List<Operation> operations = sessionService.getDocumentState(sessionId);
        if (operations == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        return operations;
    }
}
