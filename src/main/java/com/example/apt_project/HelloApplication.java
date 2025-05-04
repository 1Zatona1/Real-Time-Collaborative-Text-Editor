package com.example.apt_project;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import treeCRDT.CrdtNode;
import treeCRDT.CrdtTree;
import treeCRDT.NodeId;

import java.io.IOException;
import java.sql.Timestamp;

public class HelloApplication extends Application
{
    @Override
    public void start(Stage stage) throws IOException
    {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        stage.setTitle("Real-Time Collaborative Text Editor");
        stage.setScene(scene);

        stage.show();
        stage.setMaximized(true);
    }

    public static void main(String[] args) {
        launch();

        // Create a CRDT tree and add some nodes for demonstration
        CrdtTree crdtTree = new CrdtTree();

        // Create some nodes with different user IDs and timestamps
        NodeId nodeId1 = new NodeId(1, new Timestamp(System.currentTimeMillis()));
        CrdtNode node1 = new CrdtNode(nodeId1, 'H');

        NodeId nodeId2 = new NodeId(1, new Timestamp(System.currentTimeMillis() + 100));
        CrdtNode node2 = new CrdtNode(nodeId2, 'e');

        NodeId nodeId3 = new NodeId(2, new Timestamp(System.currentTimeMillis() + 200));
        CrdtNode node3 = new CrdtNode(nodeId3, 'l');

        NodeId nodeId4 = new NodeId(2, new Timestamp(System.currentTimeMillis() + 300));
        CrdtNode node4 = new CrdtNode(nodeId4, 'l');

        NodeId nodeId5 = new NodeId(1, new Timestamp(System.currentTimeMillis() + 400));
        CrdtNode node5 = new CrdtNode(nodeId5, 'o');

        // Add nodes to the tree
        crdtTree.addChild(crdtTree.getRoot().getId(), node1);
        crdtTree.addChild(node1.getId(), node2);
        crdtTree.addChild(node2.getId(), node3);
        crdtTree.addChild(node3.getId(), node4);
        crdtTree.addChild(node4.getId(), node5);

        // Print the CRDT tree structure
        System.out.println("\n=== CRDT Tree Structure ===");
        crdtTree.printCrdtTree();

        // Print the text content
        System.out.println("\n=== Text Content ===");
        System.out.println(crdtTree.getText());
    }
}
