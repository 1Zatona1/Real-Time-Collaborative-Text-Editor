package treeCRDT;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class CrdtNode implements Comparable<CrdtNode> {
    private NodeId id;
    private char value;
    private boolean deleted;
    private TreeSet<CrdtNode> children;
    @Override
    public int compareTo(CrdtNode other) {
        int timestampComparison = other.getId().getClock().compareTo(this.getId().getClock()); // Newest first
        if (timestampComparison != 0) {
            return timestampComparison;
        }
        return Integer.compare(other.getId().getUserId(), this.getId().getUserId()); // Higher userId first
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CrdtNode)) return false;
        CrdtNode other = (CrdtNode) obj;
        return this.id.equals(other.id); // NodeId handles equals
    }

    @Override
    public int hashCode() {
        return id.hashCode(); // NodeId handles hash
    }
    public CrdtNode(NodeId id, char value) {
        this.id = id;
        this.value = value;
        this.deleted = false;
        this.children = new TreeSet<CrdtNode>();
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
        return new ArrayList<>(children); // Optional: return as list
    }

    public void addChild(CrdtNode child) {
        this.children.add(child); // Sorted automatically
    }
    public void traverse(CrdtNode node) {
        if (node == null) {
            return;
        }

        // Process the current node (e.g., print or store the value)
        System.out.println("Node value: " + node.getValue() + ", ID: " + node.getId());

        // Traverse children in the sorted order (already done by TreeSet)
        for (CrdtNode child : node.getChildren()) {
            if (!child.isDeleted()) {  // Skip deleted nodes
                traverse(child);  // Recursive call to visit the next level
            }
        }
    }

}