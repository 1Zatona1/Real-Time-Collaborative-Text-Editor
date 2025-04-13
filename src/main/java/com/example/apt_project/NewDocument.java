package com.example.apt_project;


import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.flowless.VirtualizedScrollPane;




public class NewDocument {
    @FXML
    public VBox sidebar;
    public HBox mainContainer;
    public CodeArea codeArea;

    @FXML
    public void initialize() {
        codeArea = new CodeArea();
        codeArea.setWrapText(true);
        codeArea.setStyle("-fx-font-size: 14px; -fx-font-family: 'Consolas';");

        HBox.setHgrow(codeArea, javafx.scene.layout.Priority.ALWAYS);
        codeArea.prefWidthProperty().bind(mainContainer.widthProperty());

        mainContainer.getChildren().add(1, codeArea);


    }
}
