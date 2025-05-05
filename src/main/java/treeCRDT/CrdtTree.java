package treeCRDT;

import Network.Operation;

import java.sql.Timestamp;
import java.util.*;

public class CrdtTree {
    private CrdtNode root;
    private final Map<NodeId, CrdtNode> nodeIndex = new HashMap<>();
    private int count;
    private List<CrdtNode> lastFlattenedState = new ArrayList<>();
    private Map<Integer, CrdtNode> positionToNodeMap = new HashMap<>();
    private String cachedText;
    private List<TreeChangeListener> listeners = new ArrayList<>();

    private Stack<Operation> undoStack = new Stack<>();
    private Stack<Operation> redoStack = new Stack<>();
    private boolean isRedoing = false;

    public Map<NodeId, CrdtNode> getNodeIndex() {
        return nodeIndex;
    }

    public static class TreeChange {
        public enum Type { INSERT, DELETE }
        public Type type;
        public int position;
        public String text;

        public TreeChange(Type type, int position, String text) {
            this.type = type;
            this.position = position;
            this.text = text;
        }
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

        int minLength = Math.min(lastFlattenedState.size(), currentState.size());

        // Check for modifications in existing nodes
        for (int i = 0; i < minLength; i++) {
            CrdtNode oldNode = lastFlattenedState.get(i);
            CrdtNode newNode = currentState.get(i);

            if (!nodesEqual(oldNode, newNode)) {
                if (newNode.isDeleted()) {
                    changes.add(new TreeChange(TreeChange.Type.DELETE, i, String.valueOf(oldNode.getValue())));
                } else if (oldNode.isDeleted() && !newNode.isDeleted()) {
                    changes.add(new TreeChange(TreeChange.Type.INSERT, i, String.valueOf(newNode.getValue())));
                }
            }
        }

        // Handle additions at the end
        for (int i = minLength; i < currentState.size(); i++) {
            CrdtNode node = currentState.get(i);
            if (!node.isDeleted()) {
                changes.add(new TreeChange(TreeChange.Type.INSERT, i, String.valueOf(node.getValue())));
            }
        }

        // Handle deletions from end
        for (int i = minLength; i < lastFlattenedState.size(); i++) {
            CrdtNode node = lastFlattenedState.get(i);
            changes.add(new TreeChange(TreeChange.Type.DELETE, i, String.valueOf(node.getValue())));
        }

        lastFlattenedState = new ArrayList<>(currentState);
        updatePositionMap(lastFlattenedState);
        cachedText = null; // Invalidate cache
        return changes;
    }

