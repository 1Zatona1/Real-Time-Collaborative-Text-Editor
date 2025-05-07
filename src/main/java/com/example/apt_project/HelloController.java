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
import java.util.List;

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
    public Label titleLabel;


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


        // Now proceed with loading the next scene
        FXMLLoader loader = new FXMLLoader(getClass().getResource("BrowseDocument.fxml"));
        Parent root = loader.load();

        Stage mainStage = (Stage) browseBtn.getScene().getWindow();
        mainStage.close();

        Stage browseDocStage = new Stage();
        Scene browseDocScene = new Scene(root);
        browseDocScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        browseDocStage.setTitle("Real-Time Collaborative Text Editor");
        browseDocStage.setScene(browseDocScene);
        browseDocStage.setMaximized(true);
        browseDocStage.show();
    }

    @FXML
    public void handleJoinBtn() throws IOException {

        List<String> allOperations = HttpHelper.getListOfOperation(sessionField.getText());
        if (allOperations == null) {
            // Code to handle error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid session code");
            alert.setHeaderText(null);
            alert.setContentText("Please enter a valid session code");
            alert.showAndWait();
            return;
        }
        String sessionId = HttpHelper.getDocumentIdByCode(sessionField.getText());
        int numEditors = HttpHelper.getNumberOfEditors(sessionId);
        if (numEditors == 4 && sessionField.getText().startsWith("E")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Maximum Editors Reached");
            alert.setHeaderText(null);
            alert.setContentText("This session already has the maximum number of 4 editors.");
            alert.showAndWait();
            return;
        }


        System.out.println(allOperations);


        FXMLLoader loader = new FXMLLoader(getClass().getResource("JoinDocument.fxml"));
        Parent root = loader.load();


        Stage mainStage = (Stage) joinBtn.getScene().getWindow();
        mainStage.close();

        final JoinDocument controller = loader.getController();
        controller.setUpDocument(allOperations, sessionId, sessionField.getText());

        Stage newDocStage = new Stage();
        Scene newDocScene = new Scene(root, 800, 600);
        newDocScene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
        newDocStage.setTitle("Real-Time Collaborative Text Editor");
        newDocStage.setScene(newDocScene);
        newDocStage.show();
        newDocStage.setMaximized(true);

        newDocStage.setOnCloseRequest(event -> {
            // Run your cleanup code here (e.g., notify server, close sockets)
            System.out.println("User is closing the window");

            // Optional: access the controller if needed
            controller.handleWindowClose(); // define this method in your controller if needed
        });

    }

}
