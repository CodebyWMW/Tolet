package com.tolet;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import java.io.IOException;

public class AdminController {
    @FXML
    private ToggleButton themeToggle;

    @FXML
    public void initialize() {
        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }
        Platform.runLater(() -> DataStore.applyTheme(themeToggle.getScene()));
    }

    @FXML
    private void onBack(ActionEvent event) throws IOException {
        switchToLogin(event);
    }

    @FXML
    private void onLogout(ActionEvent event) throws IOException {
        DataStore.currentUser = null;
        switchToLogin(event);
    }

    private void switchToLogin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("login-view.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        DataStore.applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void onThemeToggle() {
        DataStore.darkMode = themeToggle.isSelected();
        DataStore.applyTheme(themeToggle.getScene());
    }
}
