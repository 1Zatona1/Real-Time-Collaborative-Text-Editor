package com.stark.server;

import treeCRDT.NodeId;
import java.sql.Timestamp;

public class Operation {
    private String type;
    private Timestamp timestamp;
    private int position;
    private int userId;
    private String textChanged;
    private NodeId nodeId; // ID of the affected node
    private NodeId parentNodeId; // ID of the parent node (for inserts)

    public Operation() {}

    public Operation(String type, Timestamp timestamp, int position, int userId, String textChanged, NodeId nodeId, NodeId parentNodeId) {
        this.type = type;
        this.timestamp = timestamp;
        this.position = position;
        this.userId = userId;
        this.textChanged = textChanged;
        this.nodeId = nodeId;
        this.parentNodeId = parentNodeId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTextChanged() {
        return textChanged;
    }

    public void setTextChanged(String textChanged) {
        this.textChanged = textChanged;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public void setNodeId(NodeId nodeId) {
        this.nodeId = nodeId;
    }

    public NodeId getParentNodeId() {
        return parentNodeId;
    }

    public void setParentNodeId(NodeId parentNodeId) {
        this.parentNodeId = parentNodeId;
    }
}