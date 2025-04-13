package com.example.apt_project;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BrowseDocument {
    public HBox mainContainer;
    public VBox sidebar;
    public CodeArea codeArea;
    public String fileContent;
    public Button exportBtn;

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
        codeArea.setWrapText(true);
        codeArea.setStyle("-fx-font-size: 14px; -fx-font-family: 'Consolas';");

        HBox.setHgrow(codeArea, javafx.scene.layout.Priority.ALWAYS);
        codeArea.prefWidthProperty().bind(mainContainer.widthProperty());

        mainContainer.getChildren().add(1, codeArea);

        if (fileContent != null) {
            codeArea.replaceText(fileContent);
        }
    }

    public void handleExport() throws IOException
    {
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


}
