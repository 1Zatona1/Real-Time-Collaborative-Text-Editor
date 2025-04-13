package com.example.apt_project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.file.Files;
import java.sql.SQLOutput;

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
        FXMLLoader loader = new FXMLLoader(getClass().getResource("NewDocument.fxml"));
        Parent root = loader.load();

        Stage mainStage = (Stage) newDocBtn.getScene().getWindow();
        mainStage.close();

        Stage newDocStage = new Stage();
        Scene newDocScene = new Scene(root);

        newDocScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        newDocStage.setTitle("Real-Time Collaborative Text Editor");
        newDocStage.setScene(newDocScene);
        newDocStage.setMaximized(true); // Maximize new window
        newDocStage.show();
    }

    public void handleBrowse() throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a text file (.txt)");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);

        Stage browseDocStage = new Stage();
        userFile = fileChooser.showOpenDialog(browseDocStage);
        if (userFile != null)
        {
            try {
                userFileContent = Files.readString(userFile.toPath());

            }
            catch (IOException e)
            {
                System.out.println("Error Reading User File: (in HelloController.java)" + e);
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
            browseDocStage.setMaximized(true); // Maximize new window
            browseDocStage.show();
        }



    }
}