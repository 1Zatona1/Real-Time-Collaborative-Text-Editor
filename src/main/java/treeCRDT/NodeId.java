package treeCRDT;

import java.sql.Timestamp;
import java.util.Objects;

public class NodeId {
    private int userId;
    private Timestamp clock;

    public NodeId(int id, Timestamp timestamp) {
        this.userId = id;
        this.clock = timestamp;
    }

    public int getUserId() {
        return userId;
    }

    public Timestamp getClock() {
        return clock;
    }

    @Override
    public String toString() {
        return userId + ":" + clock.getTime();
    }

    public static NodeId fromString(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        try {
            String[] parts = str.split(":");
            int userId = Integer.parseInt(parts[0]);
            long time = Long.parseLong(parts[1]);
            return new NodeId(userId, new Timestamp(time));
        } catch (Exception e) {
            System.err.println("Error parsing NodeId: " + str);
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NodeId)) return false;
        NodeId other = (NodeId) obj;
        return userId == other.userId && clock.equals(other.clock);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, clock);
    }
}