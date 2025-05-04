//package com.stark.server;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.RequestBody;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@org.springframework.web.bind.annotation.RestController
//@org.springframework.web.bind.annotation.RequestMapping("/api/documents")
//public class RestController {
//
//    private final DocumentService documentService;
//    private final SessionService sessionService;
//
//    @Autowired
//    public RestController(DocumentService documentService, SessionService sessionService) {
//        this.documentService = documentService;
//        this.sessionService = sessionService;
//    }
//
//    /**
//     * Create a new document
//     * @return Document information including ID, editor code, and viewer code
//     */
//    @org.springframework.web.bind.annotation.PostMapping
//    public ResponseEntity<Map<String, String>> createDocument() {
//        String sessionInfo = sessionService.createSession();
//        String[] parts = sessionInfo.split(",");
//
//        if (parts.length != 3) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//
//        String editorCode = parts[0];
//        String viewerCode = parts[1];
//        String documentId = parts[2];
//
//        // Create a document with the generated ID
//        Document document = documentService.getDocument(documentId);
//
//        Map<String, String> response = new HashMap<>();
//        response.put("documentId", documentId);
//        response.put("editorCode", editorCode);
//        response.put("viewerCode", viewerCode);
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }
//
//    /**
//     * Get the text content of a document
//     * @param documentId Document ID
//     * @return Document text content
//     */
//    @org.springframework.web.bind.annotation.GetMapping("/{documentId}")
//    public ResponseEntity<Map<String, String>> getDocumentText(@org.springframework.web.bind.annotation.PathVariable String documentId) {
//        if (!documentService.documentExists(documentId)) {
//            return ResponseEntity.notFound().build();
//        }
//
//        String text = documentService.getDocumentText(documentId);
//
//        Map<String, String> response = new HashMap<>();
//        response.put("documentId", documentId);
//        response.put("text", text);
//
//        return ResponseEntity.ok(response);
//    }
//}
