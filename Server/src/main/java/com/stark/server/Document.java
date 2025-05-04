package com.stark.server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import treeCRDT.CrdtNode;
import treeCRDT.CrdtTree;
import treeCRDT.CrdtTree.TreeChange;
import treeCRDT.CrdtTree.TreeChangeListener;
import treeCRDT.NodeId;


@Getter
public class Document {
    private final String documentId;
    private final String editorCode;
    private final String viewerCode;
    private final int creatorUserId;
    private final CrdtTree crdt;
    private final List<Integer> connectedUsers;
    private final int maxUsers = 4;

    /**
     * Creates a new document with the given ID
     * @param documentId The document ID
     * @param creatorUserId The ID of the user who created the document
     */
    public Document(String documentId, int creatorUserId, String editorCode, String viewerCode) {
        this.documentId = documentId;
        this.creatorUserId = creatorUserId;
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
        this.crdt = new CrdtTree();
        this.connectedUsers = new ArrayList<>(maxUsers);
        // Automatically connect the creator
        connectUser(creatorUserId);
    }

    /**
     * Creates a new document with the given ID
     * @param documentId The document ID
     */
    public Document(String documentId) {
        this(documentId, 1, null, null);
    }

    /**
     * Connects a user to the document if there's space available
     * @param userId User ID (1-4)
     * @return true if user was connected, false if document is full
     */
    public boolean connectUser(int userId) {
        if (connectedUsers.size() >= maxUsers) {
            return false;
        }

        if (userId < 1 || userId > 4) {
            return false;
        }

        if (connectedUsers.contains(userId)) {
            return true;
        }

        connectedUsers.add(userId);
        return true;
    }

    /**
     * Disconnects a user from the document
     * @param userId User ID to disconnect
     * @return true if user was disconnected, false if user wasn't connected
     */
    public boolean disconnectUser(int userId) {
        return connectedUsers.remove(Integer.valueOf(userId));
    }

    /**
     * Inserts text at the root node
     * @param userId User ID making the change
     * @param text Text to insert
     * @return true if insertion was successful
     */
    public boolean insertText(int userId, String text) {
        if (!connectedUsers.contains(userId)) {
            return false;
        }

        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            // Get the root node to use as parent for all characters
            CrdtNode parent = crdt.getRoot();

            // Insert all characters as children of the root node
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                NodeId nodeId = new NodeId(userId, new Timestamp(timestamp.getTime() + i)); // Ensure unique timestamps
                CrdtNode node = new CrdtNode(nodeId, c);

                // Add to CRDT as a child of the parent
                crdt.addChild(parent.getId(), node);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Inserts text after a specific node
     * @param userId User ID making the change
     * @param afterNodeId NodeId to insert after (null to insert at beginning)
     * @param text Text to insert
     * @return true if insertion was successful
     */
    public boolean insertTextAfterNode(int userId, NodeId afterNodeId, String text) {
        if (!connectedUsers.contains(userId)) {
            return false;
        }

        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());

            // Get the parent node - either the specified node or the root if null
            CrdtNode parent;
            if (afterNodeId == null) {
                parent = crdt.getRoot();
            } else {
                parent = crdt.getNodeIndex().get(afterNodeId);
                if (parent == null) {
                    parent = crdt.getRoot();
                }
            }

            // Insert all characters as children of the specified parent node
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                NodeId nodeId = new NodeId(userId, new Timestamp(timestamp.getTime() + i)); // Ensure unique timestamps
                CrdtNode node = new CrdtNode(nodeId, c);

                // Add to CRDT as a child of the parent
                crdt.addChild(parent.getId(), node);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Deletes text by marking nodes as deleted
     * @param userId User ID making the change
     * @param nodeIds List of NodeIds to mark as deleted
     * @return true if deletion was successful
     */
    public boolean deleteText(int userId, List<NodeId> nodeIds) {
        if (!connectedUsers.contains(userId)) {
            return false;
        }

        try {
            Map<NodeId, CrdtNode> nodeIndex = crdt.getNodeIndex();

            // Mark nodes as deleted
            for (NodeId nodeId : nodeIds) {
                CrdtNode node = nodeIndex.get(nodeId);
                if (node != null) {
                    node.setDeleted(true);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the current text from the CRDT
     * @return The current text
     */
    public String getText() {
        return crdt.getText();
    }

    /**
     * Gets the number of connected users
     * @return Number of connected users
     */
    public int getConnectedUserCount() {
        return connectedUsers.size();
    }

    /**
     * Checks if a user is connected to the document
     * @param userId User ID to check
     * @return true if user is connected
     */
    public boolean isUserConnected(int userId) {
        return connectedUsers.contains(userId);
    }

    /**
     * Gets the node index map from the CRDT
     * @return Map of NodeId to CrdtNode
     */
    public Map<NodeId, CrdtNode> getNodeIndex() {
        return crdt.getNodeIndex();
    }

    /**
     * Gets all nodes in the document as a map
     * @return Map of NodeId to CrdtNode
     */
    public Map<NodeId, CrdtNode> getAllNodes() {
        return crdt.getNodeIndex();
    }

    /**
     * Checks if the document has any content
     * @return true if the document has content, false if it's empty
     */
    public boolean hasContent() {
        return !getText().isEmpty();
    }

    /**
     * Gets the character count of the document
     * @return Number of characters in the document
     */
    public int getCharacterCount() {
        return getText().length();
    }

    /**
     * Adds a listener for document changes
     * @param listener The listener to add
     */
    public void addChangeListener(TreeChangeListener listener) {
        crdt.addChangeListener(listener);
    }

    /**
     * Gets the changes since the last update
     * @return List of changes
     */
    public List<TreeChange> getChangesSinceLastUpdate() {
        return crdt.getChangesSinceLastUpdate();
    }

    /**
     * Gets a node by its ID
     * @param nodeId The ID of the node to get
     * @return The node with the specified ID, or null if not found
     */
    public CrdtNode getNodeById(NodeId nodeId) {
        return crdt.getNodeIndex().get(nodeId);
    }

    /**
     * Prints the CRDT tree structure to the console (for debugging)
     */
    public void printCrdtTree() {
        crdt.printCrdtTree();
    }
}
