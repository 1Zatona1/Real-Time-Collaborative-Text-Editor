package com.stark.server;

import org.springframework.stereotype.Service;
import treeCRDT.CrdtTree;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing Document instances
 */
@Service
public class DocumentService {
    private final Map<String, Document> documents = new ConcurrentHashMap<>();

    /**
     * Get a Document by ID, creating it if it doesn't exist
     * @param documentId The document ID
     * @return The Document instance
     */
    public Document getDocument(String documentId) {
        return documents.computeIfAbsent(documentId, Document::new);
    }

    /**
     * Check if a document exists
     * @param documentId The document ID
     * @return true if the document exists
     */
    public boolean documentExists(String documentId) {
        return documents.containsKey(documentId);
    }

    /**
     * Handle text changes for a document
     * @param documentId The document ID
     * @param userId The user ID making the change
     * @param text The new text content
     * @return true if the change was successful
     */
    public boolean handleTextChange(String documentId, int userId, String text) {
        Document document = getDocument(documentId);

        // Connect user if not already connected
        if (!document.isUserConnected(userId)) {
            document.connectUser(userId);
        }

        // Insert the text
        return document.insertText(userId, text);
    }

    /**
     * Get the CRDT tree for a document
     * @param documentId The document ID
     * @return The CRDT tree
     */
    public CrdtTree getCrdtTree(String documentId) {
        return getDocument(documentId).getCrdt();
    }

    /**
     * Get the text content of a document
     * @param documentId The document ID
     * @return The document text
     */
    public String getDocumentText(String documentId) {
        return getDocument(documentId).getText();
    }
}
