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
import java.util.HashMap;
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
        int insertPos = Integer.parseInt(parts[1]);
        char c = parts[2].charAt(0);
        int userId = Integer.parseInt(parts[3]);
        Timestamp ts = Timestamp.valueOf(parts[4]);
        NodeId nodeId = new NodeId(userId, ts);
        CrdtNode newNode = new CrdtNode(nodeId, c);

        // Shift existing nodes forward to make space
        int sizeBefore = positionToNodeMap.size();
        for (int i = sizeBefore - 1; i >= insertPos; i--) {
            CrdtNode shiftedNode = positionToNodeMap.get(i);
            if (shiftedNode != null) {
                positionToNodeMap.put(i + 1, shiftedNode);
            }
        }

        // Add to CRDT tree
        CrdtNode parentNode = findParentNode(insertPos);
        crdtTree.addChild(parentNode != null ? parentNode.getId() : crdtTree.getRoot().getId(), newNode);

        // Insert into position map
        positionToNodeMap.put(insertPos, newNode);
    }

    private void processDeleteOperation(String[] parts) {
        int deletePos = Integer.parseInt(parts[1]);
        String removed = parts[2];
        int removedLen = removed.length();

        for (int i = deletePos; i < deletePos + removedLen; i++) {
            CrdtNode node = positionToNodeMap.get(i);
            if (node != null) {
                node.setDeleted(true);
            }
        }

        // Shift all nodes after the deleted ones
        int sizeBefore = positionToNodeMap.size();
        for (int i = deletePos + removedLen; i < sizeBefore; i++) {
            CrdtNode shiftedNode = positionToNodeMap.remove(i);
            if (shiftedNode != null) {
                positionToNodeMap.put(i - removedLen, shiftedNode);
            }
        }

        // Remove trailing keys if they remain
        for (int i = sizeBefore - removedLen; i < sizeBefore; i++) {
            positionToNodeMap.remove(i);
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

        // CRDT Initialization: Insert all as children of root, one after another
        CrdtNode lastInsertedNode = null;

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
            String Change = "insert,!!" + i + ",!!" + fileContent.charAt(i) + ",!!" + currentUserId + ",!!" + ts;
            myWebSocket.updateDocument(sessionId, Change);
            positionToNodeMap.put(i, newNode);
            lastInsertedNode = newNode;
        }

        // Send the initial document state to the server
        if (!fileContent.isEmpty()) {
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            String initialChange = "initialize,!!" + 0 + ",!!" + fileContent + ",!!" + currentUserId + ",!!" + ts;
            myWebSocket.updateDocument(sessionId, initialChange);
        }

        // Set up text change listener
        codeArea.plainTextChanges().subscribe(this::handleTextChange);
    }

    private void handleRemoteChange(String changeInfo) {
        // Parse the change info received from WebSocket
        String[] parts = changeInfo.split(",!!");
        if (parts.length < 5) return;

        String operation = parts[0];
        int position = Integer.parseInt(parts[1]);
        String content = parts[2];
        int userId = Integer.parseInt(parts[3]);

        // Skip processing if this is our own change
        if (userId == currentUserId) return;

        // Apply the change to the CRDT structure
        if ("insert".equals(operation)) {
            handleRemoteInsert(position, content);
        } else if ("delete".equals(operation)) {
            handleRemoteDelete(position, content);
        } else if ("initialize".equals(operation)) {
            // Handle initial document state if needed
        }
    }

    private void handleRemoteInsert(int position, String content) {
        // Find parent node where to insert
        CrdtNode parentNode = findParentNode(position);

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            try {
                Thread.sleep(1); // Ensure unique timestamp
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            NodeId newNodeId = new NodeId(
                    -1, // Use a special ID for remote users
                    new Timestamp(System.currentTimeMillis())
            );

            CrdtNode newNode = new CrdtNode(newNodeId, c);
            crdtTree.addChild(parentNode != null ? parentNode.getId() : crdtTree.getRoot().getId(), newNode);

            // Shift existing nodes
            for (int j = positionToNodeMap.size() - 1; j >= position + i; j--) {
                positionToNodeMap.put(j + 1, positionToNodeMap.get(j));
            }

            positionToNodeMap.put(position + i, newNode);
            parentNode = newNode;
        }

        updateUIFromCRDT();
    }

    private void handleRemoteDelete(int position, String content) {
        int contentLength = content.length();

        // Mark nodes as deleted
        for (int i = position; i < position + contentLength; i++) {
            CrdtNode node = positionToNodeMap.get(i);
            if (node != null) {
                node.setDeleted(true);
            }
        }

        // Shift remaining nodes
        int sizeBefore = positionToNodeMap.size();
        for (int i = position + contentLength; i < sizeBefore; i++) {
            CrdtNode shiftedNode = positionToNodeMap.remove(i);
            positionToNodeMap.put(i - contentLength, shiftedNode);
        }

        // Clean up trailing keys
        for (int i = sizeBefore - contentLength; i < sizeBefore; i++) {
            positionToNodeMap.remove(i);
        }

        updateUIFromCRDT();
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

    // Track whether we're currently updating the UI
    private boolean isUpdatingUI = false;

    private void updateUIFromCRDT() {
        if (isUpdatingUI) return; // Prevent recursive calls

        isUpdatingUI = true;

        StringBuilder sb = new StringBuilder();
        for (CrdtNode child : crdtTree.getRoot().getChildren()) {
            traverseAndBuildString(child, sb);
        }

        String currentText = codeArea.getText();
        String newText = sb.toString();

        if (!currentText.equals(newText)) {
            codeArea.replaceText(newText);
        }

        isUpdatingUI = false;
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
        // Skip processing if we're currently updating the UI from CRDT
        if (isUpdatingUI) return;

        int insertPos = change.getPosition();

        // ----- Handle Deletions -----
        if (!change.getRemoved().isEmpty()) {
            int removedLen = change.getRemoved().length();

            for (int i = insertPos; i < insertPos + removedLen; i++) {
                CrdtNode node = positionToNodeMap.get(i);
                if (node != null) {
                    node.setDeleted(true);
                }
            }

            // Shift all nodes after the deleted ones
            int sizeBefore = positionToNodeMap.size();
            for (int i = insertPos + removedLen; i < sizeBefore; i++) {
                CrdtNode shiftedNode = positionToNodeMap.remove(i);
                if (shiftedNode != null) {
                    positionToNodeMap.put(i - removedLen, shiftedNode);
                }
            }

            // Remove trailing keys if they remain
            for (int i = sizeBefore - removedLen; i < sizeBefore; i++) {
                positionToNodeMap.remove(i);
            }

            Timestamp ts = new Timestamp(System.currentTimeMillis());
            String changeInfo = "delete,!!" + insertPos + ",!!" + change.getRemoved() + ",!!" + currentUserId + ",!!" + ts;
            myWebSocket.updateDocument(sessionId, changeInfo);
        }

        // ----- Handle Insertions -----
        if (!change.getInserted().isEmpty()) {
            String insertedText = change.getInserted();
            int insertedLen = insertedText.length();

            // Shift existing nodes forward to make space
            int sizeBefore = positionToNodeMap.size();
            for (int i = sizeBefore - 1; i >= insertPos; i--) {
                CrdtNode shiftedNode = positionToNodeMap.get(i);
                if (shiftedNode != null) {
                    positionToNodeMap.put(i + insertedLen, shiftedNode);
                }
            }

            // Insert new nodes
            CrdtNode parentNode = findParentNode(insertPos);

            for (int i = 0; i < insertedText.length(); i++) {
                char c = insertedText.charAt(i);

                try {
                    Thread.sleep(1); // Ensure unique timestamp
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                Timestamp ts = new Timestamp(System.currentTimeMillis());
                NodeId newNodeId = new NodeId(currentUserId, ts);
                CrdtNode newNode = new CrdtNode(newNodeId, c);

                crdtTree.addChild(parentNode != null ? parentNode.getId() : crdtTree.getRoot().getId(), newNode);
                positionToNodeMap.put(insertPos + i, newNode);
                parentNode = newNode; // Update parent for next character
            }

            // Send the change to the server
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            String changeInfo = "insert,!!" + insertPos + ",!!" + insertedText + ",!!" + currentUserId + ",!!" + ts;
            myWebSocket.updateDocument(sessionId, changeInfo);
        }
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