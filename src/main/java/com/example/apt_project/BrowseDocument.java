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
import java.util.concurrent.atomic.AtomicInteger;

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
    private boolean isUpdatingUI = false;
    private final AtomicInteger sequence = new AtomicInteger(0);

    private CrdtTree crdtTree = new CrdtTree();
    private int currentUserId = (int) (Math.random() * 1000); // Generate random user ID
    private Map<Integer, CrdtNode> positionToNodeMap = new HashMap<>();
    private WebSocketHandler myWebSocket = new WebSocketHandler();

    @FXML
    public void initialize() {
        // Initialize CodeArea
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
                                        processRemoteInsert(parts);
                                    } else if (parts[0].equalsIgnoreCase("delete")) {
                                        processRemoteDelete(parts);
                                    }
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

    private void processRemoteInsert(String[] parts) {
        int position = Integer.parseInt(parts[1]);
        String insertedText = parts[2];

        // Update the UI directly
        if (!isUpdatingUI) {
            isUpdatingUI = true;
            try {
                String currentText = codeArea.getText();
                if (position <= currentText.length()) {
                    String newText = currentText.substring(0, position) +
                            insertedText +
                            currentText.substring(position);
                    codeArea.replaceText(newText);
                }
            } finally {
                isUpdatingUI = false;
            }
        }
    }

    private void processRemoteDelete(String[] parts) {
        int position = Integer.parseInt(parts[1]);
        String deletedText = parts[2];
        int deleteLen = deletedText.length();

        // Update the UI directly
        if (!isUpdatingUI) {
            isUpdatingUI = true;
            try {
                String currentText = codeArea.getText();
                if (position < currentText.length() && position + deleteLen <= currentText.length()) {
                    String newText = currentText.substring(0, position) +
                            currentText.substring(position + deleteLen);
                    codeArea.replaceText(newText);
                }
            } finally {
                isUpdatingUI = false;
            }
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
        // Clean up WebSocket subscription if needed

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

        // Set up the text area with content
        if (codeArea != null) {
            codeArea.replaceText(fileContent);
        }

        // Send the initial document state to the server in chunks
        // This helps avoid overwhelming the server with large documents
        int chunkSize = 50;
        for (int i = 0; i < fileContent.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, fileContent.length());
            String chunk = fileContent.substring(i, end);

            Timestamp ts = new Timestamp(System.currentTimeMillis());
            String changeInfo = "insert,!!" + i + ",!!" + chunk + ",!!" + currentUserId + ",!!" + ts;
            myWebSocket.updateDocument(sessionId, changeInfo);

            // Add small delay to avoid overwhelming the server
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Set up text change listener
        codeArea.plainTextChanges().subscribe(this::handleTextChange);
    }

    private void handleTextChange(PlainTextChange change) {
        // Skip processing if we're currently updating the UI
        if (isUpdatingUI || isProcessingRemoteChange) return;

        int insertPos = change.getPosition();
        String removed = change.getRemoved();
        String inserted = change.getInserted();

        // Handle deletions
        if (!removed.isEmpty()) {
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            String changeInfo = "delete,!!" + insertPos + ",!!" + removed + ",!!" +
                    currentUserId + ",!!" + ts + ",!!" + sequence.incrementAndGet();
            myWebSocket.updateDocument(sessionId, changeInfo);
        }

        // Handle insertions
        if (!inserted.isEmpty()) {
            Timestamp ts = new Timestamp(System.currentTimeMillis());
            String changeInfo = "insert,!!" + insertPos + ",!!" + inserted + ",!!" +
                    currentUserId + ",!!" + ts + ",!!" + sequence.incrementAndGet();
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