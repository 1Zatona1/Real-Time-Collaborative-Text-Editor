package com.example.apt_project;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import treeCRDT.CrdtNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class that provides common functionality for the application
 */
public class Utilities {

    /**
     * Generates a unique timestamp for CRDT operations
     * @return A unique timestamp with nanosecond precision
     */
    public static Timestamp getUniqueTimestamp() {
        // Base timestamp
        Timestamp ts = new Timestamp(System.currentTimeMillis());

        // Add nanoTime to handle sub-millisecond operations
        long nanos = System.nanoTime() % 1_000_000;
        ts.setNanos((int)nanos);

        return ts;
    }

    /**
     * Copies text to system clipboard
     * @param text The text to copy
     */
    public static void copyToClipboard(String text) {
        if (text != null && !text.isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
        }
    }

    /**
     * Exports text content to a file
     * @param fileContent The content to export
     * @param file The file to write to
     * @throws IOException If there's an error writing to the file
     */
    public static void exportToFile(String fileContent, File file) throws IOException {
        if (fileContent == null || fileContent.isEmpty() || file == null) {
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(fileContent);
        } catch (IOException e) {
            System.out.println("Error Exporting file: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Creates a StompFrameHandler for document subscription
     * @param sessionId The document session ID
     * @param currentUserId The current user ID
     * @param processInsertOperation Function to process insert operations
     * @param processDeleteOperation Function to process delete operations
     * @param updateUIFromCRDT Function to update UI from CRDT
     * @return A StompFrameHandler instance
     */
    public static StompFrameHandler createDocumentSubscriptionHandler(
            String sessionId,
            int currentUserId,
            DocumentOperationProcessor processor
    ) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                if (payload instanceof String) {
                    String message = payload.toString();
                    String[] parts = message.split(",!!", -1);

                    if (parts.length < 5) return; // Skip invalid messages

                    // Check if this message is from this user
                    int userId = Integer.parseInt(parts[3]);
                    if (userId == currentUserId) return; // Skip our own messages

                    processor.processOperation(parts);
                    System.out.println("Document Update: " + message);
                }
            }
        };
    }

    /**
     * Flattens a CRDT tree starting from a given node
     * @param startNode The node to start flattening from
     * @return A list of flattened nodes
     */
    public static List<CrdtNode> flattenTree(CrdtNode startNode, CrdtNode rootNode) {
        List<CrdtNode> result = new ArrayList<>();
        if (startNode == null) return result;

        // Add this node
        if (startNode != rootNode) {
            result.add(startNode);
        }

        // Add all children recursively
        for (CrdtNode child : startNode.getChildren()) {
            result.addAll(flattenTree(child, rootNode));
        }

        return result;
    }

    /**
     * Builds a string representation of the CRDT tree
     * @param node The current node
     * @param sb The StringBuilder to append to
     */
    public static void traverseAndBuildString(CrdtNode node, StringBuilder sb) {
        if (node == null) return;

        if (!node.isDeleted()) {
            sb.append(node.getValue());
        }

        for (CrdtNode child : node.getChildren()) {
            traverseAndBuildString(child, sb);
        }
    }

    /**
     * Updates position-to-node mapping from the CRDT tree structure
     * @param rootNode The root node of the CRDT tree
     * @param positionToNodeMap The map to update
     */
    public static void updatePositionMapFromTree(CrdtNode rootNode, Map<Integer, CrdtNode> positionToNodeMap) {
        // Get flattened visible nodes from tree
        List<CrdtNode> flatNodes = flattenTree(rootNode, rootNode);

        // Clear and rebuild position map
        positionToNodeMap.clear();
        int visibleIndex = 0;

        for (CrdtNode node : flatNodes) {
            if (!node.isDeleted() && node != rootNode) {
                positionToNodeMap.put(visibleIndex, node);
                visibleIndex++;
            }
        }
    }

    /**
     * Finds the parent node based on position
     * @param position The position to find the parent for
     * @param positionToNodeMap The position-to-node map
     * @param rootNode The root node of the CRDT tree
     * @return The parent node
     */
    public static CrdtNode findParentNode(int position, Map<Integer, CrdtNode> positionToNodeMap, CrdtNode rootNode) {
        if (position == 0) return rootNode;
        for (int i = position - 1; i >= 0; i--) {
            CrdtNode node = positionToNodeMap.get(i);
            if (node != null && !node.isDeleted()) {
                return node;
            }
        }
        return rootNode;
    }

    /**
     * Gets the position for a node in the current view
     * @param targetNode The node to find the position for
     * @param positionToNodeMap The position-to-node map
     * @return The position of the node, or -1 if not found
     */
    public static int getPositionForNode(CrdtNode targetNode, Map<Integer, CrdtNode> positionToNodeMap) {
        // Find position of a node in current view
        for (Map.Entry<Integer, CrdtNode> entry : positionToNodeMap.entrySet()) {
            if (entry.getValue().equals(targetNode)) {
                return entry.getKey();
            }
        }
        return -1; // Not found
    }

    /**
     * Interface for document operation processing
     */
    public interface DocumentOperationProcessor {
        void processOperation(String[] parts);
    }
}