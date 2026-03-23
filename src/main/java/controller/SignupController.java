package controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

        // Validation: name, password, role are required
        // Email OR phone is required (at least one)
        if (name.isEmpty() || password.isEmpty() || role == null) {
            messageLabel.setText("Please fill name, password, and role!");
            return;
        }

        // At least one of email or phone must be provided
        if ((email == null || email.isEmpty()) && (phone == null || phone.isEmpty())) {
            messageLabel.setText("Please provide email or phone number!");
            return;
        }

        User user = new User(name, email, password, role, phone);

        UserService service = new UserService();
        boolean success = service.registerUser(user);

        if (success) {
            messageLabel.setText("Registration successful!");
            clearFields();
        } else {
            String error = service.getLastErrorMessage();
            messageLabel.setText(error == null || error.isBlank() ? "Registration failed!" : error);
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