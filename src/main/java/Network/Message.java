package Network;

import com.google.gson.annotations.Expose;

public class Message {
    @Expose
    public String type;        // "TEXT_UPDATE", "CREATE_SESSION", "VALIDATE_CODE", etc.
    @Expose
    public String sessionId;   // Session ID
    @Expose
    public String content;     // Message content (text, session info, etc.)

    public Message() {
    }

    public Message(String type, String sessionId, String content) {
        this.type = type;
        this.sessionId = sessionId;
        this.content = content;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
