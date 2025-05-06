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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JoinDocument {
    @FXML
    public VBox sidebar;
    public HBox mainContainer;
    public CodeArea codeArea;
    public String fileContent;
    public Button exportBtn;
    public Button backBtn;

    private CrdtTree crdtTree = new CrdtTree();
    private int currentUserId = 2; // Or get from authentication
    private Map<Integer, CrdtNode> positionToNodeMap = new HashMap<>();
    private String sessionCode;
    private String editorCode;
    private String viewerCode;
    private String sessionId;
    private boolean isProcessingRemoteChange = false;
    WebSocketHandler myWebSocket = new WebSocketHandler();
    HttpHelper httpHelper = new HttpHelper();

    @FXML
    public void initialize() {
        codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area"); // Add CSS class

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        HBox.setHgrow(codeArea, javafx.scene.layout.Priority.ALWAYS);
        codeArea.prefWidthProperty().bind(mainContainer.widthProperty());

        crdtTree = new CrdtTree();
        positionToNodeMap.clear();

        mainContainer.getChildren().add(1, codeArea);

        try {
            Thread.sleep(200); // Ensure unique timestamp
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

    public void setUpDocument(List<String> myOperations, String ss, String userCode) {
        positionToNodeMap.clear(); // Clear old state
        sessionId = ss;

        if (userCode.startsWith("V"))
        {
            codeArea.setEditable(false);
        }
        for (String opString : myOperations) {
            String[] parts = opString.split(",!!", -1);
            if (parts.length < 5) continue; // Skip invalid entries

            String type = parts[0];
            int position = Integer.parseInt(parts[1]);
            String character = parts[2];
            int userId = Integer.parseInt(parts[3]);
            Timestamp timestamp = Timestamp.valueOf(parts[4]);

            NodeId nodeId = new NodeId(userId, timestamp);

            if (type.equalsIgnoreCase("insert")) {
                // Find parent (node before current position)
                CrdtNode parentNode = findParentNode(position);

                // Make sure we're dealing with a single character
                char charToInsert = character.length() > 0 ? character.charAt(0) : ' ';

                CrdtNode newNode = new CrdtNode(nodeId, charToInsert);
                crdtTree.addChild(parentNode != null ? parentNode.getId() : crdtTree.getRoot().getId(), newNode);

                // Shift positionToNodeMap right from position
                int sizeBefore = positionToNodeMap.size();
                for (int i = sizeBefore - 1; i >= position; i--) {
                    CrdtNode shiftedNode = positionToNodeMap.get(i);
                    if (shiftedNode != null) {
                        positionToNodeMap.put(i + 1, shiftedNode);
                    }
                }
                positionToNodeMap.put(position, newNode);

            } else if (type.equalsIgnoreCase("delete")) {
                int removedLen = character.length();

                for (int i = position; i < position + removedLen; i++) {
                    CrdtNode node = positionToNodeMap.get(i);
                    if (node != null) {
                        node.setDeleted(true);
                    }
                }

                // Shift all nodes after the deleted ones
                int sizeBefore = positionToNodeMap.size();
                for (int i = position + removedLen; i < sizeBefore; i++) {
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
        }

        crdtTree.printCrdtTree();
        System.out.println("Finished initialization");
        updateUIFromCRDT();
        codeArea.plainTextChanges().subscribe(this::handleTextChange);
        subscribeToDocument(sessionId);
    }

    public void subscribeToDocument(String sessionId) {
        myWebSocket.connectToWebSocket();
        StompSession stompSession = myWebSocket.getStompSession();

        String eventStr = "join,2,editor";
        myWebSocket.updateUserEvent(sessionCode, eventStr);

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
        // Ensure we're processing just a single character
        char c = parts[2].length() > 0 ? parts[2].charAt(0) : ' ';
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

        System.out.println("Processed insert: '" + c + "' at position " + insertPos);
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

        System.out.println("Processed delete: '" + removed + "' at position " + deletePos);
    }

    public void handleBackBtn() throws IOException {
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
            String Change = "delete,!!" + insertPos + ",!!" + change.getRemoved() + ",!!" + currentUserId + ",!!" + ts;
            myWebSocket.updateDocument(sessionId, Change);
        }

        // ----- Handle Insertions -----
        if (!change.getInserted().isEmpty()) {
            String insertedText = change.getInserted();

            // Shift existing nodes forward to make space
            int sizeBefore = positionToNodeMap.size();
            for (int i = sizeBefore - 1; i >= insertPos; i--) {
                CrdtNode shiftedNode = positionToNodeMap.get(i);
                if (shiftedNode != null) {
                    positionToNodeMap.put(i + insertedText.length(), shiftedNode);
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

                // Send one character at a time with the exact position
                String Change = "insert,!!" + (insertPos + i) + ",!!" + c + ",!!" + currentUserId + ",!!" + ts;
                System.out.println("Sending change: " + Change);
                myWebSocket.updateDocument(sessionId, Change);
            }
        }

        crdtTree.printCrdtTree();
        updateUIFromCRDT();
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
                System.out.println("Error Exporting file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}