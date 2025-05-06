package com.example.apt_project;

import javafx.application.Platform;
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
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import treeCRDT.CrdtNode;
import treeCRDT.CrdtTree;
import treeCRDT.NodeId;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BrowseDocument {
    public HBox mainContainer;
    public VBox sidebar;
    public CodeArea codeArea;
    public String fileContent;
    public Button exportBtn;
    public Button backBtn;
    public Label editorCodeLabel;
    public Button copyEditorCodeBtn;
    public Label editorCodeText;
    public Label viewerCodeLabel;
    public Button copyViewerCodeBtn;
    public Label viewerCodeText;
    private String editorCode;
    private String viewerCode;
    private String sessionId;
    private boolean isProcessingRemoteChange = false;


    private CrdtTree crdtTree = new CrdtTree();
    private int currentUserId = 1; // Or get from authentication
    private Map<Integer, CrdtNode> positionToNodeMap = new HashMap<>();
    private WebSocketHandler myWebSocket = new WebSocketHandler();

    @FXML
    public void initialize() {
        codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area");
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        HBox.setHgrow(codeArea, javafx.scene.layout.Priority.ALWAYS);
        codeArea.prefWidthProperty().bind(mainContainer.widthProperty());

        mainContainer.getChildren().add(1, codeArea);

        // Create a new document session
        String mySessionDetails = HttpHelper.createDocument();

        // Parse session details
        String[] parts = mySessionDetails.split(",");
        sessionId = parts[0];
        editorCode = parts[1];
        viewerCode = parts[2];

        editorCodeText.setText(editorCode);
        viewerCodeText.setText(viewerCode);

        // Initialize WebSocket connection
        myWebSocket.connectToWebSocket();
        // myWebSocket.subscribeToDocument(sessionId, this::handleRemoteChange);
        subscribeToDocument(sessionId);
        // Prompt user to select a file immediately after initialization
        promptForFileSelection();
    }

    private int getPositionForNode(CrdtNode targetNode) {
        // Find position of a node in current view
        for (Map.Entry<Integer, CrdtNode> entry : positionToNodeMap.entrySet()) {
            if (entry.getValue().equals(targetNode)) {
                return entry.getKey();
            }
        }
        return -1; // Not found
    }

    private Timestamp getUniqueTimestamp() {
        // Base timestamp
        Timestamp ts = new Timestamp(System.currentTimeMillis());

        // Add nanoTime to handle sub-millisecond operations
        long nanos = System.nanoTime() % 1_000_000;
        ts.setNanos((int)nanos);

        return ts;
    }

    private List<CrdtNode> flattenTree(CrdtNode startNode) {
        List<CrdtNode> result = new ArrayList<>();
        if (startNode == null) return result;

        // Add this node
        if (startNode != crdtTree.getRoot()) {
            result.add(startNode);
        }

        // Add all children recursively
        for (CrdtNode child : startNode.getChildren()) {
            result.addAll(flattenTree(child));
        }

        return result;
    }

    private void updatePositionMapFromTree() {
        // Get flattened visible nodes from tree
        List<CrdtNode> flatNodes = flattenTree(crdtTree.getRoot());

        // Clear and rebuild position map
        positionToNodeMap.clear();
        int visibleIndex = 0;

        for (CrdtNode node : flatNodes) {
            if (!node.isDeleted() && node != crdtTree.getRoot()) {
                positionToNodeMap.put(visibleIndex, node);
                visibleIndex++;
            }
        }
    }

    public void subscribeToDocument(String sessionId) {
        StompSession stompSession = myWebSocket.getStompSession();

        if (stompSession != null && stompSession.isConnected()) {
            try {
                String topic = "/topic/document/" + sessionId;
                stompSession.subscribe(topic, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        if (payload instanceof String) {
                            String message = payload.toString();
                            String[] parts = message.split(",!!", -1);

                            if (parts.length < 5) return; // Skip invalid messages

                            // Check if this message is from this user
                            int userId = Integer.parseInt(parts[3]);
                            if (userId == currentUserId) return; // Skip our own messages

                            Platform.runLater(() -> {
                                isProcessingRemoteChange = true;
                                try {
                                    if (parts[0].equalsIgnoreCase("insert")) {
                                        processInsertOperation(parts);
                                    } else if (parts[0].equalsIgnoreCase("delete")) {
                                        processDeleteOperation(parts);
                                    }

                                    System.out.println("Document Update: " + message);
                                    updateUIFromCRDT();
                                } finally {
                                    isProcessingRemoteChange = false;
                                }
                            });
                        }
                    }
                });

                System.out.println("Subscribed to document " + sessionId);
            } catch (Exception e) {
                System.out.println("Failed to subscribe to document: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Not connected to WebSocket server");
        }
    }

    private void processInsertOperation(String[] parts) {
        try {
            // Parse operation data
            int insertPos = Integer.parseInt(parts[1]);
            char c = parts[2].length() > 0 ? parts[2].charAt(0) : ' ';
            int userId = Integer.parseInt(parts[3]);
            Timestamp ts = Timestamp.valueOf(parts[4]);
            NodeId nodeId = new NodeId(userId, ts);

            // Find parent node based on position
            CrdtNode parentNode = findParentNode(insertPos);

            // Create new node
            CrdtNode newNode = new CrdtNode(nodeId, c);

            // Add to CRDT tree
            crdtTree.addChild(parentNode.getId(), newNode);

            // Update position map from the tree structure
            updatePositionMapFromTree();

            System.out.println("Processed insert: '" + c + "' from user " + userId);
        } catch (Exception e) {
            System.err.println("Error processing insert operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processDeleteOperation(String[] parts) {
        try {
            int deletePos = Integer.parseInt(parts[1]);
            String removed = parts[2];
            int removedLen = removed.length();

            // Mark nodes as deleted
            for (int i = deletePos; i < deletePos + removedLen; i++) {
                CrdtNode node = positionToNodeMap.get(i);
                if (node != null) {
                    node.setDeleted(true);
                }
            }

            // Update position map from tree structure
            updatePositionMapFromTree();

            System.out.println("Processed delete: '" + removed + "' at position " + deletePos);
        } catch (Exception e) {
            System.err.println("Error processing delete operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void promptForFileSelection() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                fileContent = new String(Files.readAllBytes(Paths.get(selectedFile.getPath())));
                setupDocument(fileContent);
            } catch (IOException e) {
                System.out.println("Error loading file: " + e.getMessage());
                fileContent = ""; // Set empty content if file loading fails
                setupDocument(fileContent);
            }
        } else {
            // User cancelled file selection, set empty content
            fileContent = "";
            setupDocument(fileContent);
        }
    }

    public void handleBackBtn() throws IOException {
        // Notify server that user is leaving the document
        // myWebSocket.leaveDocument(sessionId);

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

    public void handleExport() throws IOException {
        fileContent = codeArea.getText();
        if (fileContent.isEmpty()) return;

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

    public void setupDocument(String content) {
        this.fileContent = content;

        if (codeArea != null) {
            codeArea.replaceText(fileContent);
        }

        // Clear the old CRDT state
        positionToNodeMap.clear();

        // Ensure WebSocket is ready before sending updates
        try {
            Thread.sleep(50); // Brief delay to ensure connection is ready
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // CRDT Initialization: Insert all as children of root, one after another
        CrdtNode lastInsertedNode = null;

        // First, build the entire CRDT tree locally
        for (int i = 0; i < fileContent.length(); i++) {
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            NodeId newNodeId = new NodeId(
                    currentUserId,
                    ts
            );

            try {
                Thread.sleep(1); // Ensure unique timestamps
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            CrdtNode newNode = new CrdtNode(newNodeId, fileContent.charAt(i));

            // Attach to root or previous
            if (lastInsertedNode == null) {
                crdtTree.addChild(crdtTree.getRoot().getId(), newNode);
            } else {
                crdtTree.addChild(lastInsertedNode.getId(), newNode);
            }

            positionToNodeMap.put(i, newNode);
            lastInsertedNode = newNode;
        }

        // Now send all updates to the server in a separate loop
        for (int i = 0; i < fileContent.length(); i++) {
            CrdtNode node = positionToNodeMap.get(i);
            String change = "insert,!!" + i + ",!!" + fileContent.charAt(i) + ",!!" +
                    node.getId().getUserId() + ",!!" + node.getId().getClock();
            myWebSocket.updateDocument(sessionId, change);

            // Add a small delay between messages to prevent overwhelming the receiver
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        crdtTree.printCrdtTree();
        codeArea.plainTextChanges().subscribe(this::handleTextChange);
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

        String newText = sb.toString();
        if (!newText.equals(codeArea.getText())) {
            codeArea.replaceText(newText);
        }
    }

    private void traverseAndBuildString(CrdtNode node, StringBuilder sb) {
        if (node == null) return;

        if (!node.isDeleted()) {
            sb.append(node.getValue());
        }

        for (CrdtNode child : node.getChildren()) {
            traverseAndBuildString(child, sb);
        }
    }

    private void handleTextChange(PlainTextChange change) {
        // Skip processing if this change is from remote update
        if (isProcessingRemoteChange) {
            return;
        }

        int insertPos = change.getPosition();

        // ----- Handle Deletions -----
        if (!change.getRemoved().isEmpty()) {
            int removedLen = change.getRemoved().length();

            // Get the nodes to delete
            List<CrdtNode> nodesToDelete = new ArrayList<>();
            for (int i = insertPos; i < insertPos + removedLen; i++) {
                CrdtNode node = positionToNodeMap.get(i);
                if (node != null) {
                    nodesToDelete.add(node);
                }
            }

            // Mark each node as deleted
            for (CrdtNode node : nodesToDelete) {
                node.setDeleted(true);

                // Send delete operation for each character
                Timestamp ts = getUniqueTimestamp();
                String deleteOp = "delete,!!" +
                        getPositionForNode(node) + ",!!" +
                        node.getValue() + ",!!" +
                        currentUserId + ",!!" +
                        ts;
                myWebSocket.updateDocument(sessionId, deleteOp);
            }

            // Update position map
            updatePositionMapFromTree();
        }

        // ----- Handle Insertions -----
        if (!change.getInserted().isEmpty()) {
            String insertedText = change.getInserted();

            // Find the parent node for the first character
            CrdtNode parentNode = findParentNode(insertPos);

            // Insert each character as a node in the tree
            for (int i = 0; i < insertedText.length(); i++) {
                char c = insertedText.charAt(i);

                // Create unique timestamp
                Timestamp ts = getUniqueTimestamp();

                // Create new node
                NodeId newNodeId = new NodeId(currentUserId, ts);
                CrdtNode newNode = new CrdtNode(newNodeId, c);

                // Add to tree
                crdtTree.addChild(parentNode.getId(), newNode);

                // Send operation to server with position and parent NodeId
                String insertOp = "insert,!!" +
                        (insertPos + i) + ",!!" +
                        c + ",!!" +
                        currentUserId + ",!!" +
                        ts;
                myWebSocket.updateDocument(sessionId, insertOp);

                // Update parent for next character
                parentNode = newNode;
            }

            // Update position map
            updatePositionMapFromTree();
        }

        crdtTree.printCrdtTree();
        updateUIFromCRDT();
    }

    @FXML
    public void handleCopyEditorCode() {
        if (editorCodeText != null && editorCodeText.getText() != null && !editorCodeText.getText().isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(editorCodeText.getText());
            clipboard.setContent(content);
        }
    }

    @FXML
    public void handleCopyViewerCode() {
        if (viewerCodeText != null && viewerCodeText.getText() != null && !viewerCodeText.getText().isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(viewerCodeText.getText());
            clipboard.setContent(content);
        }
    }
}