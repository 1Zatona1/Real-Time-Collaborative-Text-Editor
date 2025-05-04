package com.stark.server;

import org.fxmisc.richtext.model.PlainTextChange;
import org.reactfx.Change;
import treeCRDT.NodeId;
import java.sql.Timestamp;

public class Operation {
    private String type;
    private Timestamp timestamp;
    private int position;
    private int userId;
    private String textChanged;


    public Operation() {}

    public Operation(String type, Timestamp timestamp, int position, int userId) {
        this.type = type;
        this.timestamp = timestamp;
        this.userId = userId;
        this.position = position;
        this.textChanged = textChanged;
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

}