package com.stark.server;

import treeCRDT.NodeId;
import java.sql.Timestamp;

public class Operation {
    private String type; // "insert" or "delete"
    private int position;
    private String textChanged; // Added for CrdtTree compatibility
    private int userId; // Added for CrdtTree compatibility
    private Timestamp timestamp; // Added for CrdtTree compatibility

    public Operation() {}

    public Operation(String type, String textChanged, int userId, Timestamp timestamp, int position) {
        this.type = type;
        this.textChanged = textChanged;
        this.userId = userId;
        this.position = position;
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getTextChanged() {
        return textChanged;
    }

    public int getUserId() {
        return userId;
    }

    public void setTextChanged(String textChanged) {
        this.textChanged = textChanged;
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
