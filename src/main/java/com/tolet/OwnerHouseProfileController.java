package com.tolet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

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
import network.ClientConnection;

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
            beds = parseWholeNumber(text(bedsField), "Beds");
            baths = parseWholeNumber(text(bathsField), "Baths");
            areaSqft = parseDecimalNumber(text(areaField), "Square feet");
            price = parseDecimalNumber(text(askingPriceField), "Asking price");
        } catch (NumberFormatException e) {
            setStatus(e.getMessage(), true);
            return;
        }

        if (beds < 0 || baths < 0 || areaSqft <= 0 || price <= 0) {
            setStatus("Beds/baths cannot be negative, and square feet and price must be greater than 0.", true);
            return;
        }

        String tags = buildTags();
        String type = resolvePrimaryType();

        String updateCommand = "UPDATE_OWNER_HOUSE|"
                + houseId + "|"
                + ownerId + "|"
                + encode(shortDetail) + "|"
                + encode(shortDetail) + "|"
                + encode(location) + "|"
                + encode(details) + "|"
                + encode(tags) + "|"
                + encode(availability) + "|"
                + encode(type) + "|"
                + beds + "|"
                + baths + "|"
                + areaSqft + "|"
                + (gasCheck.isSelected() ? 1 : 0) + "|"
                + (waterCheck.isSelected() ? 1 : 0) + "|"
                + (currentCheck.isSelected() ? 1 : 0) + "|"
                + price + "|"
                + encode(contact) + "|"
                + (tagFamilyButton.isSelected() ? 1 : 0) + "|"
                + (tagSingleButton.isSelected() ? 1 : 0);

        try {
            boolean updated = "SUCCESS".equals(ClientConnection.sendCommand(updateCommand));
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
        } catch (IOException e) {
            setStatus("Failed to update listing. Please try again.", true);
        }
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

        try {
            String response = ClientConnection.sendCommand("DELETE_OWNER_HOUSE|" + houseId + "|" + ownerId);
            if ("SUCCESS".equals(response)) {
                DataStore.selectedHouseId = -1;
                setStatus("Listing deleted.", false);
                onBackToProperties();
            } else {
                setStatus("Could not delete listing.", true);
            }
        } catch (IOException e) {
            setStatus("Could not delete listing.", true);
        }
    }

    private void loadHouseDetails() {
        if (houseId <= 0 || ownerId <= 0) {
            setStatus("No listing selected. Please return to Properties.", true);
            return;
        }

        try {
            String response = ClientConnection.sendCommand("GET_OWNER_HOUSE_DETAILS|" + houseId + "|" + ownerId);
            if (response == null || !response.startsWith("FOUND|")) {
                setStatus("Listing not found.", true);
                return;
            }

            String[] parts = response.split("\\|", 20);
            if (parts.length < 19) {
                setStatus("Listing details are incomplete.", true);
                return;
            }

            String title = decode(parts[1]);
            String shortDetail = decode(parts[2]);
            String location = decode(parts[3]);
            String details = decode(parts[4]);
            String tags = decode(parts[5]);
            String availability = decode(parts[6]);
            String contact = decode(parts[17]);
            String approvalStatus = decode(parts[18]);

            int bedrooms = Integer.parseInt(parts[8]);
            int bathrooms = Integer.parseInt(parts[9]);
            double area = Double.parseDouble(parts[10]);
            double rent = Double.parseDouble(parts[11]);

            boolean gas = "1".equals(parts[12]);
            boolean water = "1".equals(parts[13]);
            boolean current = "1".equals(parts[14]);
            boolean familyAllowed = "1".equals(parts[15]);
            boolean bachelorAllowed = "1".equals(parts[16]);

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
                if (availability != null && !availability.isBlank() && availabilityChoice.getItems().contains(availability)) {
                    availabilityChoice.setValue(availability);
                } else {
                    availabilityChoice.getSelectionModel().selectFirst();
                }
            }

            if (listingStatusLabel != null) {
                listingStatusLabel.setText("Status: " + normalizeStatus(approvalStatus));
            }

            loadHouseImages();
            refreshImageLabels();
        } catch (IOException | NumberFormatException e) {
            setStatus("Could not load listing details.", true);
        }
    }

    private void loadHouseImages() {
        List<ImageView> slots = new ArrayList<>();
        if (profileImage1 != null) {
            slots.add(profileImage1);
        }
        if (profileImage2 != null) {
            slots.add(profileImage2);
        }
        if (profileImage3 != null) {
            slots.add(profileImage3);
        }

        for (ImageView slot : slots) {
            slot.setImage(null);
            updateImageSlotStyle(slot, false);
        }

        existingImageNames.clear();

        try {
            List<String> rows = ClientConnection.sendCommandForLines("GET_HOUSE_IMAGES|" + houseId, "END");
            int index = 0;
            for (String row : rows) {
                if ("NO_IMAGES".equals(row)) {
                    break;
                }

                String[] parts = row.split("\\|", 3);
                if (parts.length < 3) {
                    continue;
                }

                String imageName = decode(parts[0]);
                byte[] bytes = Base64.getDecoder().decode(parts[2]);
                if (index < slots.size()) {
                    Image image = new Image(new ByteArrayInputStream(bytes), 170, 130, true, true);
                    ImageView slot = slots.get(index);
                    slot.setImage(image);
                    updateImageSlotStyle(slot, true);
                }
                if (!imageName.isBlank()) {
                    existingImageNames.add(imageName);
                }
                index++;
            }
        } catch (IOException | IllegalArgumentException e) {
            // Keep placeholders if images cannot be loaded.
        }
    }

    private boolean replaceHouseImages() {
        try {
            String clearResponse = ClientConnection.sendCommand("CLEAR_HOUSE_IMAGES|" + houseId);
            if (!"SUCCESS".equals(clearResponse)) {
                return false;
            }

            int order = 1;
            for (File file : selectedImages) {
                String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
                String response = ClientConnection.sendCommand(
                        "ADD_HOUSE_IMAGE|" + houseId + "|" + encode(file.getName()) + "|" + order + "|" + base64);
                if (!"SUCCESS".equals(response)) {
                    return false;
                }
                order++;
            }

            String coverResponse = ClientConnection.sendCommand(
                    "SET_HOUSE_COVER|" + houseId + "|" + encode("db-image://house/" + houseId + "/1"));
            return "SUCCESS".equals(coverResponse);
        } catch (IOException e) {
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

        try {
            String response = ClientConnection.sendCommand("RESOLVE_USER_ID|"
                    + encode(DataStore.currentUser.getEmail()) + "|"
                    + encode(DataStore.currentUser.getUsername()));
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

    private int parseWholeNumber(String rawValue, String fieldName) {
        String normalized = normalizeNumericInput(rawValue);
        if (normalized.isBlank()) {
            throw new NumberFormatException(fieldName + " is required.");
        }
        if (!normalized.matches("-?\\d+")) {
            throw new NumberFormatException(fieldName + " must be a whole number.");
        }
        return Integer.parseInt(normalized);
    }

    private double parseDecimalNumber(String rawValue, String fieldName) {
        String normalized = normalizeNumericInput(rawValue);
        if (normalized.isBlank()) {
            throw new NumberFormatException(fieldName + " is required.");
        }
        if (!normalized.matches("-?\\d+(\\.\\d+)?")) {
            throw new NumberFormatException(fieldName + " must be a valid number.");
        }
        return Double.parseDouble(normalized);
    }

    private String normalizeNumericInput(String rawValue) {
        if (rawValue == null) {
            return "";
        }

        String compact = rawValue.trim()
                .replace(",", "")
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace("৳", "")
                .replace("$", "")
                .replaceAll("(?i)(bdt|tk)", "");

        StringBuilder converted = new StringBuilder(compact.length());
        for (int i = 0; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (c >= '\u09E6' && c <= '\u09EF') {
                converted.append((char) ('0' + (c - '\u09E6')));
            } else if (c >= '\u0660' && c <= '\u0669') {
                converted.append((char) ('0' + (c - '\u0660')));
            } else if (c >= '\u06F0' && c <= '\u06F9') {
                converted.append((char) ('0' + (c - '\u06F0')));
            } else {
                converted.append(c);
            }
        }

        return converted.toString();
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
            names.append(i + 1).append(". ").append(files.get(i).getName());
            if (i < files.size() - 1) {
                names.append("\n");
            }
        }
        return names.toString();
    }

    private String buildExistingNames() {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < existingImageNames.size(); i++) {
            names.append(i + 1).append(". ").append(existingImageNames.get(i));
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
