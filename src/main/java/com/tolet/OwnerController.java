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
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    @FXML
    private VBox requestsPageRoot;
    @FXML
    private VBox acceptedTenantsPageRoot;
    @FXML
    private VBox propertiesPageRoot;
    @FXML
    private VBox propertiesCardsContainer;
    @FXML
    private Label propertyTitleLabel;
    @FXML
    private Label propertyLocationLabel;
    @FXML
    private Label propertyTypeLabel;
    @FXML
    private Label propertyPriceLabel;
    @FXML
    private Label propertySizeLabel;
    @FXML
    private Label propertyAvailabilityLabel;
    @FXML
    private Label propertyContactLabel;
    @FXML
    private Label propertyDetailsLabel;
    @FXML
    private ImageView propertyImage1;
    @FXML
    private ImageView propertyImage2;
    @FXML
    private ImageView propertyImage3;
    @FXML
    private VBox propertyRequestsContainer;
    @FXML
    private VBox propertyReviewsContainer;
    @FXML
    private VBox analyticsPageRoot;
    @FXML
    private Label analyticsTotalPropertiesLabel;
    @FXML
    private Label analyticsTotalRequestsLabel;
    @FXML
    private Label analyticsApprovedRequestsLabel;
    @FXML
    private Label analyticsPendingRequestsLabel;
    @FXML
    private Label analyticsDeniedRequestsLabel;
    @FXML
    private Label analyticsAvgRentLabel;
    @FXML
    private Label analyticsEstimatedRevenueLabel;
    @FXML
    private Label analyticsApprovalRateLabel;
    @FXML
    private Label analyticsOccupancyRateLabel;
    @FXML
    private Label analyticsTopPropertyLabel;
    @FXML
    private Label analyticsLatestActivityLabel;
    @FXML
    private ProgressBar analyticsApprovalBar;
    @FXML
    private ProgressBar analyticsOccupancyBar;
    @FXML
    private LineChart<String, Number> analyticsRequestsTrendChart;

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
        populatePropertiesPage();
        populateAnalyticsPage();
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
            query = "SELECT name, role, email, phone, verified FROM users WHERE name = ? COLLATE BINARY OR lower(email) = lower(?) LIMIT 1";
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
        int houseCount = 0;
        for (House house : DataStore.getHouses()) {
            if (ownerName == null || ownerName.isBlank() || ownerName.equalsIgnoreCase(house.getOwnerName())) {
                houseCount++;
            }
        }

        double income = 0;
        int pendingCount = 0;
        int approvedCount = 0;
        for (BookingRequest request : DataStore.getBookingRequests()) {
            String normalizedStatus = normalizeStatus(request.getStatus());
            if ("Pending".equalsIgnoreCase(normalizedStatus)) {
                pendingCount++;
            }
            if ("Approved".equalsIgnoreCase(normalizedStatus)) {
                approvedCount++;
                income += request.getMonthlyRent();
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

        List<BookingRequest> source = DataStore.getBookingRequests();
        if (isAcceptedTenantsPage()) {
            List<BookingRequest> approvedOnly = new ArrayList<>();
            for (BookingRequest request : source) {
                if ("Approved".equalsIgnoreCase(normalizeStatus(request.getStatus()))) {
                    approvedOnly.add(request);
                }
            }
            source = approvedOnly;
        }

        int shown = 0;
        int maxRows = (isRequestsPage() || isAcceptedTenantsPage()) ? Integer.MAX_VALUE : 2;
        for (BookingRequest request : source) {
            if (shown >= maxRows) {
                break;
            }
            if (isAcceptedTenantsPage()) {
                requestsContainer.getChildren().add(buildAcceptedTenantRow(request));
            } else {
                requestsContainer.getChildren().add(buildRequestRow(request));
            }
            shown++;
        }

        if (shown == 0) {
            HBox row = new HBox(16);
            row.getStyleClass().add("table-row");
            String emptyMessage = isAcceptedTenantsPage()
                    ? "No approved tenants yet."
                    : "No booking requests found.";
            row.getChildren().add(buildCellLabel(emptyMessage));
            requestsContainer.getChildren().add(row);
        }
    }

    private boolean isRequestsPage() {
        return requestsPageRoot != null;
    }

    private boolean isAcceptedTenantsPage() {
        return acceptedTenantsPageRoot != null;
    }

    private boolean isPropertiesPage() {
        return propertiesPageRoot != null;
    }

    private boolean isAnalyticsPage() {
        return analyticsPageRoot != null;
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
                int shown = 0;
                int maxRows = isPropertiesPage() ? Integer.MAX_VALUE : 2;
                while (rs.next()) {
                    if (shown >= maxRows) {
                        break;
                    }
                    hasRows = true;
                    String title = rs.getString("title");
                    String location = rs.getString("location");
                    double rent = rs.getDouble("rent");
                    String status = rs.getString("approval_status");
                    listedHousesContainer.getChildren().add(buildListedHouseRow(title, location, rent, status));
                    shown++;
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

        String query = "SELECT id FROM users WHERE lower(email) = lower(?) OR name = ? COLLATE BINARY LIMIT 1";
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

    private static class HouseDetails {
        private final int id;
        private final String title;
        private final String location;
        private final String type;
        private final double rent;
        private final int bedrooms;
        private final int bathrooms;
        private final double area;
        private final String details;
        private final String availability;
        private final String contactInfo;
        private final String approvalStatus;

        private HouseDetails(int id, String title, String location, String type, double rent,
                int bedrooms, int bathrooms, double area, String details,
                String availability, String contactInfo, String approvalStatus) {
            this.id = id;
            this.title = title;
            this.location = location;
            this.type = type;
            this.rent = rent;
            this.bedrooms = bedrooms;
            this.bathrooms = bathrooms;
            this.area = area;
            this.details = details;
            this.availability = availability;
            this.contactInfo = contactInfo;
            this.approvalStatus = approvalStatus;
        }
    }

    private HouseDetails selectedHouse;

    private void populatePropertiesPage() {
        if (propertiesCardsContainer == null) {
            return;
        }

        propertiesCardsContainer.getChildren().clear();
        if (propertyRequestsContainer != null) {
            propertyRequestsContainer.getChildren().clear();
        }

        int ownerId = resolveCurrentOwnerId();
        if (ownerId <= 0) {
            propertiesCardsContainer.getChildren().add(buildCellLabel("No owner account found."));
            return;
        }

        List<HouseDetails> houses = new ArrayList<>();
        String query = "SELECT id, "
                + "COALESCE(NULLIF(TRIM(title), ''), location, 'Untitled House') AS title, "
                + "COALESCE(location, '-') AS location, "
                + "COALESCE(type, '-') AS type, "
                + "COALESCE(rent, 0) AS rent, "
                + "COALESCE(bedrooms, 0) AS bedrooms, "
                + "COALESCE(bathrooms, 0) AS bathrooms, "
                + "COALESCE(area, 0.0) AS area, "
                + "COALESCE(details, '-') AS details, "
                + "COALESCE(availability, '-') AS availability, "
                + "COALESCE(contact_info, '-') AS contact_info, "
                + "COALESCE(approval_status, 'pending') AS approval_status "
                + "FROM houses WHERE owner_id = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    houses.add(new HouseDetails(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("location"),
                            rs.getString("type"),
                            rs.getDouble("rent"),
                            rs.getInt("bedrooms"),
                            rs.getInt("bathrooms"),
                            rs.getDouble("area"),
                            rs.getString("details"),
                            rs.getString("availability"),
                            rs.getString("contact_info"),
                            rs.getString("approval_status")));
                }
            }
        } catch (SQLException e) {
            propertiesCardsContainer.getChildren().add(buildCellLabel("Could not load properties."));
            return;
        }

        if (houses.isEmpty()) {
            propertiesCardsContainer.getChildren().add(buildCellLabel("No houses listed yet."));
            selectedHouse = null;
            clearPropertyDetails();
            return;
        }

        Button firstCard = null;
        for (HouseDetails house : houses) {
            Button card = buildPropertyCard(house);
            propertiesCardsContainer.getChildren().add(card);
            if (firstCard == null) {
                firstCard = card;
            }
        }

        if (firstCard != null) {
            selectPropertyCard(firstCard);
        }
        showPropertyDetails(houses.get(0));
    }

    private Button buildPropertyCard(HouseDetails house) {
        Button card = new Button();
        card.getStyleClass().add("property-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        VBox content = new VBox(4);
        Label title = new Label(house.title == null ? "Untitled House" : house.title);
        title.getStyleClass().add("card-title");

        Label location = new Label(house.location == null ? "-" : house.location);
        location.getStyleClass().add("card-location");

        Label rent = new Label(formatRent(house.rent));
        rent.getStyleClass().add("card-price");

        Label status = new Label(normalizeListingStatus(house.approvalStatus));
        status.getStyleClass().add("card-badge");

        content.getChildren().addAll(title, location, rent, status);
        card.setGraphic(content);
        card.setOnAction(event -> {
            selectPropertyCard(card);
            showPropertyDetails(house);
        });
        return card;
    }

    private void selectPropertyCard(Button selectedCard) {
        if (propertiesCardsContainer == null || selectedCard == null) {
            return;
        }

        for (Node node : propertiesCardsContainer.getChildren()) {
            if (node instanceof Button buttonCard) {
                buttonCard.getStyleClass().remove("property-card-selected");
            }
        }

        if (!selectedCard.getStyleClass().contains("property-card-selected")) {
            selectedCard.getStyleClass().add("property-card-selected");
        }
    }

    private void showPropertyDetails(HouseDetails house) {
        if (house == null) {
            selectedHouse = null;
            clearPropertyDetails();
            return;
        }

        selectedHouse = house;

        if (propertyTitleLabel != null)
            propertyTitleLabel.setText("Title: " + safeText(house.title));
        if (propertyLocationLabel != null)
            propertyLocationLabel.setText("Location: " + safeText(house.location));
        if (propertyTypeLabel != null)
            propertyTypeLabel.setText("Type: " + safeText(house.type));
        if (propertyPriceLabel != null)
            propertyPriceLabel.setText("Rent: " + formatRent(house.rent));
        if (propertySizeLabel != null)
            propertySizeLabel.setText("Size: " + house.bedrooms + " bed, " + house.bathrooms + " bath, "
                    + String.format("%.0f", house.area) + " sqft");
        if (propertyAvailabilityLabel != null)
            propertyAvailabilityLabel.setText("Availability: " + safeText(house.availability));
        if (propertyContactLabel != null)
            propertyContactLabel.setText("Contact: " + safeText(house.contactInfo));
        if (propertyDetailsLabel != null)
            propertyDetailsLabel.setText("Details: " + safeText(house.details));
        populatePropertyImages(house.id);

        populateHouseRequests(house.id);
        populateHouseReviews(house.id);
    }

    private void populatePropertyImages(int houseId) {
        List<ImageView> slots = new ArrayList<>();
        if (propertyImage1 != null)
            slots.add(propertyImage1);
        if (propertyImage2 != null)
            slots.add(propertyImage2);
        if (propertyImage3 != null)
            slots.add(propertyImage3);

        if (slots.isEmpty()) {
            return;
        }

        for (ImageView slot : slots) {
            slot.setImage(null);
            updatePropertyImageSlotStyle(slot, false);
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
                        updatePropertyImageSlotStyle(slot, true);
                    }
                    index++;
                }
            }
        } catch (SQLException e) {
            // Keep placeholders empty if images cannot be loaded.
        }
    }

    private void updatePropertyImageSlotStyle(ImageView imageView, boolean hasImage) {
        if (imageView == null || imageView.getParent() == null) {
            return;
        }

        if (imageView.getParent() instanceof javafx.scene.layout.StackPane slotPane) {
            if (hasImage) {
                if (!slotPane.getStyleClass().contains("property-detail-image-slot-has-image")) {
                    slotPane.getStyleClass().add("property-detail-image-slot-has-image");
                }
            } else {
                slotPane.getStyleClass().remove("property-detail-image-slot-has-image");
            }
        }
    }

    private void populateHouseRequests(int houseId) {
        if (propertyRequestsContainer == null) {
            return;
        }

        propertyRequestsContainer.getChildren().clear();

        String query = "SELECT u.name AS tenant_name, r.request_date, r.move_in_date, r.status "
                + "FROM rent_requests r "
                + "JOIN users u ON u.id = r.tenant_id "
                + "WHERE r.house_id = ? "
                + "ORDER BY r.id DESC";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, houseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean hasRows = false;
                while (rs.next()) {
                    hasRows = true;
                    propertyRequestsContainer.getChildren().add(buildPropertyRequestRow(
                            rs.getString("tenant_name"),
                            parseDate(rs.getString("request_date")),
                            parseDate(rs.getString("move_in_date")),
                            rs.getString("status")));
                }
                if (!hasRows) {
                    HBox row = new HBox(16);
                    row.getStyleClass().add("table-row");
                    row.getChildren().add(buildWideMessageLabel("No tenant requests for this property."));
                    propertyRequestsContainer.getChildren().add(row);
                }
            }
        } catch (SQLException e) {
            HBox row = new HBox(16);
            row.getStyleClass().add("table-row");
            row.getChildren().add(buildWideMessageLabel("Could not load tenant requests."));
            propertyRequestsContainer.getChildren().add(row);
        }
    }

    private void populateHouseReviews(int houseId) {
        if (propertyReviewsContainer == null) {
            return;
        }

        propertyReviewsContainer.getChildren().clear();

        String query = "SELECT u.name AS tenant_name, hr.review_text, hr.status, "
                + "COALESCE(hr.updated_at, hr.created_at) AS review_date "
                + "FROM house_reviews hr "
                + "JOIN users u ON u.id = hr.tenant_id "
                + "WHERE hr.house_id = ? "
                + "AND lower(trim(COALESCE(hr.status, 'submitted'))) = 'submitted' "
                + "ORDER BY COALESCE(hr.updated_at, hr.created_at) DESC, hr.id DESC";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, houseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean hasRows = false;
                while (rs.next()) {
                    hasRows = true;
                    propertyReviewsContainer.getChildren().add(buildReviewRow(
                            rs.getString("tenant_name"),
                            rs.getString("review_date"),
                            rs.getString("review_text")));
                }
                if (!hasRows) {
                    HBox row = new HBox(16);
                    row.getStyleClass().add("table-row");
                    row.getChildren().add(buildWideMessageLabel("No submitted reviews for this property yet."));
                    propertyReviewsContainer.getChildren().add(row);
                }
            }
        } catch (SQLException e) {
            HBox row = new HBox(16);
            row.getStyleClass().add("table-row");
            row.getChildren().add(buildWideMessageLabel("Could not load reviews."));
            propertyReviewsContainer.getChildren().add(row);
        }
    }

    private HBox buildReviewRow(String tenantName, String reviewDate, String reviewText) {
        HBox row = new HBox(16);
        row.getStyleClass().add("table-row");

        Label tenant = buildCellLabel(tenantName == null || tenantName.isBlank() ? "Tenant" : tenantName);
        Label date = buildCellLabel(reviewDate == null || reviewDate.isBlank() ? "-" : reviewDate);
        Label text = buildWideMessageLabel(reviewText == null || reviewText.isBlank() ? "-" : reviewText);

        row.getChildren().addAll(tenant, date, text);
        return row;
    }

    private HBox buildPropertyRequestRow(String tenantName, LocalDate requestDate, LocalDate moveInDate, String statusText) {
        HBox row = new HBox(16);
        row.getStyleClass().add("table-row");

        Label tenant = buildCellLabel(tenantName);
        Label request = buildCellLabel(formatDate(requestDate));
        Label moveIn = buildCellLabel(formatDate(moveInDate));
        Label status = new Label(normalizeStatus(statusText));
        status.getStyleClass().add("status-pill");
        status.getStyleClass().add(normalizeStatus(statusText).toLowerCase(Locale.ENGLISH));

        row.getChildren().addAll(tenant, request, moveIn, status);
        return row;
    }

    private void clearPropertyDetails() {
        if (propertyTitleLabel != null)
            propertyTitleLabel.setText("Title: -");
        if (propertyLocationLabel != null)
            propertyLocationLabel.setText("Location: -");
        if (propertyTypeLabel != null)
            propertyTypeLabel.setText("Type: -");
        if (propertyPriceLabel != null)
            propertyPriceLabel.setText("Rent: -");
        if (propertySizeLabel != null)
            propertySizeLabel.setText("Size: -");
        if (propertyAvailabilityLabel != null)
            propertyAvailabilityLabel.setText("Availability: -");
        if (propertyContactLabel != null)
            propertyContactLabel.setText("Contact: -");
        if (propertyDetailsLabel != null)
            propertyDetailsLabel.setText("Details: -");
        if (propertyImage1 != null)
            propertyImage1.setImage(null);
        if (propertyImage2 != null)
            propertyImage2.setImage(null);
        if (propertyImage3 != null)
            propertyImage3.setImage(null);
        updatePropertyImageSlotStyle(propertyImage1, false);
        updatePropertyImageSlotStyle(propertyImage2, false);
        updatePropertyImageSlotStyle(propertyImage3, false);
        if (propertyRequestsContainer != null)
            propertyRequestsContainer.getChildren().clear();
        if (propertyReviewsContainer != null)
            propertyReviewsContainer.getChildren().clear();
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private void populateAnalyticsPage() {
        if (analyticsPageRoot == null) {
            return;
        }

        int ownerId = resolveCurrentOwnerId();
        if (ownerId <= 0) {
            setAnalyticsDefaults();
            return;
        }

        int totalProperties = 0;
        double avgRent = 0;
        String propertiesQuery = "SELECT COUNT(*) AS total_properties, COALESCE(AVG(rent), 0) AS avg_rent "
                + "FROM houses WHERE owner_id = ?";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(propertiesQuery)) {
            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalProperties = rs.getInt("total_properties");
                    avgRent = rs.getDouble("avg_rent");
                }
            }
        } catch (SQLException e) {
            setAnalyticsDefaults();
            return;
        }

        int totalRequests = 0;
        int approvedRequests = 0;
        int pendingRequests = 0;
        int deniedRequests = 0;
        double estimatedRevenue = 0;
        LocalDate latestDate = null;
        String latestStatus = "-";
        String latestTenant = "-";
        int latestRequestId = -1;
        for (BookingRequest request : DataStore.getBookingRequests()) {
            totalRequests++;

            String normalized = normalizeStatus(request.getStatus());
            if ("Approved".equalsIgnoreCase(normalized)) {
                approvedRequests++;
                estimatedRevenue += request.getMonthlyRent();
            } else if ("Pending".equalsIgnoreCase(normalized)) {
                pendingRequests++;
            } else {
                deniedRequests++;
            }

            if (request.getId() > latestRequestId) {
                latestRequestId = request.getId();
                latestDate = request.getRequestDate();
                latestStatus = normalized;
                latestTenant = request.getTenantName();
            }
        }

        String topProperty = "-";
        String topPropertyQuery = "SELECT COALESCE(NULLIF(TRIM(h.title), ''), h.location, 'Untitled House') AS property_name, COUNT(*) AS req_count "
                + "FROM rent_requests r "
                + "JOIN houses h ON h.id = r.house_id "
                + "WHERE h.owner_id = ? "
                + "GROUP BY h.id "
                + "ORDER BY req_count DESC, h.id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(topPropertyQuery)) {
            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    topProperty = rs.getString("property_name") + " (" + rs.getInt("req_count") + " requests)";
                }
            }
        } catch (SQLException e) {
            topProperty = "-";
        }

        double approvalRate = totalRequests > 0 ? (approvedRequests * 100.0) / totalRequests : 0;
        double occupancyRate = totalProperties > 0 ? Math.min(100, (approvedRequests * 100.0) / totalProperties) : 0;

        if (analyticsTotalPropertiesLabel != null)
            analyticsTotalPropertiesLabel.setText(String.valueOf(totalProperties));
        if (analyticsTotalRequestsLabel != null)
            analyticsTotalRequestsLabel.setText(String.valueOf(totalRequests));
        if (analyticsApprovedRequestsLabel != null)
            analyticsApprovedRequestsLabel.setText(String.valueOf(approvedRequests));
        if (analyticsPendingRequestsLabel != null)
            analyticsPendingRequestsLabel.setText(String.valueOf(pendingRequests));
        if (analyticsDeniedRequestsLabel != null)
            analyticsDeniedRequestsLabel.setText(String.valueOf(deniedRequests));
        if (analyticsAvgRentLabel != null)
            analyticsAvgRentLabel.setText(formatRent(avgRent));
        if (analyticsEstimatedRevenueLabel != null)
            analyticsEstimatedRevenueLabel.setText(formatRent(estimatedRevenue));
        if (analyticsApprovalRateLabel != null)
            analyticsApprovalRateLabel.setText(String.format("%.0f%%", approvalRate));
        if (analyticsOccupancyRateLabel != null)
            analyticsOccupancyRateLabel.setText(String.format("%.0f%%", occupancyRate));
        if (analyticsTopPropertyLabel != null)
            analyticsTopPropertyLabel.setText(topProperty);
        if (analyticsLatestActivityLabel != null) {
            String dateText = latestDate == null ? "-" : formatDate(latestDate);
            analyticsLatestActivityLabel.setText(latestTenant + " | " + latestStatus + " | " + dateText);
        }

        if (analyticsApprovalBar != null)
            analyticsApprovalBar.setProgress(Math.max(0, Math.min(1, approvalRate / 100.0)));
        if (analyticsOccupancyBar != null)
            analyticsOccupancyBar.setProgress(Math.max(0, Math.min(1, occupancyRate / 100.0)));

        populateAnalyticsTrendChart(ownerId);
    }

    private void setAnalyticsDefaults() {
        if (analyticsTotalPropertiesLabel != null)
            analyticsTotalPropertiesLabel.setText("0");
        if (analyticsTotalRequestsLabel != null)
            analyticsTotalRequestsLabel.setText("0");
        if (analyticsApprovedRequestsLabel != null)
            analyticsApprovedRequestsLabel.setText("0");
        if (analyticsPendingRequestsLabel != null)
            analyticsPendingRequestsLabel.setText("0");
        if (analyticsDeniedRequestsLabel != null)
            analyticsDeniedRequestsLabel.setText("0");
        if (analyticsAvgRentLabel != null)
            analyticsAvgRentLabel.setText("৳0");
        if (analyticsEstimatedRevenueLabel != null)
            analyticsEstimatedRevenueLabel.setText("৳0");
        if (analyticsApprovalRateLabel != null)
            analyticsApprovalRateLabel.setText("0%");
        if (analyticsOccupancyRateLabel != null)
            analyticsOccupancyRateLabel.setText("0%");
        if (analyticsTopPropertyLabel != null)
            analyticsTopPropertyLabel.setText("-");
        if (analyticsLatestActivityLabel != null)
            analyticsLatestActivityLabel.setText("-");
        if (analyticsApprovalBar != null)
            analyticsApprovalBar.setProgress(0);
        if (analyticsOccupancyBar != null)
            analyticsOccupancyBar.setProgress(0);
        if (analyticsRequestsTrendChart != null)
            analyticsRequestsTrendChart.getData().clear();
    }

    private void populateAnalyticsTrendChart(int ownerId) {
        if (analyticsRequestsTrendChart == null) {
            return;
        }

        analyticsRequestsTrendChart.getData().clear();
        analyticsRequestsTrendChart.setLegendVisible(false);
        analyticsRequestsTrendChart.setAnimated(false);
        analyticsRequestsTrendChart.setCreateSymbols(true);

        DateTimeFormatter yearMonthFormatter = DateTimeFormatter.ofPattern("yyyy-MM", Locale.ENGLISH);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MMM yy", Locale.ENGLISH);

        Map<String, Integer> monthCounts = new LinkedHashMap<>();
        Map<String, String> monthLabels = new LinkedHashMap<>();

        LocalDate now = LocalDate.now().withDayOfMonth(1);
        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String key = month.format(yearMonthFormatter);
            monthCounts.put(key, 0);
            monthLabels.put(key, month.format(labelFormatter));
        }

        String query = "SELECT strftime('%Y-%m', r.request_date) AS ym, COUNT(*) AS cnt "
                + "FROM rent_requests r "
                + "JOIN houses h ON h.id = r.house_id "
                + "WHERE h.owner_id = ? AND r.request_date IS NOT NULL "
                + "GROUP BY ym";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String ym = rs.getString("ym");
                    if (monthCounts.containsKey(ym)) {
                        monthCounts.put(ym, rs.getInt("cnt"));
                    }
                }
            }
        } catch (SQLException e) {
            // Keep zero-filled chart when query fails.
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Integer> entry : monthCounts.entrySet()) {
            String label = monthLabels.get(entry.getKey());
            series.getData().add(new XYChart.Data<>(label, entry.getValue()));
        }

        analyticsRequestsTrendChart.getData().add(series);
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
        row.setFillHeight(false);

        Label tenant = buildCellLabel(request.getTenantName());
        Label property = buildCellLabel(request.getProperty());
        Label requestDate = buildCellLabel(formatDate(request.getRequestDate()));
        Label moveInDate = buildCellLabel(formatDate(request.getMoveInDate()));
        Label rent = buildCellLabel(formatRent(request.getMonthlyRent()));

        Label status = new Label(normalizeStatus(request.getStatus()));
        status.getStyleClass().add("status-pill");
        status.getStyleClass().add(normalizeStatus(request.getStatus()).toLowerCase(Locale.ENGLISH));
        applyRequestStatusColumnSizing(status);

        Button approve = new Button("Approve");
        approve.getStyleClass().add("btn-approve");
        applyActionButtonSizing(approve);
        Button delete = new Button("Delete");
        delete.getStyleClass().add("btn-delete");
        applyActionButtonSizing(delete);

        String currentStatus = normalizeStatus(request.getStatus());
        boolean alreadyFinal = "Approved".equalsIgnoreCase(currentStatus) || "Denied".equalsIgnoreCase(currentStatus);
        applyDecisionSelectionState(approve, currentStatus);
        if (alreadyFinal) {
            approve.setDisable(true);
        }

        approve.setOnAction(event -> {
            if (updateRequestStatus(request.getId(), "Approved")) {
                showStatusPopup("Success", "Request approved for\n" + request.getTenantName() + ".", "OK");
                populateKpis();
                populateRequests();
            } else {
                showStatusPopup("Error", "Could not approve this request.\nPlease try again.", "OK");
            }
        });

        delete.setOnAction(event -> {
            int houseId = getHouseIdFromRequest(request.getId());
            if (deleteRequest(request.getId())) {
                // Create notification for tenant only if it was approved
                if ("Approved".equalsIgnoreCase(currentStatus)) {
                    String notifyMessage = "Your rental agreement for " + request.getProperty() + " has been removed.";
                    DataStore.createNotification(request.getTenantId(), houseId, request.getTenantId(), "Rental Removed", notifyMessage, "removal");
                }
                
                showStatusPopup("Success", "Request deleted.", "OK");
                populateKpis();
                populateRequests();
            } else {
                showStatusPopup("Error", "Could not delete this request.\nPlease try again.", "OK");
            }
        });

        VBox actions = new VBox(8, approve, delete);
        applyRequestActionsColumnSizing(actions);

        row.getChildren().addAll(tenant, property, requestDate, moveInDate, rent, status, actions);
        return row;
    }

    private HBox buildAcceptedTenantRow(BookingRequest request) {
        HBox row = new HBox(16);
        row.getStyleClass().add("table-row");
        row.setFillHeight(false);

        Label tenant = buildCellLabel(request.getTenantName());
        Label property = buildCellLabel(request.getProperty());
        Label requestDate = buildCellLabel(formatDate(request.getRequestDate()));
        Label moveInDate = buildCellLabel(formatDate(request.getMoveInDate()));
        Label rent = buildCellLabel(formatRent(request.getMonthlyRent()));

        Label status = new Label("Approved");
        status.getStyleClass().add("status-pill");
        status.getStyleClass().add("approved");
        applyRequestStatusColumnSizing(status);

        Button delete = new Button("Delete");
        delete.getStyleClass().add("btn-delete");
        applyActionButtonSizing(delete);

        delete.setOnAction(event -> {
            if (deleteRequest(request.getId())) {
                showStatusPopup("Success", "Rental agreement removed.", "OK");
                populateKpis();
                populateRequests();
            } else {
                showStatusPopup("Error", "Could not delete this rental.\nPlease try again.", "OK");
            }
        });

        HBox actions = new HBox(8, delete);
        applyRequestActionsColumnSizing(actions);

        row.getChildren().addAll(tenant, property, requestDate, moveInDate, rent, status, actions);
        return row;
    }

    private boolean updateRequestStatus(int requestId, String newStatus) {
        if (requestId <= 0 || newStatus == null || newStatus.isBlank()) {
            return false;
        }

        String query = "UPDATE rent_requests SET status = ?, accepted_at = CASE "
                + "WHEN lower(?) = 'approved' THEN ? ELSE accepted_at END "
                + "WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newStatus);
            pstmt.setString(2, newStatus);
            pstmt.setString(3, LocalDate.now().toString());
            pstmt.setInt(4, requestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean deleteRequest(int requestId) {
        if (requestId <= 0) {
            return false;
        }

        String query = "DELETE FROM rent_requests WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, requestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private int getHouseIdFromRequest(int requestId) {
        if (requestId <= 0) {
            return -1;
        }

        String query = "SELECT house_id FROM rent_requests WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, requestId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("house_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private Label buildCellLabel(String text) {
        Label label = new Label(text == null ? "" : text);
        label.getStyleClass().add("table-text");
        return label;
    }

    private void applyActionButtonSizing(Button button) {
        button.setMinWidth(92);
        button.setPrefWidth(92);
        button.setTextOverrun(OverrunStyle.CLIP);
    }

    private void applyDecisionSelectionState(Button approve, String status) {
        if (approve == null) {
            return;
        }

        approve.getStyleClass().remove("decision-selected");

        String normalized = normalizeStatus(status);
        if ("Approved".equalsIgnoreCase(normalized)) {
            approve.getStyleClass().add("decision-selected");
        }
    }

    private void applyStatusPillSizing(Label statusLabel) {
        statusLabel.setMinWidth(Region.USE_PREF_SIZE);
        statusLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        statusLabel.setMaxWidth(Region.USE_PREF_SIZE);
        statusLabel.setWrapText(false);
    }

    private void applyRequestStatusColumnSizing(Label statusLabel) {
        if (statusLabel == null) {
            return;
        }
        statusLabel.setMinWidth(120);
        statusLabel.setPrefWidth(120);
        statusLabel.setMaxWidth(120);
        statusLabel.setWrapText(false);
    }

    private void applyRequestActionsColumnSizing(Region actionsContainer) {
        if (actionsContainer == null) {
            return;
        }
        actionsContainer.setMinWidth(192);
        actionsContainer.setPrefWidth(192);
        actionsContainer.setMaxWidth(192);
    }

    private Label buildWideMessageLabel(String text) {
        Label label = buildCellLabel(text);
        label.setWrapText(true);
        label.setPrefWidth(Region.USE_COMPUTED_SIZE);
        label.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(label, Priority.ALWAYS);
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
            DataStore.clearRememberedSession();
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
            showNavigationError("Failed to open List House page: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenTenants(ActionEvent event) {
        onOpenAcceptedTenants(event);
    }

    @FXML
    private void onOpenAcceptedTenants(ActionEvent event) {
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

        try {
            loadScene(stage, "owner-accepted-tenants.fxml");
        } catch (IOException e) {
            showNavigationError("Failed to open Tenants page: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenRequests(ActionEvent event) {
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

        try {
            loadScene(stage, "owner-tenants.fxml");
        } catch (IOException e) {
            showNavigationError("Failed to open Requests page: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenProperties(ActionEvent event) {
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

        try {
            loadScene(stage, "owner-properties.fxml");
        } catch (IOException e) {
            showNavigationError("Failed to open Properties page: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenHouseProfile(ActionEvent event) {
        if (selectedHouse == null || selectedHouse.id <= 0) {
            showNavigationError("Select a property card first to manage it.");
            return;
        }

        DataStore.selectedHouseId = selectedHouse.id;

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

        try {
            loadScene(stage, "owner-house-profile.fxml");
        } catch (IOException e) {
            showNavigationError("Failed to open House Profile page: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenAnalytics(ActionEvent event) {
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

        try {
            loadScene(stage, "owner-analytics.fxml");
        } catch (IOException e) {
            showNavigationError("Failed to open Analytics page: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenDashboard(ActionEvent event) {
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

        try {
            loadScene(stage, "owner-view.fxml");
        } catch (IOException e) {
            showNavigationError("Failed to open Dashboard page: " + e.getMessage());
        }
    }

    private void showNavigationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Navigation Error");
        alert.setHeaderText("Could not open List New House page");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showStatusPopup(String title, String message, String okButtonText) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(DataStore.resolveFxml("status-popup.fxml")));
            Parent root = loader.load();

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);
            popupStage.initModality(Modality.APPLICATION_MODAL);

            Rectangle clip = new Rectangle();
            clip.setArcWidth(40);
            clip.setArcHeight(40);
            clip.widthProperty().bind(root.layoutBoundsProperty().map(bounds -> bounds.getWidth()));
            clip.heightProperty().bind(root.layoutBoundsProperty().map(bounds -> bounds.getHeight()));
            root.setClip(clip);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            popupStage.setScene(scene);

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

            // Set popup content
            Label popupTitleLabel = (Label) root.lookup("#popupTitleLabel");
            Label popupMessageLabel = (Label) root.lookup("#popupMessageLabel");
            Label popupIconLabel = (Label) root.lookup("#popupIconLabel");
            Button popupOkButton = (Button) root.lookup("#popupOkButton");
            Button popupCancelButton = (Button) root.lookup("#popupCancelButton");

            if (popupTitleLabel != null) {
                popupTitleLabel.setText(title);
            }
            if (popupMessageLabel != null) {
                popupMessageLabel.setText(message);
            }
            if (popupOkButton != null) {
                popupOkButton.setText(okButtonText);
                popupOkButton.setOnAction(e -> popupStage.close());
            }
            if (popupCancelButton != null) {
                popupCancelButton.setVisible(false);
            }
            if (popupIconLabel != null) {
                if ("Success".equals(title)) {
                    popupIconLabel.setText("✓");
                    popupIconLabel.setStyle("-fx-background-color: #d4f4dd; -fx-text-fill: #0f9548; -fx-font-size: 22; -fx-font-weight: 800; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40; -fx-background-radius: 999;");
                } else if ("Error".equals(title)) {
                    popupIconLabel.setText("✕");
                    popupIconLabel.setStyle("-fx-background-color: #ffd4d0; -fx-text-fill: #d32f2f; -fx-font-size: 22; -fx-font-weight: 800; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40; -fx-background-radius: 999;");
                } else {
                    popupIconLabel.setText("!");
                    popupIconLabel.setStyle("-fx-background-color: #fff4d4; -fx-text-fill: #f9a825; -fx-font-size: 22; -fx-font-weight: 800; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40; -fx-background-radius: 999;");
                }
            }

            popupStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean showLogoutConfirmation() {
        try {
            AtomicBoolean confirmed = new AtomicBoolean(false);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(DataStore.resolveFxml("logout-popup.fxml")));
            Parent root = loader.load();

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);
            popupStage.initModality(Modality.APPLICATION_MODAL);

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.setArcWidth(40);
            clip.setArcHeight(40);
            clip.widthProperty().bind(root.layoutBoundsProperty().map(bounds -> bounds.getWidth()));
            clip.heightProperty().bind(root.layoutBoundsProperty().map(bounds -> bounds.getHeight()));
            root.setClip(clip);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            popupStage.setScene(scene);

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
            if (isRequestsPage()) {
                loadScene(stage, "owner-tenants.fxml");
            } else if (isAcceptedTenantsPage()) {
                loadScene(stage, "owner-accepted-tenants.fxml");
            } else if (isPropertiesPage()) {
                loadScene(stage, "owner-properties.fxml");
            } else if (isAnalyticsPage()) {
                loadScene(stage, "owner-analytics.fxml");
            } else {
                loadScene(stage, "owner-view.fxml");
            }
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