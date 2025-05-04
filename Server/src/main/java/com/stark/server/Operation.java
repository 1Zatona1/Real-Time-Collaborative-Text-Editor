package com.stark.server;

import treeCRDT.NodeId;

public class Operation
{
    private String type; // "insert" or "delete"
    private NodeId nodeId;
    private char character;
    private int position;

    public Operation() {}

    public Operation(String type, NodeId nodeId, char character, int position)
    {
        this.type = type;
        this.nodeId = nodeId;
        this.character = character;
        this.position = position;
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
}
