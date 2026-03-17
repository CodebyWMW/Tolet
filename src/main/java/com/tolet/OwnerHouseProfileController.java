package com.tolet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dao.HouseDAO;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class OwnerHouseProfileController {
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
    private TextField areaField;
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
    @FXML
    private Label listingStatusLabel;
    @FXML
    private ImageView profileImage1;
    @FXML
    private ImageView profileImage2;
    @FXML
    private ImageView profileImage3;

    private final List<File> selectedImages = new ArrayList<>();
    private final List<String> existingImageNames = new ArrayList<>();
    private int houseId = -1;
    private int ownerId = -1;

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
        }

        ownerId = resolveCurrentOwnerId();
        houseId = DataStore.selectedHouseId;
        loadHouseDetails();
        refreshImageLabels();
        setStatus("", false);
    }

    @FXML
    private void onBackToProperties() {
        try {
            Stage stage = (Stage) shortDetailField.getScene().getWindow();
            loadScene(stage, "owner-properties.fxml");
        } catch (IOException e) {
            setStatus("Could not open Properties page.", true);
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
            loadScene(stage, "owner-house-profile.fxml");
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
    private void onResetChanges() {
        selectedImages.clear();
        loadHouseDetails();
        setStatus("Changes reset.", false);
    }

    @FXML
    private void onSaveUpdates() {
        if (houseId <= 0 || ownerId <= 0) {
            setStatus("No listing selected. Please return to Properties.", true);
            return;
        }

        String shortDetail = text(shortDetailField);
        String location = text(locationField);
        String details = text(detailsArea);
        String contact = text(contactField);
        String availability = availabilityChoice != null ? availabilityChoice.getValue() : "Available Now";

        if (shortDetail.isBlank() || location.isBlank() || contact.isBlank()) {
            setStatus("Short detail, location and contact are required.", true);
            return;
        }

        int beds;
        int baths;
        double areaSqft;
        double price;
        try {
            beds = Integer.parseInt(text(bedsField));
            baths = Integer.parseInt(text(bathsField));
            areaSqft = Double.parseDouble(text(areaField));
            price = Double.parseDouble(text(askingPriceField));
        } catch (NumberFormatException e) {
            setStatus("Beds, baths, square feet and asking price must be valid numbers.", true);
            return;
        }

        if (beds < 0 || baths < 0 || areaSqft <= 0 || price <= 0) {
            setStatus("Beds/baths cannot be negative, and square feet and price must be greater than 0.", true);
            return;
        }

        String tags = buildTags();
        String type = resolvePrimaryType();

        HouseDAO dao = new HouseDAO();
        boolean updated = dao.updateHouseDetails(
                houseId,
                ownerId,
                shortDetail,
                shortDetail,
                location,
                details,
                tags,
                availability,
                type,
                beds,
                baths,
                areaSqft,
                gasCheck.isSelected(),
                waterCheck.isSelected(),
                currentCheck.isSelected(),
                price,
                contact,
                tagFamilyButton.isSelected(),
                tagSingleButton.isSelected());

        if (!updated) {
            setStatus("Failed to update listing. Please try again.", true);
            return;
        }

        if (!selectedImages.isEmpty()) {
            if (!replaceHouseImages()) {
                setStatus("Listing updated, but images could not be updated.", true);
                loadHouseDetails();
                return;
            }
        }

        selectedImages.clear();
        loadHouseDetails();
        setStatus("Listing updated successfully.", false);
    }

    @FXML
    private void onDeleteListing() {
        if (houseId <= 0 || ownerId <= 0) {
            setStatus("No listing selected. Please return to Properties.", true);
            return;
        }

        Stage ownerStage = null;
        if (themeToggle != null && themeToggle.getScene() != null) {
            ownerStage = (Stage) themeToggle.getScene().getWindow();
        }

        boolean confirmed = StatusPopupHelper.showConfirmPopup(
                ownerStage,
                "Delete Listing",
                "Delete this listing? This will remove all related requests and images.",
                "Delete",
                "Cancel",
                true);

        if (!confirmed) {
            return;
        }

        HouseDAO dao = new HouseDAO();
        if (dao.deleteHouseById(houseId, ownerId)) {
            DataStore.selectedHouseId = -1;
            setStatus("Listing deleted.", false);
            onBackToProperties();
        } else {
            setStatus("Could not delete listing.", true);
        }
    }

    private void loadHouseDetails() {
        if (houseId <= 0 || ownerId <= 0) {
            setStatus("No listing selected. Please return to Properties.", true);
            return;
        }

        String query = "SELECT id, owner_id, title, short_detail, location, details, tags, availability, type, "
                + "bedrooms, bathrooms, area, gas_available, water_available, current_available, rent, "
                + "contact_info, approval_status, family_allowed, bachelor_allowed "
                + "FROM houses WHERE id = ? AND owner_id = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, houseId);
            pstmt.setInt(2, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    setStatus("Listing not found.", true);
                    return;
                }

                String title = safeString(rs.getString("title"));
                String shortDetail = safeString(rs.getString("short_detail"));
                String location = safeString(rs.getString("location"));
                String details = safeString(rs.getString("details"));
                String tags = safeString(rs.getString("tags"));
                String availability = safeString(rs.getString("availability"));
                String type = safeString(rs.getString("type"));
                String contact = safeString(rs.getString("contact_info"));
                String approvalStatus = safeString(rs.getString("approval_status"));

                int bedrooms = rs.getInt("bedrooms");
                int bathrooms = rs.getInt("bathrooms");
                double area = rs.getDouble("area");
                double rent = rs.getDouble("rent");

                boolean gas = rs.getInt("gas_available") == 1;
                boolean water = rs.getInt("water_available") == 1;
                boolean current = rs.getInt("current_available") == 1;

                boolean familyAllowed = rs.getInt("family_allowed") == 1;
                boolean bachelorAllowed = rs.getInt("bachelor_allowed") == 1;

                shortDetailField.setText(!shortDetail.isBlank() ? shortDetail : title);
                locationField.setText(location);
                detailsArea.setText(details);
                bedsField.setText(String.valueOf(bedrooms));
                bathsField.setText(String.valueOf(bathrooms));
                areaField.setText(String.format(Locale.ENGLISH, "%.0f", area));
                askingPriceField.setText(String.format(Locale.ENGLISH, "%.0f", rent));
                contactField.setText(contact);

                setSelected(gasCheck, gas);
                setSelected(waterCheck, water);
                setSelected(currentCheck, current);

                boolean tagFamily = familyAllowed || tags.toLowerCase(Locale.ENGLISH).contains("family");
                boolean tagSingle = bachelorAllowed || tags.toLowerCase(Locale.ENGLISH).contains("single");
                boolean tagWorkspace = tags.toLowerCase(Locale.ENGLISH).contains("workspace");

                setSelected(tagFamilyButton, tagFamily);
                setSelected(tagSingleButton, tagSingle);
                setSelected(tagWorkspaceButton, tagWorkspace);

                if (availabilityChoice != null) {
                    if (availability != null && !availability.isBlank()
                            && availabilityChoice.getItems().contains(availability)) {
                        availabilityChoice.setValue(availability);
                    } else {
                        availabilityChoice.getSelectionModel().selectFirst();
                    }
                }

                if (listingStatusLabel != null) {
                    listingStatusLabel.setText("Status: " + normalizeStatus(approvalStatus));
                }

                loadHouseImages();
                loadExistingImageNames();
                refreshImageLabels();
            }
        } catch (SQLException e) {
            setStatus("Could not load listing details.", true);
        }
    }

    private void loadHouseImages() {
        List<ImageView> slots = new ArrayList<>();
        if (profileImage1 != null)
            slots.add(profileImage1);
        if (profileImage2 != null)
            slots.add(profileImage2);
        if (profileImage3 != null)
            slots.add(profileImage3);

        for (ImageView slot : slots) {
            slot.setImage(null);
            updateImageSlotStyle(slot, false);
        }

        String sql = "SELECT image_data FROM house_images WHERE house_id = ? ORDER BY sort_order ASC, id ASC LIMIT 3";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, houseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                int index = 0;
                while (rs.next() && index < slots.size()) {
                    byte[] bytes = rs.getBytes("image_data");
                    if (bytes != null && bytes.length > 0) {
                        Image image = new Image(new ByteArrayInputStream(bytes), 170, 130, true, true);
                        ImageView slot = slots.get(index);
                        slot.setImage(image);
                        updateImageSlotStyle(slot, true);
                    }
                    index++;
                }
            }
        } catch (SQLException e) {
            // Keep placeholders empty if images cannot be loaded.
        }
    }

    private void loadExistingImageNames() {
        existingImageNames.clear();
        String sql = "SELECT image_name FROM house_images WHERE house_id = ? ORDER BY sort_order ASC, id ASC LIMIT 3";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, houseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("image_name");
                    if (name != null && !name.isBlank()) {
                        existingImageNames.add(name);
                    }
                }
            }
        } catch (SQLException e) {
            // Ignore missing image names.
        }
    }

    private boolean replaceHouseImages() {
        String deleteSql = "DELETE FROM house_images WHERE house_id = ?";
        String insertSql = "INSERT INTO house_images (house_id, image_name, image_data, sort_order) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                deleteStmt.setInt(1, houseId);
                deleteStmt.executeUpdate();

                int order = 1;
                for (File file : selectedImages) {
                    insertStmt.setInt(1, houseId);
                    insertStmt.setString(2, file.getName());
                    insertStmt.setBytes(3, Files.readAllBytes(file.toPath()));
                    insertStmt.setInt(4, order++);
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();

                try (PreparedStatement updateCover = conn
                        .prepareStatement("UPDATE houses SET image = ? WHERE id = ?")) {
                    updateCover.setString(1, "db-image://house/" + houseId + "/1");
                    updateCover.setInt(2, houseId);
                    updateCover.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void updateImageSlotStyle(ImageView imageView, boolean hasImage) {
        if (imageView == null || imageView.getParent() == null) {
            return;
        }

        if (imageView.getParent() instanceof StackPane slotPane) {
            if (hasImage) {
                if (!slotPane.getStyleClass().contains("property-detail-image-slot-has-image")) {
                    slotPane.getStyleClass().add("property-detail-image-slot-has-image");
                }
            } else {
                slotPane.getStyleClass().remove("property-detail-image-slot-has-image");
            }
        }
    }

    private int resolveCurrentOwnerId() {
        if (DataStore.currentUser == null) {
            return -1;
        }
        if (DataStore.currentUser.getId() > 0) {
            return DataStore.currentUser.getId();
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

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Pending";
        }
        String value = status.trim().toLowerCase(Locale.ENGLISH);
        if (value.startsWith("approve")) {
            return "Approved";
        }
        if (value.startsWith("reject") || value.startsWith("deny")) {
            return "Denied";
        }
        return "Pending";
    }

    private String text(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private String text(TextArea area) {
        return area == null || area.getText() == null ? "" : area.getText().trim();
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
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
        int count = selectedImages.isEmpty() ? existingImageNames.size() : selectedImages.size();
        if (imageCountLabel != null) {
            imageCountLabel.setText(count + "/" + MAX_IMAGES + " selected");
        }

        if (imageListLabel != null) {
            if (!selectedImages.isEmpty()) {
                imageListLabel.setText(buildImageNames(selectedImages));
            } else if (!existingImageNames.isEmpty()) {
                imageListLabel.setText(buildExistingNames());
            } else {
                imageListLabel.setText("No images uploaded");
            }
        }
    }

    private String buildImageNames(List<File> files) {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < files.size(); i++) {
            names.append(i + 1)
                    .append(". ")
                    .append(files.get(i).getName());
            if (i < files.size() - 1) {
                names.append("\n");
            }
        }
        return names.toString();
    }

    private String buildExistingNames() {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < existingImageNames.size(); i++) {
            names.append(i + 1)
                    .append(". ")
                    .append(existingImageNames.get(i));
            if (i < existingImageNames.size() - 1) {
                names.append("\n");
            }
        }
        return names.toString();
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
    }
}
