package treeCRDT;

import java.util.ArrayList;
import java.util.List;

public class CrdtNode {
    private NodeId id;
    private char value;
    private boolean deleted;
    private List<CrdtNode> children;

    public CrdtNode(NodeId id, char value) {
        this.id = id;
        this.value = value;
        this.deleted = false;
        this.children = new ArrayList<CrdtNode>();
    }

    public NodeId getId() {
        return id;
    }

    public char getValue() {
        return value;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public void setValue(char value) {
        this.value = value;
    }

    public void setId(NodeId id) {
        this.id = id;
    }

    public List<CrdtNode> getChildren() {
        return children;
    }

    public void addChild(CrdtNode child) {
        this.children.add(child);

        this.children.sort((node1, node2) -> {
            int timestampComparison = node2.getId().getClock().compareTo(node1.getId().getClock());
            if (timestampComparison != 0) {
                return timestampComparison;
            }
            return Integer.compare(node2.getId().getUserId(), node1.getId().getUserId());
        });
    }
}
