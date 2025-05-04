package com.stark.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class StompRestController
{

    private final DocumentService documentService;
    private final StompSessionService sessionService;

    @Autowired
    public StompRestController(DocumentService documentService, StompSessionService sessionService) {
        this.documentService = documentService;
        this.sessionService = sessionService;
    }

    /**
     * Create a new document
     * @return Document information including ID, editor code, and viewer code
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> createDocument()
    {
        String sessionInfo = sessionService.createSession();
        String[] parts = sessionInfo.split(",");

        if (parts.length != 3)
        {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        String editorCode = parts[0];
        String viewerCode = parts[1];
        String documentId = parts[2];

        // Create a document with the generated ID
        Document document = documentService.getDocument(documentId);

        Map<String, String> response = new HashMap<>();
        response.put("documentId", documentId);
        response.put("editorCode", editorCode);
        response.put("viewerCode", viewerCode);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get the text content of a document
     * @param documentId Document ID
     * @return Document text content
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<Map<String, String>> getDocumentText(@PathVariable String documentId)
    {
        if (!documentService.documentExists(documentId)) {
            return ResponseEntity.notFound().build();
        }

        String text = documentService.getDocumentText(documentId);

        Map<String, String> response = new HashMap<>();
        response.put("documentId", documentId);
        response.put("text", text);

        return ResponseEntity.ok(response);
    }

    /**
     * Validate a session code (editor or viewer code) and return the corresponding document ID
     * @param code Session code to validate
     * @return Document ID if the code is valid, 404 if not
     */
    @GetMapping("/validate/{code}")
    public ResponseEntity<Map<String, String>> validateSessionCode(@PathVariable String code)
    {
        String documentId = sessionService.validateSessionCode(code);
        if (documentId == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, String> response = new HashMap<>();
        response.put("documentId", documentId);
        response.put("isEditor", code.startsWith("E-") ? "true" : "false");

        return ResponseEntity.ok(response);
    }
}
