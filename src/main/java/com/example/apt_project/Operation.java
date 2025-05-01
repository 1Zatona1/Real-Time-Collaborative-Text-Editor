package com.example.apt_project;

import treeCRDT.NodeId;

import java.util.List;

public class Operation {
    private String type;
    private NodeId nodeId;
    private Character value;
    private NodeId parentId;
    private String userId;
    private String action;
    private String commentId;
    private String commentText;
    private int position;
    private List<String> users;

    // Constructor for insert/delete operations
    public Operation(String type, NodeId nodeId, Character value, NodeId parentId, String userId) {
        this.type = type;
        this.nodeId = nodeId;
        this.value = value;
        this.parentId = parentId;
        this.userId = userId;
    }

    // Constructor for comment operations
    public Operation(String type, NodeId nodeId, Character value, NodeId parentId, String userId, String action, String commentId, String commentText, int position) {
        this.type = type;
        this.nodeId = nodeId;
        this.value = value;
        this.parentId = parentId;
        this.userId = userId;
        this.action = action;
        this.commentId = commentId;
        this.commentText = commentText;
        this.position = position;
    }

    // Constructor for cursor updates
    public Operation(String type, String userId, int position) {
        this.type = type;
        this.userId = userId;
        this.position = position;
    }

    // Constructor for user updates
    public Operation(String type, List<String> users) {
        this.type = type;
        this.users = users;
    }

    public String getType() {
        return type;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public Character getValue() {
        return value;
    }

    public NodeId getParentId() {
        return parentId;
    }

    public String getUserId() {
        return userId;
    }

    public String getAction() {
        return action;
    }

    public String getCommentId() {
        return commentId;
    }

    public String getCommentText() {
        return commentText;
    }

    public int getPosition() {
        return position;
    }

    public List<String> getUsers() {
        return users;
    }
}