package com.example.apt_project;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;

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
}