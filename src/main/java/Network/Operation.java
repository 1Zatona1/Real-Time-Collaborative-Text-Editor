package Network;

import treeCRDT.NodeId;
import java.sql.Timestamp;

public class Operation {
    private String type; // "insert" or "delete"
    private NodeId nodeId;
    private NodeId parentNodeId; // Added for CrdtTree compatibility
    private char character;
    private int position;
    private String textChanged; // Added for CrdtTree compatibility
    private int userId; // Added for CrdtTree compatibility
    private Timestamp timestamp; // Added for CrdtTree compatibility

    public Operation() {}

    public Operation(String type, NodeId nodeId, char character, int position) {
        this.type = type;
        this.nodeId = nodeId;
        this.character = character;
        this.position = position;
    }

    // Added constructor for CrdtTree compatibility
    public Operation(String type, Timestamp timestamp, int position, int userId, String textChanged, NodeId nodeId, NodeId parentNodeId) {
        this.type = type;
        this.timestamp = timestamp;
        this.position = position;
        this.userId = userId;
        this.textChanged = textChanged;
        this.nodeId = nodeId;
        this.parentNodeId = parentNodeId;
        if (textChanged != null && !textChanged.isEmpty()) {
            this.character = textChanged.charAt(0);
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public void setNodeId(NodeId nodeId) {
        this.nodeId = nodeId;
    }

    public char getCharacter() {
        return character;
    }

    public void setCharacter(char character) {
        this.character = character;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    // Added getters and setters for CrdtTree compatibility
    public NodeId getParentNodeId() {
        return parentNodeId;
    }

    public void setParentNodeId(NodeId parentNodeId) {
        this.parentNodeId = parentNodeId;
    }

    public String getTextChanged() {
        return textChanged;
    }

    public void setTextChanged(String textChanged) {
        this.textChanged = textChanged;
        if (textChanged != null && !textChanged.isEmpty()) {
            this.character = textChanged.charAt(0);
        }
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
