//package treeCRDT;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.*;
//
//import java.sql.Timestamp;
//import java.time.Instant;
//
//public class CrdtTreeTest {
//
//    private CrdtTree crdtTree;
//
//    @BeforeEach
//    public void setUp() {
//        crdtTree = new CrdtTree();
//    }
//
//    @Test
//    public void testGetTextEmptyTree() {
//        // A new tree should only have the root node, which is not included in getText
//        assertEquals("", crdtTree.getText());
//    }
//
//    @Test
//    public void testGetTextSingleNode() {
//        // Create a node with character 'A'
//        NodeId nodeId = new NodeId(1, Timestamp.from(Instant.now()));
//        CrdtNode node = new CrdtNode(nodeId, 'A');
//
//        // Add the node to the tree
//        crdtTree.addChild(crdtTree.getRoot().getId(), node);
//
//        // The text should be "A"
//        assertEquals("A", crdtTree.getText());
//    }
//
//    @Test
//    public void testGetTextMultipleNodes() {
//        // Create nodes for "Hello"
//        Timestamp baseTime = Timestamp.from(Instant.now());
//
//        NodeId nodeId1 = new NodeId(1, new Timestamp(baseTime.getTime()));
//        CrdtNode node1 = new CrdtNode(nodeId1, 'H');
//
//        NodeId nodeId2 = new NodeId(1, new Timestamp(baseTime.getTime() + 1));
//        CrdtNode node2 = new CrdtNode(nodeId2, 'e');
//
//        NodeId nodeId3 = new NodeId(1, new Timestamp(baseTime.getTime() + 2));
//        CrdtNode node3 = new CrdtNode(nodeId3, 'l');
//
//        NodeId nodeId4 = new NodeId(1, new Timestamp(baseTime.getTime() + 3));
//        CrdtNode node4 = new CrdtNode(nodeId4, 'l');
//
//        NodeId nodeId5 = new NodeId(1, new Timestamp(baseTime.getTime() + 4));
//        CrdtNode node5 = new CrdtNode(nodeId5, 'o');
//
//        // Add nodes to the tree
//        crdtTree.addChild(crdtTree.getRoot().getId(), node1);
//        crdtTree.addChild(crdtTree.getRoot().getId(), node2);
//        crdtTree.addChild(crdtTree.getRoot().getId(), node3);
//        crdtTree.addChild(crdtTree.getRoot().getId(), node4);
//        crdtTree.addChild(crdtTree.getRoot().getId(), node5);
//
//        // The text should be "olleH" (reverse order due to how nodes are sorted)
//        assertEquals("olleH", crdtTree.getText());
//    }
//
//    @Test
//    public void testGetTextWithDeletedNodes() {
//        // Create nodes for "Hello"
//        Timestamp baseTime = Timestamp.from(Instant.now());
//
//        NodeId nodeId1 = new NodeId(1, new Timestamp(baseTime.getTime()));
//        CrdtNode node1 = new CrdtNode(nodeId1, 'H');
//
//        NodeId nodeId2 = new NodeId(1, new Timestamp(baseTime.getTime() + 1));
//        CrdtNode node2 = new CrdtNode(nodeId2, 'e');
//
//        NodeId nodeId3 = new NodeId(1, new Timestamp(baseTime.getTime() + 2));
//        CrdtNode node3 = new CrdtNode(nodeId3, 'l');
//
//        NodeId nodeId4 = new NodeId(1, new Timestamp(baseTime.getTime() + 3));
//        CrdtNode node4 = new CrdtNode(nodeId4, 'l');
//
//        NodeId nodeId5 = new NodeId(1, new Timestamp(baseTime.getTime() + 4));
//        CrdtNode node5 = new CrdtNode(nodeId5, 'o');
//
//        // Add nodes to the tree
//        crdtTree.addChild(crdtTree.getRoot().getId(), node1);
//        crdtTree.addChild(crdtTree.getRoot().getId(), node2);
//        crdtTree.addChild(crdtTree.getRoot().getId(), node3);
//        crdtTree.addChild(crdtTree.getRoot().getId(), node4);
//        crdtTree.addChild(crdtTree.getRoot().getId(), node5);
//
//        // Mark some nodes as deleted
//        node2.setDeleted(true); // Delete 'e'
//        node4.setDeleted(true); // Delete second 'l'
//
//        // The text should be "olH" (reverse order due to how nodes are sorted, without the deleted 'e' and second 'l')
//        assertEquals("olH", crdtTree.getText());
//    }
//
//    @Test
//    public void testGetTextWithNestedNodes() {
//        // Create a more complex tree structure with nested nodes
//        Timestamp baseTime = Timestamp.from(Instant.now());
//
//        NodeId nodeId1 = new NodeId(1, new Timestamp(baseTime.getTime()));
//        CrdtNode node1 = new CrdtNode(nodeId1, 'A');
//
//        NodeId nodeId2 = new NodeId(1, new Timestamp(baseTime.getTime() + 1));
//        CrdtNode node2 = new CrdtNode(nodeId2, 'B');
//
//        NodeId nodeId3 = new NodeId(1, new Timestamp(baseTime.getTime() + 2));
//        CrdtNode node3 = new CrdtNode(nodeId3, 'C');
//
//        // Add nodes to create a structure
//        crdtTree.addChild(crdtTree.getRoot().getId(), node1);
//        crdtTree.addChild(node1.getId(), node2);
//        crdtTree.addChild(node2.getId(), node3);
//
//        // The text should include all nodes in the flattened order
//        assertEquals("ABC", crdtTree.getText());
//    }
//
//    @Test
//    public void testCachedText() {
//        // Create a node
//        NodeId nodeId = new NodeId(1, Timestamp.from(Instant.now()));
//        CrdtNode node = new CrdtNode(nodeId, 'X');
//
//        // Add the node to the tree
//        crdtTree.addChild(crdtTree.getRoot().getId(), node);
//
//        // First call to getText should compute and cache the result
//        String text1 = crdtTree.getText();
//        assertEquals("X", text1);
//
//        // Mark the node as deleted
//        node.setDeleted(true);
//
//        // Second call should return the cached result, not reflecting the deletion
//        String text2 = crdtTree.getText();
//        assertEquals("X", text2);
//
//        // Note: This test demonstrates that getText() caches its result
//        // and doesn't reflect changes to the tree structure until the cache is invalidated.
//        // In a real application, there should be a method to invalidate the cache
//        // when the tree structure changes.
//    }
//}
