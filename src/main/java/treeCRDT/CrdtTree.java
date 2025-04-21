package treeCRDT;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class CrdtTree {
    private CrdtNode root;
    private  final Map<NodeId, CrdtNode> nodeIndex = new HashMap<>();
    private int count;

    public CrdtTree() {
        this.count = 0;

        // Create a root node with userId=0 and timestamp=0
        this.root = new CrdtNode(new NodeId(0, Timestamp.valueOf("1970-01-01 00:00:00")), '\u0000');
        nodeIndex.put(root.getId(), root);
    }

    public CrdtNode getRoot() {
        return root;
    }

    public void setRoot(CrdtNode root) {
        this.root = root;
    }
    public synchronized void addChild(NodeId parentId, CrdtNode child) {
        CrdtNode parent = nodeIndex.get(parentId);

        if (parent == null && parentId.getUserId() == 0) {
            parent = root;
        }

        if (parent != null) {
            parent.addChild(child);
            nodeIndex.put(child.getId(), child); // Index the new child
            count++;
        }
    }
    public int getCount() {
        return count;

    }
    public void traverse() {
        // Start traversal from root's children
        for (CrdtNode child : root.getChildren()) {
            traverse(child); // Traverse each child of the root
        }
    }


    // Recursive method to traverse from a given node
    private void traverse(CrdtNode node) {
        if (node == null || node.isDeleted()) {
            return; // Skip if the node is null or marked as deleted
        }

        // Process current node (print or do any necessary operation)
        System.out.println("Node Value: " + node.getValue() + ", ID: " + node.getId());

        // Traverse children nodes (TreeSet ensures sorted order by timestamp and userId)
        for (CrdtNode child : node.getChildren()) {
            traverse(child); // Recursive call to traverse each child
        }
    }
}
