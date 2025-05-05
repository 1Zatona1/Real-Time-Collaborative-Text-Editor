package com.example.apt_project;

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
    public Label editorCodeLabel;
    public Label editorCodeText;
    public Label viewerCodeLabel;
    public Label viewerCodeText;
    public Button copyEditorCodeBtn;
    public Button copyViewerCodeBtn;
    private CrdtTree crdtTree = new CrdtTree();
    private int currentUserId = 2; // Or get from authentication
    private Map<Integer, CrdtNode> positionToNodeMap = new HashMap<>();
    private int logicalPositionCounter = 0;
//    private TextEditorWebSocketClient webSocketClient;
    private String sessionCode;
    private String editorCode;
    private String viewerCode;
    private String sessionId;
    private boolean ignoreIncoming = false;
    WebSocketHandler myWebSocket = new WebSocketHandler();
    HttpHelper httpHelper = new HttpHelper();



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



    public void setUpDocument(List<String> myOperations, String ss)
    {
        positionToNodeMap.clear(); // Clear old state
        sessionId = ss;
        for (String opString : myOperations) {
            String[] parts = opString.split(",", -1);
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

                CrdtNode newNode = new CrdtNode(nodeId, character.charAt(0));
                crdtTree.addChild(parentNode != null ? parentNode.getId() : crdtTree.getRoot().getId(), newNode);

                // Shift positionToNodeMap right from position
                for (int j = positionToNodeMap.size() - 1; j >= position; j--) {
                    positionToNodeMap.put(j + 1, positionToNodeMap.get(j));
                }
                positionToNodeMap.put(position, newNode);

            } else if (type.equalsIgnoreCase("delete")) {
                // Mark node at position as deleted
                CrdtNode node = positionToNodeMap.get(position);
                if (node != null) {
                    node.setDeleted(true);
                    positionToNodeMap.remove(position);
                    // Optionally shift the map left to fill the gap
                    for (int j = position + 1; j <= positionToNodeMap.size(); j++) {
                        CrdtNode shifted = positionToNodeMap.remove(j);
                        if (shifted != null) {
                            positionToNodeMap.put(j - 1, shifted);
                        }
                    }
                }
            }
        }
        crdtTree.printCrdtTree();
        System.out.println("Finished");
        updateUIFromCRDT();
        codeArea.plainTextChanges().subscribe(this::handleTextChange);
        subscribeToDocument(sessionId);

    }

    public void subscribeToDocument(String sessionId){
        myWebSocket.connectToWebSocket();
        StompSession stompSession = myWebSocket.getStompSession();


        if (stompSession != null && stompSession.isConnected()) {
            try {
                String topic = "/topic/document/" + sessionId;
                stompSession.subscribe(topic, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return String.class; // Expected payload type
                    }
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        // Check the payload type and handle accordingly
                        if (payload instanceof String) {
                            String message = payload.toString();
                            String[] parts = message.split(",", -1);
                            if (parts[0].equalsIgnoreCase("insert")) {
                                int position = Integer.parseInt(parts[1]);
                                CrdtNode parentNode = findParentNode(position);
                                Timestamp ts = Timestamp.valueOf(parts[4]);
                                NodeId nodeId = new NodeId(
                                        Integer.parseInt(parts[3]),
                                        ts
                                );
                                CrdtNode newNode = new CrdtNode(nodeId, parts[2].charAt(0));
                                crdtTree.addChild(parentNode != null ? parentNode.getId() : crdtTree.getRoot().getId(), newNode);

                                // Shift positionToNodeMap right from position
                                for (int j = positionToNodeMap.size() - 1; j >= position; j--) {
                                    positionToNodeMap.put(j + 1, positionToNodeMap.get(j));
                                }
                                positionToNodeMap.put(position, newNode);
                            }
                            else
                            {
                                int position = Integer.parseInt(parts[1]);
                                CrdtNode node = positionToNodeMap.get(position);
                                if (node != null) {
                                    node.setDeleted(true);
                                    positionToNodeMap.remove(position);
                                    // Optionally shift the map left to fill the gap
                                    for (int j = position + 1; j <= positionToNodeMap.size(); j++) {
                                        CrdtNode shifted = positionToNodeMap.remove(j);
                                        if (shifted != null) {
                                            positionToNodeMap.put(j - 1, shifted);
                                        }
                                    }
                                }
                            }
                            System.out.println("Document Update: " + message);
                            updateUIFromCRDT();
                        }
                    }
                });

                System.out.println("Subscribed to document " + sessionId);
            } catch (Exception e) {
                System.out.println("Failed to subscribe to document: " + e.getMessage());
            }
        } else {
            System.out.println("Not connected to WebSocket server");
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
            String Change = "delete," + insertPos + "," + change.getRemoved() + "," + currentUserId + "," + ts;
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

                String Change = "insert," + insertPos + "," + change.getInserted() + "," + currentUserId + "," + ts;
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
        codeArea.replaceText(sb.toString());
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
