package com.tolet;

import java.io.IOException;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

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

        if (passwordVisibleField != null) {
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
        }
    }

    @FXML
    protected void onLoginClick(javafx.event.ActionEvent event) {
        String username = emailField.getText().trim();
        String password = isPasswordVisible ? passwordVisibleField.getText().trim() : passwordField.getText().trim();

        if (username.equalsIgnoreCase("admin") && password.equals("140945")) {
            try {
                DataStore.currentUser = new User("System Admin", "admin@tolet.com", "140945", "Admin");
                Parent root = FXMLLoader.load(getClass().getResource(DataStore.resolveFxml("admin-view-new.fxml")));
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                DataStore.applyWindowSize(stage);
                stage.show();
                return;
            } catch (Exception e) {
                statusLabel.setText("Error loading Admin Panel: " + e.getMessage());
                statusLabel.setStyle("-fx-text-fill: red;");
                e.printStackTrace();
                return;
            }
        }

        if (DataStore.validateUser(username, password)) {
            statusLabel.setText("Login Successful!");
            statusLabel.setStyle("-fx-text-fill: green;");

            String role = DataStore.currentUser.getRole();
            String fxmlFile;
            if (role.equalsIgnoreCase("Admin")) {
                fxmlFile = "admin-view-new.fxml";
            } else if (role.equalsIgnoreCase("House Owner")) {
                fxmlFile = "owner-view.fxml";
            } else {
                fxmlFile = "tenant-view.fxml";
            }

            try {
                Parent root = FXMLLoader.load(getClass().getResource(DataStore.resolveFxml(fxmlFile)));
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

    @FXML
    protected void onSignUpClick() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(DataStore.resolveFxml("signup-view.fxml")));
            Stage stage = (Stage) themeToggle.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            DataStore.applyWindowSize(stage);
            stage.show();
        } catch (IOException e) {
            statusLabel.setText("Could not open signup page.");
            statusLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
    }

    @FXML
    protected void onForgotPassword(javafx.event.ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(DataStore.resolveFxml("forgot-password-view.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            DataStore.applyWindowSize(stage);
            stage.show();
        } catch (IOException e) {
            statusLabel.setText("Could not open password reset page.");
            statusLabel.setStyle("-fx-text-fill: red;");
            e.printStackTrace();
        }
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
            Parent root = FXMLLoader.load(getClass().getResource(DataStore.resolveFxml("login-view.fxml")));
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
