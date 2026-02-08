package com.tolet;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> roleSelector;
    @FXML
    private Label errorLabel;
    @FXML
    private ToggleButton themeToggle;

    @FXML
    public void initialize() {
        roleSelector.getItems().addAll("Admin", "House Owner", "Tenant");
        roleSelector.setValue("Tenant");
        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }
        Platform.runLater(() -> DataStore.applyTheme(usernameField.getScene()));
    }

    @FXML
    protected void onLoginClick(ActionEvent event) throws IOException {
        String user = usernameField.getText();
        String pass = passwordField.getText();
        String role = roleSelector.getValue();

        if (user == null || user.isBlank() || pass == null || pass.isBlank() || role == null || role.isBlank()) {
            errorLabel.setText("Enter username, password, and role.");
            errorLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if (DataStore.validate(user, pass, role)) {
            String fxmlFile = "";
            switch (role) {
                case "Admin":
                    fxmlFile = "admin-view.fxml";
                    break;
                case "House Owner":
                    fxmlFile = "owner-view.fxml";
                    break;
                case "Tenant":
                    fxmlFile = "tenant-view.fxml";
                    break;
            }
            switchScene(event, fxmlFile);
        } else {
            errorLabel.setText("Invalid Login! Try user: 'owner' pass: '123'");
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void switchScene(ActionEvent event, String fxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxml));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        DataStore.applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    protected void onSignUpClick() {
        // Simple logic to just add a user directly for demo
        String user = usernameField.getText();
        String pass = passwordField.getText();
        String role = roleSelector.getValue();
        if (!user.isEmpty() && !pass.isEmpty()) {
            DataStore.users.add(new User(user, pass, role));
            errorLabel.setText("User Registered! Please Login.");
            errorLabel.setStyle("-fx-text-fill: green;");
        } else {
            errorLabel.setText("Enter user/pass to register.");
            errorLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void onThemeToggle() {
        DataStore.darkMode = themeToggle.isSelected();
        DataStore.applyTheme(themeToggle.getScene());
    }
}