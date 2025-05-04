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
import treeCRDT.CrdtNode;
import treeCRDT.CrdtTree;
import treeCRDT.NodeId;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
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

    private CrdtTree crdtTree = new CrdtTree();
    private int currentUserId = 1; // Or get from authentication
    private Map<Integer, CrdtNode> positionToNodeMap = new HashMap<>();



    @FXML
    public void  initialize()
    {
        codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area");
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        HBox.setHgrow(codeArea, javafx.scene.layout.Priority.ALWAYS);
        codeArea.prefWidthProperty().bind(mainContainer.widthProperty());


        mainContainer.getChildren().add(1, codeArea);

        String mySessionDetails = HttpHelper.createDocument();
        WebSocketHandler myWebSocket = new WebSocketHandler();
        myWebSocket.connectToWebSocket();

        String[] parts = mySessionDetails.split(",");

        sessionId = parts[0];
        editorCode = parts[1];
        viewerCode = parts[2];

        editorCodeText.setText(editorCode);
        viewerCodeText.setText(viewerCode);

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
            NodeId newNodeId = new NodeId(
                    currentUserId,
                    new Timestamp(System.currentTimeMillis())
            );

            try {
                Thread.sleep(1); // Optional: slow for ordering
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
            System.out.println("position: " + i + " " + "Node entered " + newNode + " value: " + newNode.getValue());
            positionToNodeMap.put(i, newNode);
            lastInsertedNode = newNode;
        }

        crdtTree.printCrdtTree();
        codeArea.plainTextChanges().subscribe(this::handleTextChange);
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

    private void handleTextChange(PlainTextChange change) {
        // Handle deletions
        if (!change.getRemoved().isEmpty()) {
            for (int i = change.getPosition(); i < change.getPosition() + change.getRemoved().length(); i++) {
                CrdtNode node = positionToNodeMap.get(i);
                if (node != null) {
                    node.setDeleted(true);
                    // Add Handling of sending the operation to server
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

                // Add Handling of sending the operation to server
            }
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