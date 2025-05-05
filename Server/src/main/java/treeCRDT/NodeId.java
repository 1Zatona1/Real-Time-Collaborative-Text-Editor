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
        return "Id{" +
                "id=" + userId +
                ", timestamp='" + clock + '\'' +
                '}';
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