package com.tolet;

import java.io.IOException;
import java.time.Year;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

public class SignupPageController {

    @FXML
    private TextField emailField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private ComboBox<String> monthCombo;
    @FXML
    private ComboBox<String> dayCombo;
    @FXML
    private ComboBox<String> yearCombo;
    @FXML
    private CheckBox marketingCheck;
    @FXML
    private CheckBox termsCheck;
    @FXML
    private Label statusLabel;
    @FXML
    private ToggleButton themeToggle;

    @FXML
    public void initialize() {
        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }

        monthCombo.getItems().add("MM");
        for (int m = 1; m <= 12; m++) {
            monthCombo.getItems().add(String.format("%02d", m));
        }
        monthCombo.setValue("MM");

        dayCombo.getItems().add("DD");
        for (int d = 1; d <= 31; d++) {
            dayCombo.getItems().add(String.format("%02d", d));
        }
        dayCombo.setValue("DD");

        yearCombo.getItems().add("YYYY");
        int thisYear = Year.now().getValue();
        for (int y = thisYear; y >= 1950; y--) {
            yearCombo.getItems().add(String.valueOf(y));
        }
        yearCombo.setValue("YYYY");
    }

    @FXML
    private void onCreateAccount() {
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        String month = monthCombo.getValue();
        String day = dayCombo.getValue();
        String year = yearCombo.getValue();

        if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            setStatus("Please fill all required fields.", false);
            return;
        }

        if ("MM".equals(month) || "DD".equals(day) || "YYYY".equals(year)) {
            setStatus("Please select a valid date of birth.", false);
            return;
        }

        if (!password.equals(confirmPassword)) {
            setStatus("Passwords do not match.", false);
            return;
        }

        if (!termsCheck.isSelected()) {
            setStatus("Please accept Terms of Service and Privacy Policy.", false);
            return;
        }

        if (DataStore.usernameExists(username)) {
            setStatus("Username already taken.", false);
            return;
        }

        if (email.contains("@") && DataStore.emailExists(email)) {
            setStatus("Email already registered.", false);
            return;
        }

        String birthdate = year + "-" + month + "-" + day;
        boolean created = DataStore.registerUser(username, email, password, "Tenant", birthdate);
        if (created) {
            setStatus("Account created successfully. Please sign in.", true);
        } else {
            setStatus("Registration failed. Try another email/username.", false);
        }
    }

    @FXML
    private void onBackToSignIn(ActionEvent event) {
        navigate(event, DataStore.resolveFxml("login-view.fxml"));
    }

    @FXML
    private void onThemeToggle(ActionEvent event) {
        if (themeToggle == null) {
            return;
        }
        DataStore.darkMode = themeToggle.isSelected();
        navigate(event, DataStore.resolveFxml("signup-view.fxml"));
    }

    private void navigate(ActionEvent event, String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            DataStore.applyWindowSize(stage);
            stage.show();
        } catch (IOException e) {
            setStatus("Could not open page.", false);
            e.printStackTrace();
        }
    }

    private void setStatus(String text, boolean success) {
        statusLabel.setText(text);
        statusLabel.setStyle(success ? "-fx-text-fill: #7dff9b;" : "-fx-text-fill: #ff8a8a;");
    }
}
