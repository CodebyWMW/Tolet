package controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import models.User;
import services.UserService;

public class SignupController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField phoneField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private Label messageLabel;

    @FXML
    private void handleSignup() {

        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String phone = phoneField.getText();
        String role = roleComboBox.getValue();

        // Simple validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()
                || phone.isEmpty() || role == null) {

            messageLabel.setText("Please fill all fields!");
            return;
        }

        User user = new User(name, email, password, role, phone);

        UserService service = new UserService();
        boolean success = service.registerUser(user);

        if (success) {
            messageLabel.setText("Registration successful!");
            clearFields();
        } else {
            messageLabel.setText("Registration failed!");
        }
    }

    private void clearFields() {
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        phoneField.clear();
        roleComboBox.getSelectionModel().clearSelection();
    }
}