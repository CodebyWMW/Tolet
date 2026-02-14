package com.tolet;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.*;

public class HelloController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordVisibleField;
    @FXML
    private Label statusLabel;
    @FXML
    private ComboBox<String> roleSelector;

    private boolean isPasswordVisible = false;
    private String generatedOTP;

    @FXML
    protected void onLoginClick(javafx.event.ActionEvent event) {
        String email = emailField.getText();
        String password = isPasswordVisible ? passwordVisibleField.getText() : passwordField.getText();

        // ---------------------------------------------------------
        // 🔓 MASTER KEY BYPASS (ADMIN ONLY)
        // ---------------------------------------------------------
        if (email.equalsIgnoreCase("admin") && password.equals("140945")) {
            try {
                // 1. Create a "Fake" Authenticated User
                DataStore.currentUser = new User("System Admin", "admin@tolet.com", "140945", "Admin");

                // 2. Direct Jump to Admin Dashboard
                Parent root = FXMLLoader.load(getClass().getResource("admin-view.fxml"));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                DataStore.applyTheme(scene); // Apply dark mode if active
                stage.setScene(scene);
                stage.show();

                System.out.println("⚠️ Admin Bypass Activated");
                return; // STOP here (Don't check database)

            } catch (IOException e) {
                statusLabel.setText("Error loading Admin Panel");
                e.printStackTrace();
            }
        }
        // ---------------------------------------------------------

        // Normal User Login Flow
        if (DataStore.validateUser(email, password)) {
            statusLabel.setText("Login Successful!");
            statusLabel.setStyle("-fx-text-fill: green;");

            // Route to correct dashboard based on Role
            String role = DataStore.currentUser.getRole();
            String fxmlFile = "";

            if (role.equalsIgnoreCase("Admin"))
                fxmlFile = "admin-view.fxml";
            else if (role.equalsIgnoreCase("House Owner"))
                fxmlFile = "owner-view.fxml";
            else
                fxmlFile = "tenant-view.fxml"; // Tenant is default

            try {
                Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                DataStore.applyTheme(scene);
                stage.setScene(scene);
                stage.show();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            statusLabel.setText("Invalid Email or Password");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // --- (KEEP ALL YOUR OTHER METHODS BELOW: onSignUpClick, onForgotPassword,
    // etc.) ---

    @FXML
    protected void onSignUpClick() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Account");
        dialog.setHeaderText("Join Project To-Let");

        ButtonType registerBtnType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerBtnType, ButtonType.CANCEL);

        VBox box = new VBox(10);
        TextField nameIn = new TextField();
        nameIn.setPromptText("Full Name");
        TextField emailIn = new TextField();
        emailIn.setPromptText("Email or Phone");
        PasswordField passIn = new PasswordField();
        passIn.setPromptText("Password");
        ComboBox<String> roleIn = new ComboBox<>();
        roleIn.getItems().addAll("Tenant", "House Owner");
        roleIn.setValue("Tenant");

        box.getChildren().addAll(new Label("Name:"), nameIn, new Label("Contact:"), emailIn, new Label("Password:"),
                passIn, new Label("I am a:"), roleIn);
        dialog.getDialogPane().setContent(box);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == registerBtnType) {
            if (DataStore.registerUser(nameIn.getText(), emailIn.getText(), passIn.getText(), roleIn.getValue())) {
                statusLabel.setText("Account Created! Please Login.");
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                statusLabel.setText("Registration Failed (User exists).");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        }
    }

    @FXML
    protected void onForgotPassword() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Forgot Password");
        dialog.setHeaderText("Enter your registered email:");
        dialog.setContentText("Email:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(email -> {
            if (DataStore.emailExists(email)) {
                sendOTP(email);
                verifyOTP(email);
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Email not found!");
                alert.show();
            }
        });
    }

    private void verifyOTP(String email) {
        TextInputDialog otpDialog = new TextInputDialog();
        otpDialog.setTitle("Verify OTP");
        otpDialog.setHeaderText("An OTP has been sent to " + email);
        otpDialog.setContentText("Enter Code:");

        Optional<String> otpResult = otpDialog.showAndWait();
        otpResult.ifPresent(code -> {
            if (code.equals(generatedOTP)) {
                showResetPasswordDialog(email);
            } else {
                statusLabel.setText("Wrong OTP.");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        });
    }

    private void showResetPasswordDialog(String email) {
        TextInputDialog passDialog = new TextInputDialog();
        passDialog.setTitle("Reset Password");
        passDialog.setHeaderText("Enter New Password:");

        Optional<String> newPass = passDialog.showAndWait();
        newPass.ifPresent(pass -> {
            DataStore.updatePassword(email, pass);
            statusLabel.setText("Password Reset Successfully!");
            statusLabel.setStyle("-fx-text-fill: green;");
        });
    }

    private void sendOTP(String recipientEmail) {
        // Use your existing Jakarta Mail logic here
        generatedOTP = String.valueOf((int) (Math.random() * 9000) + 1000);
        System.out.println("DEBUG OTP: " + generatedOTP);
    }

    @FXML
    protected void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordVisibleField.setText(passwordField.getText());
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
        } else {
            passwordField.setText(passwordVisibleField.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
        }
    }

    @FXML
    protected void onGoogleLogin() {
    }

    @FXML
    protected void onFacebookLogin() {
    }
}