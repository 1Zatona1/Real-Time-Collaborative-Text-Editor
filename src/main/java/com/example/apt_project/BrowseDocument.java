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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

    public void setFileContent(String content)
    {
        this.fileContent = content;
        if (codeArea != null) {
            codeArea.replaceText(fileContent);
        }
    }

    @FXML
    public void  initialize()
    {
        codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area");
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        HBox.setHgrow(codeArea, javafx.scene.layout.Priority.ALWAYS);
        codeArea.prefWidthProperty().bind(mainContainer.widthProperty());

        mainContainer.getChildren().add(1, codeArea);

        if (fileContent != null) {
            codeArea.replaceText(fileContent);
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
