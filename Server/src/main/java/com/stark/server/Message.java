package com.stark.server;

// we did this for STOMP zeft controller
// yarab ma ne4ofo tany



public class Message {
    private String content;
    private boolean isComplete;
    private String type;
    private String sessionId;
    private String userId;
    private Operation operation;

    public Message()
    {
    }

    public Message(String content, boolean isComplete)
    {
        this.content = content;
        this.isComplete = isComplete;
    }

    public Message(String type, String sessionId, String userId)
    {
        this.type = type;
        this.sessionId = sessionId;
        this.userId = userId;
    }

    public Message(String type, String sessionId, Operation operation)
    {
        this.type = type;
        this.sessionId = sessionId;
        this.operation = operation;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }
}