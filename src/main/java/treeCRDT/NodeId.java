package treeCRDT;

import java.sql.Timestamp;

class NodeId {
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
}
