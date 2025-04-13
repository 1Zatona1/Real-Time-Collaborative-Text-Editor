package com.example.apt_project;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;

public class BrowseDocument {
    public HBox mainContainer;
    public VBox sidebar;
    public CodeArea codeArea;
    public String fileContent;

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

}
