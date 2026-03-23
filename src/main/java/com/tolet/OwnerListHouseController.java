package com.tolet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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
import network.ClientConnection;

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
        clear(areaField);
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

        int ownerId = resolveCurrentOwnerId();
        if (ownerId <= 0) {
            setStatus("Unable to resolve owner account. Please log in again.", true);
            return;
        }

        String tags = buildTags();
        String type = resolvePrimaryType();

        String createCommand = "CREATE_OWNER_HOUSE|"
                + ownerId + "|"
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
                + (tagSingleButton.isSelected() ? 1 : 0) + "|"
                + encode(shortDetail);

        try {
            String createResponse = ClientConnection.sendCommand(createCommand);
            if (createResponse == null || !createResponse.startsWith("SUCCESS|")) {
                setStatus("Failed to save listing.", true);
                return;
            }

            int houseId = Integer.parseInt(createResponse.split("\\|", 2)[1]);
            int order = 1;
            for (File file : selectedImages) {
                String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
                String imageCommand = "ADD_HOUSE_IMAGE|"
                        + houseId + "|"
                        + encode(file.getName()) + "|"
                        + order + "|"
                        + base64;
                String imageResponse = ClientConnection.sendCommand(imageCommand);
                if (!"SUCCESS".equals(imageResponse)) {
                    setStatus("Listing saved but image upload failed.", true);
                    return;
                }
                order++;
            }

            String coverResponse = ClientConnection.sendCommand(
                    "SET_HOUSE_COVER|" + houseId + "|" + encode("db-image://house/" + houseId + "/1"));
            if (!"SUCCESS".equals(coverResponse)) {
                setStatus("Listing saved but cover image failed.", true);
                return;
            }

            clearForm();
            setStatus("House listed successfully. It is now pending admin approval.", false);
        } catch (IOException | NumberFormatException e) {
            setStatus("Failed to save listing: " + e.getMessage(), true);
        }
    }

    private int resolveCurrentOwnerId() {
        if (DataStore.currentUser == null) {
            return -1;
        }

        String command = "RESOLVE_USER_ID|"
                + encode(DataStore.currentUser.getEmail()) + "|"
                + encode(DataStore.currentUser.getUsername());
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

    @FXML
    private void onAddLocationFromMap() {
        // TODO: Implement Google Maps integration to get house location
        // This method will be implemented in the future
    }
}
