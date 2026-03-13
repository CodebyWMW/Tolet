package com.tolet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import database.DatabaseConnection;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class OwnerListHouseController {
    private static final int MAX_IMAGES = 3;

    @FXML
    private ToggleButton themeToggle;
    @FXML
    private TextField shortDetailField;
    @FXML
    private TextField bedsField;
    @FXML
    private TextField bathsField;
    @FXML
    private TextField locationField;
    @FXML
    private TextArea detailsArea;
    @FXML
    private ToggleButton tagSingleButton;
    @FXML
    private ToggleButton tagFamilyButton;
    @FXML
    private ToggleButton tagWorkspaceButton;
    @FXML
    private ChoiceBox<String> availabilityChoice;
    @FXML
    private CheckBox gasCheck;
    @FXML
    private CheckBox waterCheck;
    @FXML
    private CheckBox currentCheck;
    @FXML
    private TextField askingPriceField;
    @FXML
    private TextField contactField;
    @FXML
    private Label imageCountLabel;
    @FXML
    private Label imageListLabel;
    @FXML
    private Label statusMessageLabel;

    private final List<File> selectedImages = new ArrayList<>();

    @FXML
    public void initialize() {
        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }

        if (availabilityChoice != null) {
            availabilityChoice.setItems(FXCollections.observableArrayList(
                    "Available Now",
                    "Available Within 30 Days",
                    "Currently Occupied"));
            availabilityChoice.getSelectionModel().selectFirst();
        }

        if (DataStore.currentUser != null) {
            String phoneOrEmail = DataStore.currentUser.getEmail();
            if (phoneOrEmail == null || phoneOrEmail.isBlank()) {
                phoneOrEmail = DataStore.currentUser.getUsername();
            }
            if (contactField != null && phoneOrEmail != null) {
                contactField.setText(phoneOrEmail);
            }
        }

        refreshImageLabels();
        setStatus("", false);
    }

    @FXML
    private void onBackToOwnerDashboard() {
        try {
            Stage stage = (Stage) shortDetailField.getScene().getWindow();
            loadScene(stage, "owner-view.fxml");
        } catch (IOException e) {
            setStatus("Could not open Owner Dashboard.", true);
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
            loadScene(stage, "owner-list-house.fxml");
        } catch (IOException e) {
            setStatus("Could not switch theme.", true);
        }
    }

    @FXML
    private void onUploadHouseImages() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select up to 3 house images");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));

        Stage stage = (Stage) shortDetailField.getScene().getWindow();
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files == null || files.isEmpty()) {
            return;
        }

        selectedImages.clear();
        for (File file : files) {
            if (selectedImages.size() == MAX_IMAGES) {
                break;
            }
            selectedImages.add(file);
        }

        refreshImageLabels();
        if (files.size() > MAX_IMAGES) {
            setStatus("Only the first 3 images were selected.", false);
        } else {
            setStatus("Images selected successfully.", false);
        }
    }

    @FXML
    private void onResetForm() {
        clearForm();
        setStatus("Form reset.", false);
    }

    private void clearForm() {
        clear(shortDetailField);
        clear(bedsField);
        clear(bathsField);
        clear(locationField);
        clear(detailsArea);
        clear(askingPriceField);
        clear(contactField);

        setSelected(tagSingleButton, false);
        setSelected(tagFamilyButton, false);
        setSelected(tagWorkspaceButton, false);
        setSelected(gasCheck, false);
        setSelected(waterCheck, false);
        setSelected(currentCheck, false);
        if (availabilityChoice != null) {
            availabilityChoice.getSelectionModel().selectFirst();
        }

        selectedImages.clear();
        refreshImageLabels();
    }

    @FXML
    private void onSubmitHouseListing() {
        String shortDetail = text(shortDetailField);
        String location = text(locationField);
        String details = text(detailsArea);
        String contact = text(contactField);
        String availability = availabilityChoice != null ? availabilityChoice.getValue() : "Available Now";

        if (shortDetail.isBlank() || location.isBlank() || contact.isBlank()) {
            setStatus("Short detail, location and contact are required.", true);
            return;
        }

        if (selectedImages.isEmpty()) {
            setStatus("Please upload at least one house picture.", true);
            return;
        }

        int beds;
        int baths;
        double price;
        try {
            beds = Integer.parseInt(text(bedsField));
            baths = Integer.parseInt(text(bathsField));
            price = Double.parseDouble(text(askingPriceField));
        } catch (NumberFormatException e) {
            setStatus("Beds, baths and asking price must be valid numbers.", true);
            return;
        }

        if (beds < 0 || baths < 0 || price <= 0) {
            setStatus("Beds/baths cannot be negative and price must be greater than 0.", true);
            return;
        }

        int ownerId = resolveCurrentOwnerId();
        if (ownerId <= 0) {
            setStatus("Unable to resolve owner account. Please log in again.", true);
            return;
        }

        String tags = buildTags();
        String type = resolvePrimaryType();

        String insertHouse = "INSERT INTO houses (title, short_detail, location, details, tags, availability, type, bedrooms, bathrooms, gas_available, water_available, current_available, rent, contact_info, owner_id, image, approval_status, family_allowed, bachelor_allowed) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertImage = "INSERT INTO house_images (house_id, image_name, image_data, sort_order) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect()) {
            conn.setAutoCommit(false);
            try {
                int houseId;
                try (PreparedStatement pstmt = conn.prepareStatement(insertHouse, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, shortDetail);
                    pstmt.setString(2, shortDetail);
                    pstmt.setString(3, location);
                    pstmt.setString(4, details);
                    pstmt.setString(5, tags);
                    pstmt.setString(6, availability);
                    pstmt.setString(7, type);
                    pstmt.setInt(8, beds);
                    pstmt.setInt(9, baths);
                    pstmt.setInt(10, gasCheck.isSelected() ? 1 : 0);
                    pstmt.setInt(11, waterCheck.isSelected() ? 1 : 0);
                    pstmt.setInt(12, currentCheck.isSelected() ? 1 : 0);
                    pstmt.setDouble(13, price);
                    pstmt.setString(14, contact);
                    pstmt.setInt(15, ownerId);
                    pstmt.setString(16, null);
                    pstmt.setString(17, "pending");
                    pstmt.setInt(18, tagFamilyButton.isSelected() ? 1 : 0);
                    pstmt.setInt(19, tagSingleButton.isSelected() ? 1 : 0);
                    pstmt.executeUpdate();

                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Failed to retrieve house id.");
                        }
                        houseId = keys.getInt(1);
                    }
                }

                try (PreparedStatement imageStmt = conn.prepareStatement(insertImage)) {
                    int order = 1;
                    for (File file : selectedImages) {
                        imageStmt.setInt(1, houseId);
                        imageStmt.setString(2, file.getName());
                        imageStmt.setBytes(3, Files.readAllBytes(file.toPath()));
                        imageStmt.setInt(4, order++);
                        imageStmt.addBatch();
                    }
                    imageStmt.executeBatch();
                }

                try (PreparedStatement updateCover = conn
                        .prepareStatement("UPDATE houses SET image = ? WHERE id = ?")) {
                    updateCover.setString(1, "db-image://house/" + houseId + "/1");
                    updateCover.setInt(2, houseId);
                    updateCover.executeUpdate();
                }

                conn.commit();
                clearForm();
                setStatus("House listed successfully. It is now pending admin approval.", false);
            } catch (Exception e) {
                conn.rollback();
                setStatus("Failed to save listing: " + e.getMessage(), true);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            setStatus("Database error: " + e.getMessage(), true);
        }
    }

    private int resolveCurrentOwnerId() {
        if (DataStore.currentUser == null) {
            return -1;
        }

        String query = "SELECT id FROM users WHERE lower(email) = lower(?) OR name = ? COLLATE NOCASE LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, DataStore.currentUser.getEmail());
            pstmt.setString(2, DataStore.currentUser.getUsername());
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

    private String buildTags() {
        List<String> tags = new ArrayList<>();
        if (tagSingleButton.isSelected()) {
            tags.add("Single");
        }
        if (tagFamilyButton.isSelected()) {
            tags.add("Family");
        }
        if (tagWorkspaceButton.isSelected()) {
            tags.add("Workspace/Office");
        }
        return tags.isEmpty() ? "General" : String.join(", ", tags);
    }

    private String resolvePrimaryType() {
        if (tagFamilyButton.isSelected()) {
            return "Family";
        }
        if (tagSingleButton.isSelected()) {
            return "Single";
        }
        if (tagWorkspaceButton.isSelected()) {
            return "Workspace";
        }
        return "General";
    }

    private String text(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private String text(TextArea area) {
        return area == null || area.getText() == null ? "" : area.getText().trim();
    }

    private void clear(TextField field) {
        if (field != null) {
            field.clear();
        }
    }

    private void clear(TextArea area) {
        if (area != null) {
            area.clear();
        }
    }

    private void setSelected(ToggleButton button, boolean selected) {
        if (button != null) {
            button.setSelected(selected);
        }
    }

    private void setSelected(CheckBox checkBox, boolean selected) {
        if (checkBox != null) {
            checkBox.setSelected(selected);
        }
    }

    private void refreshImageLabels() {
        if (imageCountLabel != null) {
            imageCountLabel.setText(selectedImages.size() + "/" + MAX_IMAGES + " selected");
        }

        if (imageListLabel != null) {
            if (selectedImages.isEmpty()) {
                imageListLabel.setText("No images selected");
            } else {
                StringBuilder names = new StringBuilder();
                for (int i = 0; i < selectedImages.size(); i++) {
                    names.append(i + 1)
                            .append(". ")
                            .append(selectedImages.get(i).getName());
                    if (i < selectedImages.size() - 1) {
                        names.append("\n");
                    }
                }
                imageListLabel.setText(names.toString());
            }
        }
    }

    private void setStatus(String message, boolean error) {
        if (message == null || message.isBlank()) {
            if (statusMessageLabel != null) {
                statusMessageLabel.setText("");
            }
            return;
        }

        if (statusMessageLabel != null) {
            statusMessageLabel.setText("");
        }

        showStatusPopup(message, error);
    }

    private void showStatusPopup(String message, boolean error) {
        Stage ownerStage = null;
        if (themeToggle != null && themeToggle.getScene() != null) {
            ownerStage = (Stage) themeToggle.getScene().getWindow();
        }

        if (!StatusPopupHelper.showStatusPopup(ownerStage, message, error) && statusMessageLabel != null) {
            statusMessageLabel.setText(message);
            statusMessageLabel.setStyle(error ? "-fx-text-fill: #f73122;" : "-fx-text-fill: #2256c7;");
        }
    }

    private void loadScene(Stage stage, String baseFxml) throws IOException {
        DataStore.rememberWindowState(stage);
        boolean wasMaximized = stage.isMaximized();
        boolean wasFullScreen = stage.isFullScreen();

        Parent root = FXMLLoader.load(getClass().getResource(
                DataStore.resolveFxml(baseFxml)));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        if (!wasFullScreen) {
            DataStore.applyWindowSize(stage);
        }
        stage.setMaximized(wasMaximized);
        stage.setFullScreen(wasFullScreen);
        stage.show();
    }
}
