package com.example.apt_project;

import Network.CustomWebSocketClient;
import Network.DocumentWebSocketHandler;
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Random;

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
    private int currentUserId = 1; // Or get from authentication
    private TextEditorWebSocketClient webSocketClient;
    private DocumentWebSocketHandler documentHandler;
    private String sessionCode;
    private String editorCode;
    private String viewerCode;
    private String sessionId;
    private boolean ignoreIncoming = false;

    /**
     * Sets up the session with an existing DocumentWebSocketHandler
     * @param editorCode The editor code for the document
     * @param viewerCode The viewer code for the document
     * @param documentId The document ID
     * @param handler The DocumentWebSocketHandler to use
     */
    public void setSessionWithExistingConnection(String editorCode, String viewerCode, String documentId, DocumentWebSocketHandler handler) {
        this.editorCode = editorCode;
        this.viewerCode = viewerCode;
        this.sessionId = documentId;
        this.documentHandler = handler;
        // Use the appropriate code for connection - if editorCode is empty, use viewerCode
        this.sessionCode = editorCode.isEmpty() ? viewerCode : editorCode;

        // Update UI
        if (editorCodeText != null) {
            editorCodeText.setText(editorCode);
        }
        if (viewerCodeText != null) {
            viewerCodeText.setText(viewerCode);
        }

        // The handler is already connected to the document in HelloController
    }

    /**
     * Sets up the session with an existing CustomWebSocketClient (legacy method)
     * @param editorCode The editor code for the document
     * @param viewerCode The viewer code for the document
     * @param sessionId The session ID
     * @param uri The WebSocket URI
     * @param existingClient The existing CustomWebSocketClient
     * @deprecated Use setSessionWithExistingConnection(String, String, String, DocumentWebSocketHandler) instead
     */
    @Deprecated
    public void setSessionWithExistingConnection(String editorCode, String viewerCode, String sessionId, String uri, CustomWebSocketClient existingClient) {
        // Create a DocumentWebSocketHandler instead
        DocumentWebSocketHandler handler = new DocumentWebSocketHandler(
                message -> {
                    ignoreIncoming = true;
                    codeArea.replaceText(message);
                    ignoreIncoming = false;
                },
                error -> System.err.println("WebSocket error: " + error)
        );

        // Connect to the document using the new handler
        if (handler.connect(sessionId)) {
            setSessionWithExistingConnection(editorCode, viewerCode, sessionId, handler);
        } else {
            System.err.println("Failed to connect to document with ID: " + sessionId);
        }
    }

    @FXML
    public void initialize() {
        codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area"); // Add CSS class

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        HBox.setHgrow(codeArea, javafx.scene.layout.Priority.ALWAYS);
        codeArea.prefWidthProperty().bind(mainContainer.widthProperty());

        codeArea.plainTextChanges().subscribe(this::handleTextChange);

        mainContainer.getChildren().add(1, codeArea);

        // If no session has been set yet, use a default one for testing
        if (sessionCode == null) {
            // Generate random 6-digit code
            sessionCode = String.valueOf(new Random().nextInt(900000) + 100000);
            editorCode = sessionCode;
            viewerCode = sessionCode;

            if (editorCodeText != null) {
                editorCodeText.setText(sessionCode); // Label on screen
            }
            if (viewerCodeText != null) {
                viewerCodeText.setText(sessionCode); // Label on screen
            }

            // Create a new document using HTTPHelper
            try {
                // Create a DocumentWebSocketHandler for WebSocket communication
                documentHandler = new DocumentWebSocketHandler(
                        message -> {
                            ignoreIncoming = true;
                            codeArea.replaceText(message);
                            ignoreIncoming = false;
                        },
                        error -> System.err.println("WebSocket error: " + error)
                );

                // For testing purposes, we'll use the session code as the document ID
                sessionId = sessionCode;

                // Connect to the document
                boolean connected = documentHandler.connect(sessionId);
                if (!connected) {
                    System.err.println("Failed to connect to document with ID: " + sessionId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        // Only send changes to the server if they weren't caused by receiving a message
        if (!ignoreIncoming && documentHandler != null && documentHandler.isConnected()) {
            // For simplicity, we're just handling insertions and deletions directly
            // In a real implementation, you would track the changes more precisely
            if (change.getInserted().length() > 0 && change.getRemoved().length() == 0) {
                // This is an insertion
                documentHandler.insertText(change.getInserted(), change.getPosition());
            } else if (change.getRemoved().length() > 0) {
                // This is a deletion
                documentHandler.deleteText(change.getPosition(), change.getPosition() + change.getRemoved().length());
            }
        }
    }

    public void handleExport() throws IOException {
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
