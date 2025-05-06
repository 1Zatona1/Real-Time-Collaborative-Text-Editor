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
    public String createDocument() {
        return sessionService.createDocument();
    }

    // Retrieve the current document state (CRDT content) for a session
    @GetMapping("/{code}/state")
    public List<String> getDocumentState(@PathVariable String code) {
        SessionService.CodeType type = sessionService.validateCode(code);
        if (type != SessionService.CodeType.INVALID) {
            // Handle valid code
            // --> 2. send to the user that invoked this function the recent list of operations and type
            String sessionId = sessionService.getSessionIdByCode(code);
            List<Operation> operations = sessionService.getDocumentState(sessionId);
            if (operations == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
            }
            return operations.stream()
                    .map(op -> String.join(",!!",
                            op.getType(),
                            String.valueOf(op.getPosition()),
                            op.getTextChanged() == null ? "" : op.getTextChanged(),
                            String.valueOf(op.getUserId()),
                            op.getTimestamp().toString()
                    ))
                    .toList();
        }
        else{
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
    }

    @GetMapping("/{code}/editors/count")
    public int getEditorsCount(@PathVariable String code) {
        SessionService.CodeType type = sessionService.validateCode(code);
        if (type != SessionService.CodeType.INVALID) {
            String sessionId = sessionService.getSessionIdByCode(code);
            return sessionService.getEditorsCount(sessionId);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
    }

    @GetMapping("/{code}/getid")
    public String getDocumentId(@PathVariable String code) {
        SessionService.CodeType type = sessionService.validateCode(code);
        if (type != SessionService.CodeType.INVALID) {
            String sessionId = sessionService.getSessionIdByCode(code);
            return sessionId;
        }
        return null;
    }

}
