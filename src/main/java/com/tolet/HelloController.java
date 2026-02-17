package com.tolet;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.application.Platform;
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
    @FXML
    private ToggleButton themeToggle;
    @FXML
    private MediaView liveMediaView;

    private boolean isPasswordVisible = false;
    private String generatedOTP;

    @FXML
    public void initialize() {
        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }
        if (liveMediaView != null) {
            String mediaFile = liveMediaView.getUserData() != null
                    ? liveMediaView.getUserData().toString()
                    : "live.mp4";
            String mediaUrl = getClass().getResource("/com/tolet/" + mediaFile).toExternalForm();
            MediaPlayer player = new MediaPlayer(new Media(mediaUrl));
            liveMediaView.setOpacity(0.0);
            liveMediaView.setSmooth(true);
            liveMediaView.setCache(true);
            player.setAutoPlay(true);
            player.setMute(true);
            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setOnReady(() -> {
                liveMediaView.setOpacity(1.0);
                player.play();
            });
            liveMediaView.setMediaPlayer(player);
        }
    }

    @FXML
    protected void onLoginClick(javafx.event.ActionEvent event) {
        String username = emailField.getText().trim();
        String password = isPasswordVisible ? passwordVisibleField.getText().trim() : passwordField.getText().trim();

        System.out.println("DEBUG - Username: '" + username + "' | Password: '" + password + "'");

        // ---------------------------------------------------------
        // 🔓 MASTER KEY BYPASS (ADMIN ONLY)
        // ---------------------------------------------------------
        if (username.equalsIgnoreCase("admin") && password.equals("140945")) {
            try {
                // 1. Create a "Fake" Authenticated User
                DataStore.currentUser = new User("System Admin", "admin@tolet.com", "140945", "Admin");

                // 2. Direct Jump to Admin Dashboard
                String resolvedFxml = DataStore.resolveFxml("admin-view-new.fxml");
                System.out.println("Loading: " + resolvedFxml);
                Parent root = FXMLLoader.load(getClass().getResource(resolvedFxml));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                DataStore.applyWindowSize(stage);
                stage.show();

                System.out.println("⚠️ Admin Bypass Activated");
                return; // STOP here (Don't check database)

            } catch (Exception e) {
                statusLabel.setText("Error loading Admin Panel: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
                e.printStackTrace();
            }
        }
        // ---------------------------------------------------------

        // Normal User Login Flow
        if (DataStore.validateUser(username, password)) {
            statusLabel.setText("Login Successful!");
            statusLabel.setStyle("-fx-text-fill: green;");

            // Route to correct dashboard based on Role
            String role = DataStore.currentUser.getRole();
            String fxmlFile = "";

            if (role.equalsIgnoreCase("Admin"))
                fxmlFile = "admin-view-new.fxml";
            else if (role.equalsIgnoreCase("House Owner"))
                fxmlFile = "owner-view.fxml";
            else
                fxmlFile = "tenant-view.fxml"; // Tenant is default

            try {
                String resolvedFxml = DataStore.resolveFxml(fxmlFile);
                System.out.println("Loading: " + resolvedFxml);
                Parent root = FXMLLoader.load(getClass().getResource(resolvedFxml));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                DataStore.applyWindowSize(stage);
                stage.show();
            } catch (Exception e) {
                statusLabel.setText("Error: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
                e.printStackTrace();
            }

        } else {
            statusLabel.setText("Invalid Username or Password");
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
        nameIn.setPromptText("Username");
        TextField emailIn = new TextField();
        emailIn.setPromptText("Email or Phone");
        PasswordField passIn = new PasswordField();
        passIn.setPromptText("Password");
        ComboBox<String> roleIn = new ComboBox<>();
        roleIn.getItems().addAll("Tenant", "Owner");
        roleIn.setValue("Tenant");

        box.getChildren().addAll(new Label("Username:"), nameIn, new Label("Email/Phone:"), emailIn,
                new Label("Password:"), passIn, new Label("Role (Tenant/Owner):"), roleIn);
        dialog.getDialogPane().setContent(box);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == registerBtnType) {
            String username = nameIn.getText().trim();
            String email = emailIn.getText().trim();
            String password = passIn.getText().trim();
            String role = roleIn.getValue();
            if ("Owner".equalsIgnoreCase(role)) {
                role = "House Owner";
            }

            // Validate inputs
            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                statusLabel.setText("All fields are required!");
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            // Check if username already exists (case-insensitive)
            if (DataStore.usernameExists(username)) {
                statusLabel.setText("Username already taken!");
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            // Check if email already exists
            if (email.contains("@") && DataStore.emailExists(email)) {
                statusLabel.setText("Email already registered!");
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            if (DataStore.registerUser(username, email, password, role)) {
                statusLabel.setText("Account Created! Please Login.");
                statusLabel.setStyle("-fx-text-fill: green;");
            } else {
                statusLabel.setText("Registration Failed.");
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
    protected void onThemeToggle() {
        if (themeToggle == null) {
            return;
        }
        DataStore.darkMode = themeToggle.isSelected();

        try {
            Parent root = FXMLLoader.load(getClass().getResource(
                    DataStore.resolveFxml("login-view.fxml")));
            Stage stage = (Stage) themeToggle.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            DataStore.applyWindowSize(stage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void onGoogleLogin() {
    }

    @FXML
    protected void onFacebookLogin() {
    }
}