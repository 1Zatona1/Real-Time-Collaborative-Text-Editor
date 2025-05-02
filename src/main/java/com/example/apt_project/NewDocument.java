package com.example.apt_project;


import Network.CustomWebSocketClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.PlainTextChange;
import treeCRDT.CrdtNode;
import treeCRDT.CrdtTree;
import treeCRDT.NodeId;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.sql.Timestamp;
import java.util.*;


public class NewDocument {
    @FXML
    public VBox sidebar;
    public HBox mainContainer;
    public CodeArea codeArea;
    public String fileContent;
    public Button exportBtn;
    public Button backBtn;
    public Label editorCodeLabel;
    public Label editorCodeText;
    public Label viewerCodeLabel;
    public Label viewerCodeText;
    public Button copyEditorCodeBtn;
    public Button copyViewerCodeBtn;
    private CrdtTree crdtTree = new CrdtTree();
    private int currentUserId = 1; // Or get from authentication
    private Map<Integer, CrdtNode> positionToNodeMap = new HashMap<>();
    private int logicalPositionCounter = 0;
    private TextEditorWebSocketClient webSocketClient;
    private String sessionCode;
    private String editorCode;
    private String viewerCode;
    private String sessionId;
    private boolean ignoreIncoming = false;

    public void setSession(String editorCode, String viewerCode, String sessionId) {
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
        this.sessionId = sessionId;
        // Use the appropriate code for connection - if editorCode is empty, use viewerCode
        this.sessionCode = editorCode.isEmpty() ? viewerCode : editorCode;

        // Update UI
        if (editorCodeText != null) {
            editorCodeText.setText(editorCode);
        }
        if (viewerCodeText != null) {
            viewerCodeText.setText(viewerCode);
        }

        // Connect to WebSocket if not already connected
        if (webSocketClient == null || !webSocketClient.isOpen()) 
        {
            try {
                String uri = "ws://localhost:8080/ws?code=" + sessionCode;
                webSocketClient = new TextEditorWebSocketClient(new URI(uri), msg -> {
                    ignoreIncoming = true;

                    // Clear and rebuild CRDT from received text
                    crdtTree = new CrdtTree();
                    positionToNodeMap.clear();
                    int pos = 0;
                    CrdtNode parent = crdtTree.getRoot();

                    for (char c : msg.toCharArray())
                    {
                        NodeId id = new NodeId(0, new Timestamp(System.currentTimeMillis())); // Remote change
                        CrdtNode node = new CrdtNode(id, c);
                        crdtTree.addChild(parent.getId(), node);
                        positionToNodeMap.put(pos++, node);
                        parent = node;
                    }

                    updateUIFromCRDT(); // Update CodeArea from CRDT
                    ignoreIncoming = false;
                });

                webSocketClient.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setSessionWithExistingConnection(String editorCode, String viewerCode, String sessionId, String uri, CustomWebSocketClient existingClient) {
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
        this.sessionId = sessionId;
        // Use the appropriate code for connection - if editorCode is empty, use viewerCode
        this.sessionCode = editorCode.isEmpty() ? viewerCode : editorCode;

        // Update UI
        if (editorCodeText != null) {
            editorCodeText.setText(editorCode);
        }
        if (viewerCodeText != null) {
            viewerCodeText.setText(viewerCode);
        }

        // Create a new TextEditorWebSocketClient with the same URI
        try {
            // We don't need to close the existing client, just create a new one
            webSocketClient = new TextEditorWebSocketClient(new URI(uri), msg -> {
                ignoreIncoming = true;

                // Clear and rebuild CRDT from received text
                crdtTree = new CrdtTree();
                positionToNodeMap.clear();
                int pos = 0;
                CrdtNode parent = crdtTree.getRoot();

                for (char c : msg.toCharArray()) {
                    NodeId id = new NodeId(0, new Timestamp(System.currentTimeMillis())); // Remote change
                    CrdtNode node = new CrdtNode(id, c);
                    crdtTree.addChild(parent.getId(), node);
                    positionToNodeMap.put(pos++, node);
                    parent = node;
                }

                updateUIFromCRDT(); // Update CodeArea from CRDT
                ignoreIncoming = false;
            });

            webSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @FXML
    public void initialize()
    {
        codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area"); // Add CSS class

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        HBox.setHgrow(codeArea, javafx.scene.layout.Priority.ALWAYS);
        codeArea.prefWidthProperty().bind(mainContainer.widthProperty());

        crdtTree = new CrdtTree();
        positionToNodeMap.clear();

        codeArea.plainTextChanges().subscribe(this::handleTextChange);

        mainContainer.getChildren().add(1, codeArea);

        // If no session has been set yet, use a default one for testing
        if (sessionCode == null)
        {
            // Generate random 6-digit code
            sessionCode = String.valueOf(new Random().nextInt(900000) + 100000);
            //sessionCode = "999999"; // For testing purposes
            editorCode = sessionCode;
            viewerCode = sessionCode;

            if (editorCodeText != null)
            {
                editorCodeText.setText(sessionCode); // Label on screen
            }

            // Connect to WebSocket
            try {
                String uri = "ws://localhost:8080/ws?code=" + sessionCode;
                webSocketClient = new TextEditorWebSocketClient(new URI(uri), msg -> {
                    ignoreIncoming = true;

                    // Clear and rebuild CRDT from received text
                    crdtTree = new CrdtTree();
                    positionToNodeMap.clear();
                    int pos = 0;
                    CrdtNode parent = crdtTree.getRoot();

                    for (char c : msg.toCharArray()) {
                        NodeId id = new NodeId(0, new Timestamp(System.currentTimeMillis())); // Remote change
                        CrdtNode node = new CrdtNode(id, c);
                        crdtTree.addChild(parent.getId(), node);
                        positionToNodeMap.put(pos++, node);
                        parent = node;
                    }

                    updateUIFromCRDT(); // Update CodeArea from CRDT
                    ignoreIncoming = false;
                });

                webSocketClient.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void handleBackBtn() throws IOException
    {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
        Parent root = loader.load();

        Stage currentStage = (Stage) backBtn.getScene().getWindow();
        currentStage.close();

        Stage newStage = new Stage();
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

        newStage.setTitle("Real-Time Collaborative Text Editor");
        newStage.setScene(scene);

        newStage.show();
        newStage.setMaximized(true);


    }

    private void handleTextChange(PlainTextChange change) {
        // Handle deletions
        if (!change.getRemoved().isEmpty()) {
            for (int i = change.getPosition(); i < change.getPosition() + change.getRemoved().length(); i++) {
                CrdtNode node = positionToNodeMap.get(i);
                if (node != null) {
                    node.setDeleted(true);
                }
            }
        }

        // Handle insertions
        if (!change.getInserted().isEmpty()) {
            String insertedText = change.getInserted();
            int insertPos = change.getPosition();

            // Find parent node (node before insertion point)
            CrdtNode parentNode = findParentNode(insertPos);

            for (int i = 0; i < insertedText.length(); i++) {
                char c = insertedText.charAt(i);
                NodeId newNodeId = new NodeId(
                        currentUserId,
                        new Timestamp(System.currentTimeMillis())
                );

                CrdtNode newNode = new CrdtNode(newNodeId, c);
                crdtTree.addChild(parentNode != null ? parentNode.getId() : crdtTree.getRoot().getId(), newNode);

                // Update position mapping
                positionToNodeMap.put(insertPos + i, newNode);
            }
        }
        crdtTree.printCrdtTree();
        updateUIFromCRDT();

        if (webSocketClient != null && webSocketClient.isOpen() && !ignoreIncoming) {
            String currentText = codeArea.getText(); // Or crdtTree.getText();
            webSocketClient.send(currentText);
        }

    }

    private CrdtNode findParentNode(int position) {
        if (position == 0) return crdtTree.getRoot();
        for (int i = position - 1; i >= 0; i--) {
            CrdtNode node = positionToNodeMap.get(i);
            if (node != null && !node.isDeleted()) {
                return node;
            }
        }
        return crdtTree.getRoot();
    }

    private void updateUIFromCRDT() {
        StringBuilder sb = new StringBuilder();
        for (CrdtNode child : crdtTree.getRoot().getChildren()) {
            traverseAndBuildString(child, sb);
        }
        codeArea.replaceText(sb.toString());
    }

    private void traverseAndBuildString(CrdtNode node, StringBuilder sb) {
        if (node == null || node.isDeleted()) return;

        sb.append(node.getValue());
        for (CrdtNode child : node.getChildren()) {
            traverseAndBuildString(child, sb);
        }
    }




    public void handleExport() throws IOException
    {
        fileContent = codeArea.getText();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");

        // Set extension filter to save only .txt files
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);

        // Show save file dialog
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(fileContent);
            } catch (IOException e) {
                System.out.println("Error Exporting file (BrowseDocument.java)" + e);
            }
        }
    }

    @FXML
    public void handleCopyEditorCode() {
        if (editorCode != null && !editorCode.isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(editorCode);
            clipboard.setContent(content);
        }
    }

    @FXML
    public void handleCopyViewerCode() {
        if (viewerCode != null && !viewerCode.isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(viewerCode);
            clipboard.setContent(content);
        }
    }

}
