package com.example.apt_project;


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
import java.util.List;
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

    @FXML
    private Label errorLabel;
    public Label titleLabel;
    public File userFile;
    public String userFileContent;

    @FXML
    public void handleNewDocBtn() throws IOException {


        FXMLLoader loader = new FXMLLoader(getClass().getResource("NewDocument.fxml"));
        Parent root = loader.load();

        Stage mainStage = (Stage) newDocBtn.getScene().getWindow();
        mainStage.close();

        Stage newDocStage = new Stage();
        Scene newDocScene = new Scene(root, 800, 600);
        newDocScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        newDocStage.setTitle("Real-Time Collaborative Text Editor");
        newDocStage.setScene(newDocScene);
        newDocStage.show();
        newDocStage.setMaximized(true);
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
            controller.setupDocument(userFileContent);
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

        List<String> allOperations = HttpHelper.getListOfOperation(sessionField.getText());
        if (allOperations == null) {
            // Code to handle error message
            errorLabel.setText("Invalid session code. Please try again.");
            errorLabel.setVisible(true);
            return;
        }
        String sessionId = HttpHelper.getDocumentIdByCode(sessionField.getText());

        System.out.println(allOperations);


        FXMLLoader loader = new FXMLLoader(getClass().getResource("JoinDocument.fxml"));
        Parent root = loader.load();


        Stage mainStage = (Stage) joinBtn.getScene().getWindow();
        mainStage.close();

        JoinDocument controller = loader.getController();
        controller.setUpDocument(allOperations, sessionId);

        Stage newDocStage = new Stage();
        Scene newDocScene = new Scene(root, 800, 600);
        newDocScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        newDocStage.setTitle("Real-Time Collaborative Text Editor");
        newDocStage.setScene(newDocScene);
        newDocStage.show();
        newDocStage.setMaximized(true);

    }

}
