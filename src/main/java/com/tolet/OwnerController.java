package com.tolet;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import database.DatabaseConnection;

public class OwnerController {
    @FXML
    private ToggleButton themeToggle;
    @FXML
    private Label profileNameLabel;
    @FXML
    private Label profileInitialLabel;
    @FXML
    private Label profileRoleLabel;
    @FXML
    private Label profileVerificationLabel;
    @FXML
    private Label profileEmailLabel;
    @FXML
    private Label profilePhoneLabel;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label kpiIncomeLabel;
    @FXML
    private Label kpiOccupancyLabel;
    @FXML
    private Label kpiPendingLabel;
    @FXML
    private VBox requestsContainer;
    @FXML
    private VBox listedHousesContainer;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",
            Locale.ENGLISH);
    private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy",
            Locale.ENGLISH);

    @FXML
    public void initialize() {
        if (themeToggle != null)
            themeToggle.setSelected(DataStore.darkMode);

        populateProfile();
        populateKpis();
        populateRequests();
        populateListedHouses();
    }

    private void populateProfile() {
        ProfileMeta profileMeta = resolveProfileMeta();
        if (profileNameLabel != null)
            profileNameLabel.setText(profileMeta.name);
        if (profileInitialLabel != null)
            profileInitialLabel.setText(profileMeta.initial);
        if (profileVerificationLabel != null)
            profileVerificationLabel.setText("Status - " + (profileMeta.verified ? "Verified" : "Unverified"));
        if (profileRoleLabel != null)
            profileRoleLabel.setText("Role - " + sanitizeRole(profileMeta.role));
        String phone = profileMeta.phone == null ? "" : profileMeta.phone.trim();
        String email = profileMeta.email == null ? "" : profileMeta.email.trim();
        boolean hasPhone = !phone.isBlank() && !"-".equals(phone);
        boolean hasEmail = !email.isBlank() && !"-".equals(email);

        // Show only one contact field: phone has priority over email.
        if (hasPhone) {
            if (profilePhoneLabel != null) {
                profilePhoneLabel.setText("Phone: " + phone);
                profilePhoneLabel.setVisible(true);
                profilePhoneLabel.setManaged(true);
            }
            if (profileEmailLabel != null) {
                profileEmailLabel.setVisible(false);
                profileEmailLabel.setManaged(false);
            }
        } else if (hasEmail) {
            if (profileEmailLabel != null) {
                profileEmailLabel.setText("Email: " + email);
                profileEmailLabel.setVisible(true);
                profileEmailLabel.setManaged(true);
            }
            if (profilePhoneLabel != null) {
                profilePhoneLabel.setVisible(false);
                profilePhoneLabel.setManaged(false);
            }
        } else {
            if (profileEmailLabel != null) {
                profileEmailLabel.setText("Contact: -");
                profileEmailLabel.setVisible(true);
                profileEmailLabel.setManaged(true);
            }
            if (profilePhoneLabel != null) {
                profilePhoneLabel.setVisible(false);
                profilePhoneLabel.setManaged(false);
            }
        }
        if (welcomeLabel != null)
            welcomeLabel.setText("Welcome back, " + profileMeta.name + "!");
        if (dateLabel != null)
            dateLabel.setText(LocalDate.now().format(DATE_FORMAT));
    }

    private ProfileMeta resolveProfileMeta() {
        String fallbackName = "Owner";
        String fallbackRole = "-";
        String fallbackEmail = "-";
        String fallbackPhone = "-";

        if (DataStore.currentUser == null) {
            return new ProfileMeta(fallbackName, fallbackRole, fallbackEmail, fallbackPhone, false);
        }

        String currentName = DataStore.currentUser.getUsername();
        String currentEmail = DataStore.currentUser.getEmail();
        int currentUserId = DataStore.currentUser.getId();
        String name = currentName != null && !currentName.isBlank() ? currentName : fallbackName;
        String role = fallbackRole;
        String email = fallbackEmail;
        String phone = fallbackPhone;
        boolean verified = false;

        String query;
        boolean queryById = currentUserId > 0;
        if (queryById) {
            query = "SELECT name, role, email, phone, verified FROM users WHERE id = ? LIMIT 1";
        } else {
            query = "SELECT name, role, email, phone, verified FROM users WHERE name = ? COLLATE NOCASE OR lower(email) = lower(?) LIMIT 1";
        }

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            if (queryById) {
                pstmt.setInt(1, currentUserId);
            } else {
                pstmt.setString(1, currentName);
                pstmt.setString(2, currentEmail);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dbName = rs.getString("name");
                    String dbRole = rs.getString("role");
                    String dbEmail = rs.getString("email");
                    String dbPhone = rs.getString("phone");

                    if (dbName != null && !dbName.isBlank())
                        name = dbName;
                    if (dbRole != null && !dbRole.isBlank())
                        role = dbRole;
                    if (dbEmail != null && !dbEmail.isBlank())
                        email = dbEmail;
                    if (dbPhone != null && !dbPhone.isBlank())
                        phone = dbPhone;

                    verified = rs.getInt("verified") == 1;
                }
            }
        } catch (SQLException e) {
            verified = false;
        }

        return new ProfileMeta(name, role, email, phone, verified);
    }

    private String sanitizeRole(String role) {
        if (role == null) {
            return "-";
        }
        String cleaned = role.replaceAll("(?i)\\s*\\((verified|unverified)\\)\\s*", "").trim();
        return cleaned.isBlank() ? "-" : cleaned;
    }

    private static class ProfileMeta {
        private final String name;
        private final String role;
        private final String email;
        private final String phone;
        private final boolean verified;
        private final String initial;

        private ProfileMeta(String name, String role, String email, String phone, boolean verified) {
            this.name = name;
            this.role = role;
            this.email = email;
            this.phone = phone;
            this.verified = verified;
            this.initial = (name != null && !name.isBlank()) ? String.valueOf(Character.toUpperCase(name.charAt(0))) : "O";
        }
    }

    private void populateKpis() {
        String ownerName = DataStore.currentUser != null ? DataStore.currentUser.getUsername() : null;
        double income = 0;
        int houseCount = 0;
        for (House house : DataStore.getHouses()) {
            if (ownerName == null || ownerName.isBlank() || ownerName.equalsIgnoreCase(house.getOwnerName())) {
                income += house.getRent();
                houseCount++;
            }
        }

        int pendingCount = 0;
        int approvedCount = 0;
        for (BookingRequest request : DataStore.getBookingRequests()) {
            String status = request.getStatus();
            if (status != null) {
                if (status.equalsIgnoreCase("Pending"))
                    pendingCount++;
                if (status.equalsIgnoreCase("Approved"))
                    approvedCount++;
            }
        }

        int occupancy = 0;
        if (houseCount > 0) {
            occupancy = (int) Math.min(100, Math.round((approvedCount * 100.0) / houseCount));
        }

        if (kpiIncomeLabel != null)
            kpiIncomeLabel.setText(formatCurrencyCompact(income));
        if (kpiPendingLabel != null)
            kpiPendingLabel.setText(String.valueOf(pendingCount));
        if (kpiOccupancyLabel != null)
            kpiOccupancyLabel.setText(occupancy + "%");
    }

    private void populateRequests() {
        if (requestsContainer == null)
            return;
        requestsContainer.getChildren().clear();
        for (BookingRequest request : DataStore.getBookingRequests()) {
            requestsContainer.getChildren().add(buildRequestRow(request));
        }
    }

    private void populateListedHouses() {
        if (listedHousesContainer == null) {
            return;
        }

        listedHousesContainer.getChildren().clear();

        int ownerId = resolveCurrentOwnerId();
        if (ownerId <= 0) {
            listedHousesContainer.getChildren().add(buildEmptyListedRow("No owner account found."));
            return;
        }

        String query = "SELECT id, "
                + "COALESCE(NULLIF(TRIM(title), ''), location, 'Untitled House') AS title, "
                + "COALESCE(location, '-') AS location, "
                + "COALESCE(rent, 0) AS rent, "
                + "COALESCE(approval_status, 'pending') AS approval_status "
                + "FROM houses WHERE owner_id = ? ORDER BY id DESC LIMIT 12";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean hasRows = false;
                while (rs.next()) {
                    hasRows = true;
                    String title = rs.getString("title");
                    String location = rs.getString("location");
                    double rent = rs.getDouble("rent");
                    String status = rs.getString("approval_status");
                    listedHousesContainer.getChildren().add(buildListedHouseRow(title, location, rent, status));
                }
                if (!hasRows) {
                    listedHousesContainer.getChildren().add(buildEmptyListedRow("No houses listed yet."));
                }
            }
        } catch (SQLException e) {
            listedHousesContainer.getChildren().add(buildEmptyListedRow("Could not load house listings."));
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

    private HBox buildListedHouseRow(String title, String location, double rent, String approvalStatus) {
        HBox row = new HBox(16);
        row.getStyleClass().add("table-row");

        Label titleLabel = buildCellLabel(title == null ? "Untitled House" : title);
        Label locationLabel = buildCellLabel(location == null ? "-" : location);
        Label rentLabel = buildCellLabel(formatRent(rent));

        String normalized = normalizeListingStatus(approvalStatus);
        Label status = new Label(normalized);
        status.getStyleClass().add("status-pill");
        status.getStyleClass().add(normalized.toLowerCase(Locale.ENGLISH));

        row.getChildren().addAll(titleLabel, locationLabel, rentLabel, status);
        return row;
    }

    private HBox buildEmptyListedRow(String message) {
        HBox row = new HBox(16);
        row.getStyleClass().add("table-row");
        Label label = buildCellLabel(message);
        row.getChildren().add(label);
        return row;
    }

    private String normalizeListingStatus(String status) {
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

    private HBox buildRequestRow(BookingRequest request) {
        HBox row = new HBox(16);
        row.getStyleClass().add("table-row");

        Label tenant = buildCellLabel(request.getTenantName());
        Label property = buildCellLabel(request.getProperty());
        Label requestDate = buildCellLabel(formatDate(request.getRequestDate()));
        Label moveInDate = buildCellLabel(formatDate(request.getMoveInDate()));
        Label rent = buildCellLabel(formatRent(request.getMonthlyRent()));

        Label status = new Label(normalizeStatus(request.getStatus()));
        status.getStyleClass().add("status-pill");
        status.getStyleClass().add(normalizeStatus(request.getStatus()).toLowerCase(Locale.ENGLISH));

        Button approve = new Button("Approve");
        approve.getStyleClass().add("btn-approve");
        Button deny = new Button("Deny");
        deny.getStyleClass().add("btn-deny");

        HBox actions = new HBox(8, approve, deny);

        row.getChildren().addAll(tenant, property, requestDate, moveInDate, rent, status, actions);
        return row;
    }

    private Label buildCellLabel(String text) {
        Label label = new Label(text == null ? "" : text);
        label.getStyleClass().add("table-text");
        return label;
    }

    private String formatDate(LocalDate date) {
        if (date == null)
            return "";
        return date.format(REQUEST_DATE_FORMAT);
    }

    private String formatRent(double rent) {
        return "৳" + String.format("%,.0f", rent);
    }

    private String formatCurrencyCompact(double amount) {
        if (amount >= 1000) {
            double value = amount / 1000.0;
            String formatted = value >= 10 ? String.format("%.0f", value) : String.format("%.1f", value);
            return "৳" + formatted + "k";
        }
        return "৳" + String.format("%.0f", amount);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank())
            return "Pending";
        String value = status.trim().toLowerCase(Locale.ENGLISH);
        if (value.startsWith("approve"))
            return "Approved";
        if (value.startsWith("deny") || value.startsWith("reject"))
            return "Denied";
        return "Pending";
    }

    @FXML
    private void onLogout(ActionEvent event) throws IOException {
        if (showLogoutConfirmation()) {
            DataStore.currentUser = null;
            switchToLogin(event);
        }
    }

    @FXML
    private void onOpenListHouse(ActionEvent event) {
        Stage stage = null;
        if (event != null && event.getSource() instanceof Node) {
            stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        } else if (themeToggle != null && themeToggle.getScene() != null) {
            stage = (Stage) themeToggle.getScene().getWindow();
        }

        if (stage == null) {
            showNavigationError("Unable to locate the current window for navigation.");
            return;
        }

        String resolved = DataStore.resolveFxml("owner-list-house.fxml");
        URL viewUrl = getClass().getResource(resolved);

        // Fallback to light view when the resolved themed file is unavailable.
        if (viewUrl == null && resolved != null && resolved.endsWith("-dark.fxml")) {
            viewUrl = getClass().getResource("owner-list-house.fxml");
        }

        if (viewUrl == null) {
            showNavigationError("List House page is missing. Expected: " + resolved);
            return;
        }

        try {
            DataStore.rememberWindowState(stage);
            boolean wasMaximized = stage.isMaximized();
            boolean wasFullScreen = stage.isFullScreen();

            Parent root = FXMLLoader.load(viewUrl);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            if (!wasFullScreen) {
                DataStore.applyWindowSize(stage);
            }
            stage.setMaximized(wasMaximized);
            stage.setFullScreen(wasFullScreen);
            stage.show();
        } catch (IOException e) {
            showNavigationError("Failed to open List House page: " + e.getMessage());
        }
    }

    private void showNavigationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Navigation Error");
        alert.setHeaderText("Could not open List New House page");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean showLogoutConfirmation() {
        try {
            AtomicBoolean confirmed = new AtomicBoolean(false);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("logout-popup.fxml"));
            Parent root = loader.load();

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.UNDECORATED);
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));

            // Make window draggable
            final double[] xOffset = { 0 };
            final double[] yOffset = { 0 };

            root.setOnMousePressed(event -> {
                xOffset[0] = event.getSceneX();
                yOffset[0] = event.getSceneY();
            });

            root.setOnMouseDragged(event -> {
                popupStage.setX(event.getScreenX() - xOffset[0]);
                popupStage.setY(event.getScreenY() - yOffset[0]);
            });

            // Get buttons from FXML
            Button yesButton = (Button) root.lookup("#yesButton");
            Button cancelButton = (Button) root.lookup("#cancelButton");

            yesButton.setOnAction(e -> {
                confirmed.set(true);
                popupStage.close();
            });

            cancelButton.setOnAction(e -> popupStage.close());

            popupStage.showAndWait();
            return confirmed.get();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void switchToLogin(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        loadScene(stage, "login-view.fxml");
    }

    @FXML
    private void onThemeToggle() {
        if (themeToggle == null) {
            return;
        }
        DataStore.darkMode = themeToggle.isSelected();
        try {
            Stage stage = (Stage) themeToggle.getScene().getWindow();
            loadScene(stage, "owner-view.fxml");
        } catch (IOException e) {
            e.printStackTrace();
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