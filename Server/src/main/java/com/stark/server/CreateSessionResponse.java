package com.stark.server;

public class CreateSessionResponse {
    private String sessionId, editCode, viewCode;

    public CreateSessionResponse(String sessionId, String editCode, String viewCode) {
        this.sessionId = sessionId;
        this.editCode = editCode;
        this.viewCode = viewCode;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getEditCode() {
        return editCode;
    }

    public void setEditCode(String editCode) {
        this.editCode = editCode;
    }

    public String getViewCode() {
        return viewCode;
    }

    public void setViewCode(String viewCode) {
        this.viewCode = viewCode;
    }

    @Override
    public String toString() {
        return "CreateSessionResponse{" +
                "sessionId='" + sessionId + '\'' +
                ", editCode='" + editCode + '\'' +
                ", viewCode='" + viewCode + '\'' +
                '}';
    }

}
