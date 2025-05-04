package com.example.apt_project;

import Network.NetworkConfig;
import Network.CustomWebSocketClient;
import Network.DocumentWebSocketHandler;
import Network.HTTPHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class HelloController {

    @FXML
    public ImageView newDocImg;
    public ImageView browseImg;
    public ImageView joinImg;
    public Button browseBtn;
    public Button joinBtn;
    public TextField sessionField;
    public Button newDocBtn;
    public Label sessionLabel;
    public Label titleLabel;
    public File userFile;
    public String userFileContent;

    @FXML
    public void handleNewDocBtn() throws IOException {
        try {
            // Create a new document using HTTPHelper
            Map<String, String> documentInfo = HTTPHelper.createDocument();

            String documentId = documentInfo.get("documentId");
            String editorCode = documentInfo.get("editorCode");
            String viewerCode = documentInfo.get("viewerCode");

            if (documentId == null || editorCode == null || viewerCode == null) {
                showAlert("Error", "Failed to create document: Missing information from server");
                return;
            }

            // Create a DocumentWebSocketHandler for WebSocket communication
            DocumentWebSocketHandler documentHandler = new DocumentWebSocketHandler(
                    message -> {}, // Message handler will be set in NewDocument
                    error -> Platform.runLater(() -> showAlert("Error", error))
            );

            // Connect to the document
            boolean connected = documentHandler.connect(documentId);
            if (!connected) {
                showAlert("Error", "Failed to connect to document");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("NewDocument.fxml"));
            Parent root = loader.load();
            NewDocument controller = loader.getController();

            // Pass the document information and handler to the NewDocument controller
            controller.setSessionWithExistingConnection(editorCode, viewerCode, documentId, documentHandler);

            Stage mainStage = (Stage) newDocBtn.getScene().getWindow();
            mainStage.close();

            Stage newDocStage = new Stage();
            Scene newDocScene = new Scene(root, 800, 600);
            newDocScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            newDocStage.setTitle("Real-Time Collaborative Text Editor");
            newDocStage.setScene(newDocScene);
            newDocStage.show();
            newDocStage.setMaximized(true);
        } catch (IOException e) {
            showAlert("Error", "Failed to create document: " + e.getMessage());
        }
    }

    public void handleBrowse() throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a text file (.txt)");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);

        Stage browseDocStage = new Stage();
        userFile = fileChooser.showOpenDialog(browseDocStage);
        if (userFile != null) {
            try {
                userFileContent = Files.readString(userFile.toPath());
            } catch (IOException e) {
                System.out.println("Error Reading User File: " + e);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("BrowseDocument.fxml"));
            Parent root = loader.load();

            Stage mainStage = (Stage) browseBtn.getScene().getWindow();
            mainStage.close();

            BrowseDocument controller = loader.getController();
            controller.setFileContent(userFileContent);
            Scene browseDocScene = new Scene(root);
            browseDocScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            browseDocStage.setTitle("Real-Time Collaborative Text Editor");
            browseDocStage.setScene(browseDocScene);
            browseDocStage.setMaximized(true);
            browseDocStage.show();
        }
    }

    @FXML
    public void handleJoinBtn() throws IOException {
        String code = sessionField.getText().trim();
        if (code.isEmpty()) {
            showAlert("Error", "Please enter a session code");
            return;
        }

        // Use HTTPHelper to validate the session code and get the document ID
        try {
            // Validate the session code and get the document ID
            Map<String, String> validationResult = HTTPHelper.validateSessionCode(code);
            String documentId = validationResult.get("documentId");
            boolean isEditor = Boolean.parseBoolean(validationResult.get("isEditor"));

            // Set the editor and viewer codes based on the validation result
            String editorCode = isEditor ? code : "";
            String viewerCode = isEditor ? "" : code;

            // Create a DocumentWebSocketHandler for WebSocket communication
            DocumentWebSocketHandler documentHandler = new DocumentWebSocketHandler(
                    message -> {}, // Message handler will be set in NewDocument
                    error -> Platform.runLater(() -> showAlert("Error", error))
            );

            // Connect to the document
            boolean connected = documentHandler.connect(documentId);
            if (!connected) {
                showAlert("Error", "Failed to connect to document");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("NewDocument.fxml"));
            Parent root = loader.load();
            NewDocument controller = loader.getController();

            // Pass the document information and handler to the NewDocument controller
            controller.setSessionWithExistingConnection(editorCode, viewerCode, documentId, documentHandler);

            Stage mainStage = (Stage) joinBtn.getScene().getWindow();
            mainStage.close();

            Stage newDocStage = new Stage();
            Scene newDocScene = new Scene(root, 800, 600);
            newDocScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            newDocStage.setTitle("Real-Time Collaborative Text Editor");
            newDocStage.setScene(newDocScene);
            newDocStage.show();
            newDocStage.setMaximized(true);
        } catch (Exception e) {
            showAlert("Error", "Failed to join document: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message)
    {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
