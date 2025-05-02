package com.example.apt_project;

import Network.NetworkConfig;
import Network.CustomWebSocketClient;
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
        CustomWebSocketClient wsClient = new CustomWebSocketClient(NetworkConfig.SERVER_URL,
                op -> {}, // Ignore operations during session creation
                error -> Platform.runLater(() -> showAlert("Error", error)));
        try {
            wsClient.connectBlocking();
            wsClient.send("CREATE_SESSION");
        } catch (InterruptedException e) {
            showAlert("Error", "Connection interrupted: " + e.getMessage());
            return;
        }

        // Wait briefly for response (simplified, could use a proper callback)
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            showAlert("Error", "Interrupted while creating session");
            return;
        }

        String response = wsClient.getLastMessage();
        wsClient.close();
        if (response != null && response.startsWith("SESSION_CREATED:")) {
            String[] parts = response.split(":")[1].split(",");
            String editorCode = parts[0];
            String viewerCode = parts[1];
            String sessionId = parts[2];

            FXMLLoader loader = new FXMLLoader(getClass().getResource("NewDocument.fxml"));
            Parent root = loader.load();
            NewDocument controller = loader.getController();
            controller.setSession(editorCode, viewerCode, sessionId);

            Stage mainStage = (Stage) newDocBtn.getScene().getWindow();
            mainStage.close();

            Stage newDocStage = new Stage();
            Scene newDocScene = new Scene(root, 800, 600);
            newDocScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            newDocStage.setTitle("Real-Time Collaborative Text Editor");
            newDocStage.setScene(newDocScene);
            newDocStage.show();
            newDocStage.setMaximized(true);
        } else {
            showAlert("Error", "Failed to create session");
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

        CustomWebSocketClient wsClient = new CustomWebSocketClient(NetworkConfig.SERVER_URL,
                op -> {}, // Ignore operations during validation
                error -> Platform.runLater(() -> showAlert("Error", error)));
        try {
            wsClient.connectBlocking();
            wsClient.send("VALIDATE_CODE:" + code);
        } catch (InterruptedException e) {
            showAlert("Error", "Connection interrupted: " + e.getMessage());
            return;
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            showAlert("Error", "Interrupted while validating code");
            return;
        }

        String response = wsClient.getLastMessage();
        wsClient.close();
        if (response != null && response.startsWith("VALID_SESSION:")) {
            String sessionId = response.split(":")[1];
            FXMLLoader loader = new FXMLLoader(getClass().getResource("NewDocument.fxml"));
            Parent root = loader.load();
            NewDocument controller = loader.getController();
            controller.setSession(code, code.startsWith("V-") ? "" : code, sessionId);

            Stage mainStage = (Stage) joinBtn.getScene().getWindow();
            mainStage.close();

            Stage newDocStage = new Stage();
            Scene newDocScene = new Scene(root, 800, 600);
            newDocScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
            newDocStage.setTitle("Real-Time Collaborative Text Editor");
            newDocStage.setScene(newDocScene);
            newDocStage.show();
            newDocStage.setMaximized(true);
        } else {
            showAlert("Error", "Invalid session code");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