    private boolean nodesEqual(CrdtNode a, CrdtNode b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.getValue() == b.getValue() && // char comparison with == is fine for primitives
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
            nodeIndex.put(child.getId(), child);
            count++;

            // Create and store operation
            Operation op = new Operation(
                    "INSERT",
                    child.getId().getClock(),
                    getPositionForNode(child),
                    child.getId().getUserId(),
                    String.valueOf(child.getValue()),
                    child.getId(),
                    parentId
            );
            undoStack.push(op);
            redoStack.clear(); // New operation invalidates redo history

            // Update state
            lastFlattenedState = flattenTree();
            updatePositionMap(lastFlattenedState);
            cachedText = null; // Invalidate cache
            notifyChanges(getChangesSinceLastUpdate());
        }
    }

    public synchronized void deleteNode(NodeId nodeId, int position, int userId, Timestamp timestamp) {
        CrdtNode node = nodeIndex.get(nodeId);
        if (node != null && !node.isDeleted()) {
            node.setDeleted(true);

            // Create and store operation
            Operation op = new Operation(
                    "DELETE",
                    timestamp,
                    position,
                    userId,
                    String.valueOf(node.getValue()),
                    nodeId,
                    null // No parent for delete
            );
            undoStack.push(op);
            redoStack.clear();

            // Update state
            lastFlattenedState = flattenTree();
            updatePositionMap(lastFlattenedState);
            cachedText = null; // Invalidate cache
            notifyChanges(getChangesSinceLastUpdate());
        }
    }



    private int getPositionForNode(CrdtNode node)
    {
        List<CrdtNode> flattened = flattenTree();
        for (int i = 0; i < flattened.size(); i++)
        {
            if (flattened.get(i).equals(node))
            {
                return i;
            }
        }
        return flattened.size(); // Append at end if not found
    }

    public int getCount() {
        return count;
    }

    public void traverse() {
        for (CrdtNode child : root.getChildren()) {
            traverse(child);
        }
    }

    private void traverse(CrdtNode node) {
        if (node == null || node.isDeleted()) {
            return;
        }
        System.out.println("Node Value: " + node.getValue() + ", ID: " + node.getId());
        for (CrdtNode child : node.getChildren()) {
            traverse(child);
        }
    }

    public void printCrdtTree() {
        System.out.println("CRDT Tree Structure:");
        System.out.println("====================");
        printNode(root, 0, new HashMap<>());
        System.out.println("====================");
    }

    private void printNode(CrdtNode node, int depth, Map<Integer, Boolean>   lastChildMap) {
        if (node == null) return;

        if (depth > 0) {
            StringBuilder prefix = new StringBuilder();
            for (int i = 1; i < depth; i++) {
                prefix.append(lastChildMap.getOrDefault(i, false) ? "    " : "│   ");
            }
            if (depth > 0) {
                prefix.append(lastChildMap.getOrDefault(depth, false) ? "└── " : "├── ");
            }

            System.out.printf("%s%s (User: %d, Time: %s%s)%n",
                    prefix,
                    node.getValue(),
                    node.getId().getUserId(),
                    node.getId().getClock(),
                    node.isDeleted() ? " [DELETED]" : "");
        }

        int childCount = node.getChildren().size();
        for (int i = 0; i < childCount; i++) {
            CrdtNode child = node.getChildren().get(i);
            lastChildMap.put(depth + 1, i == childCount - 1);
            printNode(child, depth + 1, lastChildMap);
        }
    }

    public synchronized void undo()
    {
        if (undoStack.isEmpty())
        {
            System.out.println("[undo] undoStack is empty, nothing to do.");
            return;
        }
        Operation op = undoStack.pop();

        redoStack.push(op); // Enable redo
        System.out.println("[undo] Pushed op back onto redoStack; redoStack size=" + redoStack.size());


        CrdtNode node = nodeIndex.get(op.getNodeId());
        if (node == null) return;

        List<TreeChange> changes = new ArrayList<>();
        if ("INSERT".equals(op.getType()))
        {
            node.setDeleted(true);
            changes.add(new TreeChange(TreeChange.Type.DELETE, op.getPosition(), op.getTextChanged()));
        } else if ("DELETE".equals(op.getType()))
        {
            // Undo delete: restore node
            node.setDeleted(false);
            changes.add(new TreeChange(TreeChange.Type.INSERT, op.getPosition(), op.getTextChanged()));
        }

        // Update state
        lastFlattenedState = flattenTree();
        updatePositionMap(lastFlattenedState);
        cachedText = null; // Invalidate cache
        notifyChanges(changes);
    }

    public synchronized void redo()
    {
        // 1) Bail out if there’s nothing to redo

        try {
            isRedoing = true;

            if (redoStack.isEmpty()) {
                System.out.println("[redo] redoStack is empty, nothing to do.");
                return;
            }

            // 2) Pop the next op and log it
            Operation op = redoStack.pop();
            String type = op.getType() != null ? op.getType().toUpperCase() : "<null>";
            System.out.printf("[redo] Popped op: type=%s, nodeId=%s, parentId=%s, pos=%d, char=%s%n",
                    type,
                    op.getNodeId(),
                    op.getParentNodeId(),
                    op.getPosition(),
                    op.getTextChanged());

            // 3) Apply it
            CrdtNode node = nodeIndex.get(op.getNodeId());
            if (node == null) {
                System.out.println("[redo] Node not found in index – recreating it.");
                node = new CrdtNode(op.getNodeId(), op.getCharacter());
                nodeIndex.put(node.getId(), node);
            }

            switch (type) {
                case "INSERT":
                    // Undelete + ensure it’s attached
                    node.setDeleted(false);
                    CrdtNode parent = nodeIndex.get(op.getParentNodeId());
                    if (parent == null && op.getParentNodeId().getUserId() == 0) {
                        parent = root;
                    }
                    if (parent != null && !parent.getChildren().contains(node))
                    {
                        parent.addChild(node);
                        System.out.println("[redo] Reattached node under parent " + parent.getId());
                    }
                    System.out.println("[redo] Marked node as visible");
                    break;

                case "DELETE":
                    node.setDeleted(true);
                    System.out.println("[redo] Marked node as deleted");
                    break;

                default:
                    System.out.println("[redo] Unknown op type: " + type + "; pushing back and exiting.");
                    redoStack.push(op);
                    return;
            }

            // 4) Push back onto undoStack
            undoStack.push(op);
            System.out.println("[redo] Pushed op back onto undoStack; undoStack size=" + undoStack.size());

            // 5) Refresh state and fire diff
            lastFlattenedState = flattenTree();
            updatePositionMap(lastFlattenedState);
            cachedText = null;
            List<TreeChange> changes = getChangesSinceLastUpdate();
            System.out.println("[redo] Computed changes: " + changes);
            notifyChanges(changes);
            System.out.println("[redo] Done.");


        } finally {
            // ALWAYS reset this flag, even on early return
            isRedoing = false;
        }
    }


}
