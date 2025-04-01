module com.example.apt_project {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires richtextfx;  // Add javafx.graphics as well for RichTextFX
    requires flowless;

    opens com.example.apt_project to javafx.fxml;
    exports com.example.apt_project;
}