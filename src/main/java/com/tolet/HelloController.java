package com.tolet;

import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
    private CheckBox rememberMe;
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

        if (rememberMe != null) {
            rememberMe.setSelected(DataStore.isKeepSignedInEnabled());
        }

        Platform.runLater(this::tryAutoLoginFromRememberedSession);
    }

    @FXML
    protected void onLoginClick(javafx.event.ActionEvent event) {
        String username = emailField.getText().trim();
        String password = isPasswordVisible ? passwordVisibleField.getText().trim() : passwordField.getText().trim();

        if ("admin".equals(username) && password.equals("140945")) {
            try {
                DataStore.currentUser = new User("System Admin", "admin@tolet.com", "140945", "Admin");
                DataStore.updateRememberedSession(rememberMe != null && rememberMe.isSelected());
                showStatus("Login Successful!", false);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                loadDashboardForCurrentUser(stage);
                return;
            } catch (Exception e) {
                showStatus("Error loading Admin Panel: " + getRootCauseMessage(e), true);
                e.printStackTrace();
                return;
            }
        }

        if (DataStore.validateUser(username, password)) {
            DataStore.updateRememberedSession(rememberMe != null && rememberMe.isSelected());
            showStatus("Login Successful!", false);

            try {
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                loadDashboardForCurrentUser(stage);
            } catch (Exception e) {
                showStatus("Error: " + getRootCauseMessage(e), true);
                e.printStackTrace();
            }
        } else {
            DataStore.clearRememberedSession();
            showStatus("Invalid Username or Password", true);
        }
    }

    @FXML
    protected void onSignUpClick() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(DataStore.resolveFxml("signup-view.fxml")));
            Stage stage = (Stage) themeToggle.getScene().getWindow();
            switchSceneRoot(stage, root);
        } catch (IOException e) {
            showStatus("Could not open signup page.", true);
            e.printStackTrace();
        }
    }

    @FXML
    protected void onForgotPassword(javafx.event.ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(DataStore.resolveFxml("forgot-password-view.fxml")));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            switchSceneRoot(stage, root);
        } catch (IOException e) {
            showStatus("Could not open password reset page.", true);
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
            switchSceneRoot(stage, root);
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

    private void tryAutoLoginFromRememberedSession() {
        if (!DataStore.restoreRememberedSession()) {
            return;
        }

        Stage stage = null;
        if (themeToggle != null && themeToggle.getScene() != null) {
            stage = (Stage) themeToggle.getScene().getWindow();
        } else if (emailField != null && emailField.getScene() != null) {
            stage = (Stage) emailField.getScene().getWindow();
        }

        if (stage == null) {
            return;
        }

        try {
            showStatus("Signed in automatically.", false);
            loadDashboardForCurrentUser(stage);
        } catch (IOException e) {
            DataStore.clearRememberedSession();
            DataStore.currentUser = null;
        }
    }

    private void loadDashboardForCurrentUser(Stage stage) throws IOException {
        String role = DataStore.currentUser != null ? DataStore.currentUser.getRole() : "";
        String fxmlFile;
        if (role != null && role.equalsIgnoreCase("Admin")) {
            fxmlFile = "admin-view-new.fxml";
        } else if (isOwnerRole(role)) {
            fxmlFile = "owner-view.fxml";
        } else {
            fxmlFile = "tenant-view.fxml";
        }

        Parent root = FXMLLoader.load(getClass().getResource(DataStore.resolveFxml(fxmlFile)));
        switchSceneRoot(stage, root);
    }

    private void switchSceneRoot(Stage stage, Parent root) {
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(new Scene(root));
        } else {
            DataStore.prepareSceneForRootSwap(scene);
            scene.setRoot(root);
        }
        stage.show();
    }

    private boolean isOwnerRole(String role) {
        if (role == null) {
            return false;
        }

        String normalized = role.trim().toLowerCase();
        return normalized.equals("house owner")
                || normalized.equals("owner")
                || normalized.equals("bariwala")
                || normalized.equals("landlord");
    }

    private String getRootCauseMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }

        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }

    private void showStatus(String message, boolean error) {
        if (statusLabel != null) {
            statusLabel.setText("");
        }

        Stage ownerStage = null;
        if (themeToggle != null && themeToggle.getScene() != null) {
            ownerStage = (Stage) themeToggle.getScene().getWindow();
        }

        if (!StatusPopupHelper.showStatusPopup(ownerStage, message, error) && statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle(error ? "-fx-text-fill: #d92d20;" : "-fx-text-fill: #159a62;");
        }
    }
}
