package com.tolet;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

import database.DatabaseConnection;
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

        String query;
        boolean queryById = userId > 0;
        if (queryById) {
            query = "SELECT id, name, email, phone, role, birthdate, verified, profile_image FROM users WHERE id = ? LIMIT 1";
        } else {
            query = "SELECT id, name, email, phone, role, birthdate, verified, profile_image FROM users "
                    + "WHERE name = ? COLLATE NOCASE OR lower(ifnull(email, '')) = lower(?) LIMIT 1";
        }

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            if (queryById) {
                pstmt.setInt(1, userId);
            } else {
                pstmt.setString(1, currentName);
                pstmt.setString(2, currentEmail);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    setMessage("Could not load profile from database.", true);
                    return;
                }

                int resolvedUserId = rs.getInt("id");
                String name = safe(rs.getString("name"));
                String email = safe(rs.getString("email"));
                String phone = safe(rs.getString("phone"));
                String role = safe(rs.getString("role"));
                String birthdate = safe(rs.getString("birthdate"));
                boolean verified = rs.getInt("verified") == 1;
                selectedPhotoPath = safe(rs.getString("profile_image"));

                DataStore.currentUser = new User(
                        name,
                        email,
                        DataStore.currentUser.getPassword(),
                        role,
                        resolvedUserId);

                nameField.setText(name);
                emailField.setText(email);
                phoneField.setText(phone);
                birthdateField.setText(birthdate);
                roleLabel.setText("Role: " + (role.isBlank() ? "-" : role));
                statusLabel.setText("Status: " + (verified ? "Verified" : "Unverified"));
                photoPathLabel.setText(selectedPhotoPath.isBlank() ? "No photo selected" : selectedPhotoPath);
                applyAvatar(selectedPhotoPath);
            }
        } catch (SQLException e) {
            setMessage("Database error while loading profile.", true);
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

        String conflictMessage = findDuplicateFieldMessage(userId, name, normalizedEmail, normalizedPhone);
        if (conflictMessage != null) {
            setMessage(conflictMessage, true);
            return;
        }

        String update = "UPDATE users SET name = ?, email = ?, phone = ?, birthdate = ?, profile_image = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(update)) {
            pstmt.setString(1, name);
            pstmt.setString(2, normalizedEmail.isBlank() ? null : normalizedEmail);
            pstmt.setString(3, normalizedPhone.isBlank() ? null : normalizedPhone);
            pstmt.setString(4, birthdate.isBlank() ? null : birthdate);
            pstmt.setString(5, selectedPhotoPath == null || selectedPhotoPath.isBlank() ? null : selectedPhotoPath);
            pstmt.setInt(6, userId);

            int updated = pstmt.executeUpdate();
            if (updated <= 0) {
                setMessage("No profile changes were saved.", true);
                return;
            }

            DataStore.currentUser = new User(
                    name,
                    normalizedEmail,
                    DataStore.currentUser.getPassword(),
                    DataStore.currentUser.getRole(),
                    userId);

                // Keep remembered-session identity in sync when username/email changes.
                DataStore.updateRememberedSession(DataStore.isKeepSignedInEnabled());

            setMessage("Profile updated successfully.", false);
        } catch (SQLException e) {
            String message = e.getMessage() == null ? "Database update failed." : e.getMessage();
            if (message.toLowerCase().contains("unique")) {
                setMessage("Name, email, or phone already exists. Use a different value.", true);
            } else {
                setMessage("Database update failed.", true);
            }
        }
    }

    private String findDuplicateFieldMessage(int userId, String name, String email, String phone) {
        try (Connection conn = DatabaseConnection.connect()) {
            if (existsByName(conn, userId, name)) {
                return "This full name is already used by another account.";
            }
            if (!email.isBlank() && existsByEmail(conn, userId, email)) {
                return "This email is already used by another account.";
            }
            if (!phone.isBlank() && existsByPhone(conn, userId, phone)) {
                return "This phone number is already used by another account.";
            }
        } catch (SQLException e) {
            return "Database check failed. Please try again.";
        }
        return null;
    }

    private boolean existsByName(Connection conn, int userId, String name) throws SQLException {
        String query = "SELECT 1 FROM users WHERE id <> ? AND name = ? COLLATE NOCASE LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean existsByEmail(Connection conn, int userId, String email) throws SQLException {
        String query = "SELECT 1 FROM users WHERE id <> ? AND lower(ifnull(email, '')) = lower(?) LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean existsByPhone(Connection conn, int userId, String phone) throws SQLException {
        String query = "SELECT 1 FROM users WHERE id <> ? AND ifnull(phone, '') = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, phone);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
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

        String query = "SELECT id FROM users WHERE name = ? COLLATE NOCASE OR lower(ifnull(email, '')) = lower(?) LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            return -1;
        }

        return -1;
    }
}
