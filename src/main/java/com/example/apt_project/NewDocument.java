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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
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
    public Label editor_1;

    public VBox editorListContainer;
    public VBox viewerListContainer;
    private final Set<String> usedNames = new HashSet<>();

    private CrdtTree crdtTree = new CrdtTree();
    private int currentUserId = 1; // Or get from authentication
    private Map<Integer, CrdtNode> positionToNodeMap = new HashMap<>();
    private String editorCode;
    private String viewerCode;
    private String sessionId;
    private boolean isProcessingRemoteChange = false;
    WebSocketHandler myWebSocket = new WebSocketHandler();





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

        String mySessionDetails = HttpHelper.createDocument();
        myWebSocket.connectToWebSocket();

        String[] parts = mySessionDetails.split(",");

        sessionId = parts[0];
        editorCode = parts[1];
        viewerCode = parts[2];

        editorCodeText.setText(editorCode);
        viewerCodeText.setText(viewerCode);
        editor_1.setText("Editor: You");

        subscribeToUserEvent(sessionId);
        subscribeToDocument(sessionId);
    }

    private String getUniqueUsername() {
        String name;
        do {
            name = UsernameGenerator.generateAnonymousUsername();
        } while (usedNames.contains(name));
        usedNames.add(name);
        return name;
    }

    public void addRandomEditor(String userId) {
        String username = getUniqueUsername();
        addEditor(username, userId);
    }

    public void addRandomViewer(String userId) {
        String username = getUniqueUsername();
        addViewer(username, userId);
    }

    public void addEditor(String username, String userId) {
        Label label = new Label("Editor: " + username + " " + userId);
        label.getStyleClass().add("user-label"); // optional style class
        editorListContainer.getChildren().add(label);
    }

    public void addViewer(String username, String userId) {
        Label label = new Label("Viewer: " + username + " " + userId);
        label.getStyleClass().add("user-label"); // optional style class
        viewerListContainer.getChildren().add(label);
    }

    public void removeEditor(String userId) {
        Label toRemove = null;
        for (javafx.scene.Node node : editorListContainer.getChildren()) {
            if (node instanceof Label label && label.getText().contains(userId)) {
                toRemove = label;
                break;
            }
        }
        if (toRemove != null) {
            editorListContainer.getChildren().remove(toRemove);
        }
    }

    public void removeViewer(String userId) {
        Label toRemove = null;
        for (javafx.scene.Node node : viewerListContainer.getChildren()) {
            if (node instanceof Label label && label.getText().contains(userId)) {
                toRemove = label;
                break;
            }
        }
        if (toRemove != null) {
            viewerListContainer.getChildren().remove(toRemove);
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

    public void subscribeToUserEvent(String sessionId){
        StompSession stompSession = myWebSocket.getStompSession();

        if (stompSession != null && stompSession.isConnected()) {
            try {
                String topic = "/topic/userEvent/" + sessionId;
                stompSession.subscribe(topic, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        if (payload instanceof String) {
                            System.out.println("ana 4a8al aho wla eh");

                            Platform.runLater(() -> {
                                String message = payload.toString();
                                String[] parts = message.split(",", -1);
                                if (parts.length < 3) return;
                                String typeOfEvent = parts[0];
                                String userId = parts[1];
                                String typeOfUser = parts[2];

                                if (typeOfEvent.equalsIgnoreCase("join")) {
                                    if (typeOfUser.equalsIgnoreCase("editor")) {
                                        int count = HttpHelper.getNumberOfEditors(sessionId);
                                        System.out.println("Count now: " + count);
                                        if (count <= 4) {
                                            addRandomEditor(userId);
                                        }

                                    }
                                    else if (typeOfUser.equalsIgnoreCase("viewer")) {
                                        addRandomViewer(userId);
                                    }
                                }
                                else if(typeOfEvent.equalsIgnoreCase("leave")) {
                                    if (typeOfUser.equalsIgnoreCase("editor")) {
                                        removeEditor(userId);
                                    }
                                    else if (typeOfUser.equalsIgnoreCase("viewer")) {
                                        removeViewer(userId);
                                    }
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

    public void handleBackBtn() throws IOException
    {
        String userEvent = "leave," + currentUserId + "," + "editor";
        myWebSocket.updateUserEvent(sessionId, userEvent);

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
        // Skip processing if this change is from remote update
        if (isProcessingRemoteChange) {
            return;
        }

        int insertPos = change.getPosition();
        // ----- Handle Deletions -----
        if (!change.getRemoved().isEmpty()) {
            int removedLen = change.getRemoved().length();

            for (int i = insertPos; i < insertPos + removedLen; i++) {
                CrdtNode node = positionToNodeMap.get(i);
                if (node != null) {
                    node.setDeleted(true);
                    // Add Handling of sending the operation to server
                }
            }

            // Shift all nodes after the deleted ones
            int sizeBefore = positionToNodeMap.size();
            for (int i = insertPos + removedLen; i < sizeBefore; i++) {
                CrdtNode shiftedNode = positionToNodeMap.remove(i);
                positionToNodeMap.put(i - removedLen, shiftedNode);
            }

            // Remove trailing keys if they remain
            for (int i = sizeBefore - removedLen; i < sizeBefore; i++) {
                positionToNodeMap.remove(i);
            }

            Timestamp ts = new Timestamp(System.currentTimeMillis());
            String Change = "delete,!!" + insertPos + ",!!" + change.getRemoved() + ",!!" + currentUserId + ",!!" + ts;
            myWebSocket.updateDocument(sessionId, Change);
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

                NodeId newNodeId = new NodeId(
                        currentUserId,
                        ts
                );

                CrdtNode newNode = new CrdtNode(newNodeId, c);
                crdtTree.addChild(parentNode != null ? parentNode.getId() : crdtTree.getRoot().getId(), newNode);
                positionToNodeMap.put(insertPos + i, newNode);

                parentNode = newNode; // Update parent for next character

                String Change = "insert,!!" + (insertPos + i) + ",!!" + c + ",!!" + currentUserId + ",!!" + ts;
                myWebSocket.updateDocument(sessionId, Change);
            }
        }

        crdtTree.printCrdtTree();
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

    private void updateUIFromCRDT() {
        StringBuilder sb = new StringBuilder();
        for (CrdtNode child : crdtTree.getRoot().getChildren()) {
            traverseAndBuildString(child, sb);
        }

        String newText = sb.toString();
        String oldText = codeArea.getText();

        if (!newText.equals(oldText)) {
            if (isProcessingRemoteChange) {
                // Just replace the text without affecting the local cursor
                int caretPosition = codeArea.getCaretPosition();
                int anchorPosition = codeArea.getAnchor();

                codeArea.replaceText(newText);

                // Restore caret & selection to what they were before remote update
                int newLength = newText.length();
                int newCaretPos = Math.min(caretPosition, newLength);
                int newAnchorPos = Math.min(anchorPosition, newLength);
                codeArea.selectRange(newAnchorPos, newCaretPos);

            } else {
                // Local change (e.g. undo/redo/import): allow cursor to adapt
                codeArea.replaceText(newText);
            }
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

    public void handleExport() throws IOException
    {
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

    public void handleCopyEditorCode() {
        if (editorCode != null && !editorCode.isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(editorCode);
            clipboard.setContent(content);
        }
    }

    public void handleCopyViewerCode() {
        if (viewerCode != null && !viewerCode.isEmpty()) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(viewerCode);
            clipboard.setContent(content);
        }
    }
}