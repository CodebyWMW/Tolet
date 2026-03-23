package com.tolet;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import network.ClientConnection;

public class UserProfileController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:\\+?88)?01[3-9]\\d{8}$");

    @FXML
    private ToggleButton themeToggle;
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField birthdateField;
    @FXML
    private Label roleLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Label photoPathLabel;
    @FXML
    private ImageView avatarImageView;

    private String selectedPhotoPath;

    @FXML
    public void initialize() {
        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }
        clipAvatar();
        loadUserProfile();
    }

    private void clipAvatar() {
        if (avatarImageView == null) {
            return;
        }
        Circle clip = new Circle(53, 53, 53);
        avatarImageView.setClip(clip);
    }

    private void loadUserProfile() {
        if (DataStore.currentUser == null) {
            setMessage("No active user session found.", true);
            return;
        }

        int userId = DataStore.currentUser.getId();
        String currentName = safe(DataStore.currentUser.getUsername());
        String currentEmail = safe(DataStore.currentUser.getEmail());

        String command = "GET_USER_PROFILE|"
                + userId + "|"
                + encode(currentName) + "|"
                + encode(currentEmail);

        try {
            String response = ClientConnection.sendCommand(command);
            if (response == null || response.isBlank() || response.startsWith("NOT_FOUND")) {
                setMessage("Could not load profile from server.", true);
                return;
            }

            String[] parts = response.split("\\|", 10);
            if (parts.length < 10 || !"FOUND".equals(parts[0])) {
                setMessage("Invalid profile response from server.", true);
                return;
            }

            int resolvedUserId = Integer.parseInt(parts[1]);
            String name = decode(parts[2]);
            String email = decode(parts[3]);
            String phone = decode(parts[4]);
            String role = decode(parts[5]);
            String birthdate = decode(parts[6]);
            boolean verified = "1".equals(parts[7]);
            selectedPhotoPath = decode(parts[8]);
            String password = decode(parts[9]);

            DataStore.currentUser = new User(name, email, password, role, resolvedUserId);

            nameField.setText(name);
            emailField.setText(email);
            phoneField.setText(phone);
            birthdateField.setText(birthdate);
            roleLabel.setText("Role: " + (role.isBlank() ? "-" : role));
            statusLabel.setText("Status: " + (verified ? "Verified" : "Unverified"));
            photoPathLabel.setText(selectedPhotoPath.isBlank() ? "No photo selected" : selectedPhotoPath);
            applyAvatar(selectedPhotoPath);
        } catch (IOException | NumberFormatException e) {
            setMessage("Server error while loading profile.", true);
        }
    }

    @FXML
    private void onChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Profile Photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));

        Stage stage = (Stage) avatarImageView.getScene().getWindow();
        File selected = chooser.showOpenDialog(stage);
        if (selected == null) {
            return;
        }

        selectedPhotoPath = selected.getAbsolutePath();
        photoPathLabel.setText(selectedPhotoPath);
        applyAvatar(selectedPhotoPath);
    }

    @FXML
    private void onSaveProfile() {
        if (DataStore.currentUser == null) {
            setMessage("No active user session found.", true);
            return;
        }

        String name = safe(nameField.getText());
        String email = safe(emailField.getText());
        String phone = safe(phoneField.getText());
        String birthdate = safe(birthdateField.getText());

        if (name.isBlank()) {
            setMessage("Name cannot be empty.", true);
            return;
        }

        if (!email.isBlank() && !EMAIL_PATTERN.matcher(email).matches()) {
            setMessage("Enter a valid email address (example: user@example.com).", true);
            return;
        }

        if (!phone.isBlank() && !PHONE_PATTERN.matcher(normalizePhone(phone)).matches()) {
            setMessage("Enter a valid BD phone number (example: 01XXXXXXXXX).", true);
            return;
        }

        if (!birthdate.isBlank()) {
            try {
                LocalDate.parse(birthdate);
            } catch (DateTimeParseException e) {
                setMessage("Birthdate must be in YYYY-MM-DD format.", true);
                return;
            }
        }

        String normalizedEmail = email.isBlank() ? "" : email.toLowerCase();
        String normalizedPhone = normalizePhone(phone);

        int userId = resolveCurrentUserId();
        if (userId <= 0) {
            setMessage("Could not match the logged-in user in database.", true);
            return;
        }

        String command = "UPDATE_USER_PROFILE|"
                + userId + "|"
                + encode(name) + "|"
                + encode(normalizedEmail) + "|"
                + encode(normalizedPhone) + "|"
                + encode(birthdate) + "|"
                + encode(selectedPhotoPath) + "|"
                + encode(DataStore.currentUser.getPassword());

        try {
            String response = ClientConnection.sendCommand(command);
            if ("SUCCESS".equals(response)) {
                DataStore.currentUser = new User(
                        name,
                        normalizedEmail,
                        DataStore.currentUser.getPassword(),
                        DataStore.currentUser.getRole(),
                        userId);
                DataStore.updateRememberedSession(DataStore.isKeepSignedInEnabled());
                setMessage("Profile updated successfully.", false);
                return;
            }

            switch (response) {
                case "ERROR:DUPLICATE_NAME" -> setMessage("This full name is already used by another account.", true);
                case "ERROR:DUPLICATE_EMAIL" -> setMessage("This email is already used by another account.", true);
                case "ERROR:DUPLICATE_PHONE" -> setMessage("This phone number is already used by another account.", true);
                default -> setMessage("Profile update failed.", true);
            }
        } catch (IOException e) {
            setMessage("Server error while updating profile.", true);
        }
    }

    @FXML
    private void onBackToDashboard() {
        try {
            String role = DataStore.currentUser != null ? safe(DataStore.currentUser.getRole()).toLowerCase() : "";
            String baseFxml = role.contains("tenant") ? "tenant-view.fxml" : "owner-view.fxml";

            Stage stage = (Stage) themeToggle.getScene().getWindow();
            DataStore.rememberWindowState(stage);
            boolean wasMaximized = stage.isMaximized();
            boolean wasFullScreen = stage.isFullScreen();

            Parent root = FXMLLoader.load(getClass().getResource(DataStore.resolveFxml(baseFxml)));
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                DataStore.prepareSceneForRootSwap(scene);
                scene.setRoot(root);
            }

            stage.setMaximized(wasMaximized);
            stage.setFullScreen(wasFullScreen);
            stage.show();
        } catch (IOException e) {
            setMessage("Could not open dashboard.", true);
        }
    }

    @FXML
    private void onThemeToggle() {
        if (themeToggle == null) {
            return;
        }
        DataStore.darkMode = themeToggle.isSelected();
        try {
            Stage stage = (Stage) themeToggle.getScene().getWindow();
            DataStore.rememberWindowState(stage);
            boolean wasMaximized = stage.isMaximized();
            boolean wasFullScreen = stage.isFullScreen();

            Parent root = FXMLLoader.load(getClass().getResource(DataStore.resolveFxml("user-profile.fxml")));
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                DataStore.prepareSceneForRootSwap(scene);
                scene.setRoot(root);
            }
            stage.setMaximized(wasMaximized);
            stage.setFullScreen(wasFullScreen);
            stage.show();
        } catch (IOException e) {
            setMessage("Could not switch theme.", true);
        }
    }

    private void applyAvatar(String imagePath) {
        if (avatarImageView == null) {
            return;
        }
        if (imagePath == null || imagePath.isBlank()) {
            avatarImageView.setImage(null);
            return;
        }

        File file = new File(imagePath);
        if (!file.exists()) {
            avatarImageView.setImage(null);
            return;
        }

        try {
            avatarImageView.setImage(new Image(file.toURI().toString(), false));
        } catch (Exception ignored) {
            avatarImageView.setImage(null);
        }
    }

    private void setMessage(String text, boolean error) {
        if (messageLabel == null) {
            return;
        }
        messageLabel.setText(text == null ? "" : text);
        if (error) {
            messageLabel.setStyle("-fx-text-fill: #f73122;");
        } else {
            messageLabel.setStyle(DataStore.darkMode ? "-fx-text-fill: #ff7547;" : "-fx-text-fill: #D4AF37;");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePhone(String phone) {
        String value = safe(phone);
        if (value.startsWith("+88")) {
            return value.substring(3);
        }
        if (value.startsWith("88") && value.length() == 13) {
            return value.substring(2);
        }
        return value;
    }

    private int resolveCurrentUserId() {
        if (DataStore.currentUser == null) {
            return -1;
        }

        if (DataStore.currentUser.getId() > 0) {
            return DataStore.currentUser.getId();
        }

        String name = safe(DataStore.currentUser.getUsername());
        String email = safe(DataStore.currentUser.getEmail());

        String command = "RESOLVE_USER_ID|" + encode(email) + "|" + encode(name);
        try {
            String response = ClientConnection.sendCommand(command);
            if (response != null && response.startsWith("FOUND|")) {
                String[] parts = response.split("\\|", 2);
                if (parts.length == 2) {
                    return Integer.parseInt(parts[1]);
                }
            }
        } catch (IOException | NumberFormatException e) {
            return -1;
        }

        return -1;
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", " ").replace("\r", "").replace("\n", "<NL>").trim();
    }

    private String decode(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("<NL>", "\n").trim();
    }
}
