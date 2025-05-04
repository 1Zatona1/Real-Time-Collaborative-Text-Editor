package com.stark.server;

public class UserEvent {
    private String type; // "join" or "leave"
    private int userId;
    private String userType; // "editor" or "viewer"

    public UserEvent() {}

    public UserEvent(String type, int userId, String userType) {
        this.type = type;
        this.userId = userId;
        this.userType = userType;
    }

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getUserId() { return userId; }
    public void setUsername(int userId) { this.userId = userId; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}
