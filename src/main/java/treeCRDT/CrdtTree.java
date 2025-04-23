package treeCRDT;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrdtTree {
    private CrdtNode root;
    private  final Map<NodeId, CrdtNode> nodeIndex = new HashMap<>();
    private int count;
    private List<CrdtNode> lastFlattenedState = new ArrayList<>();
    private Map<Integer, CrdtNode> positionToNodeMap = new HashMap<>();
    private String cachedText;
    private List<TreeChangeListener> listeners = new ArrayList<>();



    public static class TreeChange {
        public enum Type { INSERT, DELETE }
        public Type type;
        public int position;
        public String text;
    }

    public interface TreeChangeListener {
        void onTreeChanged(List<TreeChange> changes);
    }

    public void addChangeListener(TreeChangeListener listener) {
        listeners.add(listener);
    }

    private void notifyChanges(List<TreeChange> changes) {
        for (TreeChangeListener listener : listeners) {
            listener.onTreeChanged(changes);
        }
    }

    private void updatePositionMap(List<CrdtNode> nodes) {
        positionToNodeMap.clear();
        for (int i = 0; i < nodes.size(); i++) {
            positionToNodeMap.put(i, nodes.get(i));
        }
    }

    public String getText() {
        if (cachedText == null) {
            StringBuilder sb = new StringBuilder();
            for (CrdtNode node : flattenTree()) {
                if (!node.isDeleted()) {
                    sb.append(node.getValue());
                }
            }
            cachedText = sb.toString();
        }
        return cachedText;
    }

    public List<TreeChange> getChangesSinceLastUpdate() {
        List<CrdtNode> currentState = flattenTree();
        List<TreeChange> changes = new ArrayList<>();

        // Simple diff between last state and current state
        int minLength = Math.min(lastFlattenedState.size(), currentState.size());

        // Check for modifications in existing nodes
        for (int i = 0; i < minLength; i++) {
            CrdtNode oldNode = lastFlattenedState.get(i);
            CrdtNode newNode = currentState.get(i);

            if (!nodesEqual(oldNode, newNode)) {
                if (newNode.isDeleted()) {
                    changes.add(new TreeChange());
                } else if (oldNode.isDeleted() && !newNode.isDeleted()) {
                    changes.add(new TreeChange());
                }
            }
        }

        // Handle additions at the end
        for (int i = minLength; i < currentState.size(); i++) {
            CrdtNode node = currentState.get(i);
            if (!node.isDeleted()) {
                changes.add(new TreeChange());
            }
        }

        // Handle deletions from end
        for (int i = minLength; i < lastFlattenedState.size(); i++) {
            changes.add(new TreeChange());
        }

        lastFlattenedState = new ArrayList<>(currentState);
        return changes;
    }

    private boolean nodesEqual(CrdtNode a, CrdtNode b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.getValue() == b.getValue() &&
                a.isDeleted() == b.isDeleted() &&
                a.getId().equals(b.getId());
    }

    private List<CrdtNode> flattenTree() {
        List<CrdtNode> nodes = new ArrayList<>();
        traverseFlatten(root, nodes);
        return nodes;
    }

    private void traverseFlatten(CrdtNode node, List<CrdtNode> output) {
        if (node == null) return;
        if (node != root) { // Skip root node
            output.add(node);
        }
        for (CrdtNode child : node.getChildren()) {
            traverseFlatten(child, output);
        }
    }

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

    public void printCrdtTree() {
        System.out.println("CRDT Tree Structure:");
        System.out.println("====================");
        printNode(root, 0, new HashMap<>());
        System.out.println("====================");
    }

    private void printNode(CrdtNode node, int depth, Map<Integer, Boolean> lastChildMap) {
        if (node == null) return;

        // Skip printing the root node's value (it's just a placeholder)
        if (depth > 0) {
            // Build the tree prefix
            StringBuilder prefix = new StringBuilder();
            for (int i = 1; i < depth; i++) {
                prefix.append(lastChildMap.getOrDefault(i, false) ? "    " : "│   ");
            }
            if (depth > 0) {
                prefix.append(lastChildMap.getOrDefault(depth, false) ? "└── " : "├── ");
            }

            // Print node information
            System.out.printf("%s%s (User: %d, Time: %s%s)%n",
                    prefix,
                    node.getValue(),
                    node.getId().getUserId(),
                    node.getId().getClock(),
                    node.isDeleted() ? " [DELETED]" : "");
        }

        // Recursively print children
        int childCount = node.getChildren().size();
        for (int i = 0; i < childCount; i++) {
            CrdtNode child = node.getChildren().get(i);
            lastChildMap.put(depth + 1, i == childCount - 1);
            printNode(child, depth + 1, lastChildMap);
        }
    }
}
