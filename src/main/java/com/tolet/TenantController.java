package com.tolet;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.concurrent.Task;

import database.DatabaseConnection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TenantController {
    private static final double PROPERTY_CARD_WIDTH = 350;
    private String lastShownDeniedNotificationKey;

    @FXML
    private Label welcomeLabel, totalCountLabel, listedHousesLabel, savedHomesLabel,
            bookedRequestsLabel;
    @FXML
    private TextField searchField;
    @FXML
    private FlowPane filterContainer;
    @FXML
    private FlowPane propertiesGrid;
    @FXML
    private VBox currentRentedHouseContainer;
    @FXML
    private FlowPane currentRentedHouseGrid;
    @FXML
    private TextArea reviewTextArea;
    @FXML
    private ComboBox<ReviewHouseOption> reviewHouseSelector;
    @FXML
    private FlowPane reviewsContainer;
    @FXML
    private ToggleButton themeToggle;
    @FXML
    private HBox bookingStatusCard;
    @FXML
    private Label bookingStatusTitleLabel;
    @FXML
    private Label bookingStatusMessageLabel;
    @FXML
    private Label profileNameLabel;
    @FXML
    private Label profileVerificationLabel;
    @FXML
    private Label profileRoleLabel;
    @FXML
    private Label profileEmailLabel;
    @FXML
    private Label profilePhoneLabel;
    @FXML
    private BorderPane pageRoot;
    @FXML
    private Button navDashboardButton;
    @FXML
    private Button navSearchButton;
    @FXML
    private Button navWishlistButton;
    @FXML
    private Button navReviewButton;
    @FXML
    private ImageView detailsImageView;
    @FXML
    private Label detailsImageCounterLabel;
    @FXML
    private Label detailsTitleLabel;
    @FXML
    private Label detailsLocationLabel;
    @FXML
    private Label detailsRentLabel;
    @FXML
    private Label detailsOwnerLabel;
    @FXML
    private Label detailsAvailabilityLabel;
    @FXML
    private Label detailsContactLabel;
    @FXML
    private Label detailsTypeLabel;
    @FXML
    private Label detailsBedroomsLabel;
    @FXML
    private Label detailsBathroomsLabel;
    @FXML
    private Label detailsAreaLabel;
    @FXML
    private Label detailsShortDetailLabel;
    @FXML
    private Label detailsDescriptionLabel;
    @FXML
    private FlowPane detailsTagsContainer;
    @FXML
    private Label detailsAmenitiesLabel;
    @FXML
    private VBox tenantNotificationsContainer;

    private List<House> allHouses;
    private List<String> activeFilters = new ArrayList<>();
    private final String[] FILTERS = { "Family", "Bachelor", "Gas Available", "Parking", "Furnished" };
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();
    private Timeline refreshTimeline;
    private final List<Image> detailCarouselImages = new ArrayList<>();
    private int detailCarouselIndex = 0;
    private Timeline detailCarouselTimeline;
    private final Set<Integer> wishlistedHouseIds = new HashSet<>();

    private static class ReviewHouseOption {
        private final int houseId;
        private final String label;

        private ReviewHouseOption(int houseId, String label) {
            this.houseId = houseId;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @FXML
    public void initialize() {
        allHouses = new ArrayList<>();

        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }

        if (DataStore.currentUser != null && welcomeLabel != null) {
            welcomeLabel.setText("Welcome Back, " + DataStore.currentUser.getUsername() + "!");
        }

        populateProfile();

        if (detailsTitleLabel != null) {
            initializeHouseDetailsView();
        }

        // Initialize Filters
        if (filterContainer != null) {
            for (String filter : FILTERS) {
                ToggleButton chip = new ToggleButton(filter);
                chip.getStyleClass().add("filter-chip");
                chip.setOnAction(e -> toggleFilter(filter, chip.isSelected()));
                filterContainer.getChildren().add(chip);
            }
        }

        // Load data off the UI thread to keep scene switch responsive.
        if (propertiesGrid != null) {
            loadHousesAsync();
            refreshWishlistIdsAsync();
            refreshListedHousesCountAsync();
            refreshTenantBookingKpisAsync();
            refreshPendingRequestStatusAsync();
            refreshTenantNotificationsAsync();
            loadCurrentRentedHouseAsync();
            startAutoRefresh();
        }

        // Search listener with debounce to avoid rerendering on every keystroke.
        if (searchField != null) {
            searchDebounce.setOnFinished(e -> applyFilters());
            searchField.textProperty().addListener((obs, old, val) -> searchDebounce.playFromStart());
        }

        if (reviewHouseSelector != null) {
            loadReviewHouseOptions();
            loadRecentReviews();
        }

        applyActiveNav(getCurrentBaseFxml());
    }

    private void populateProfile() {
        TenantProfileMeta profileMeta = resolveTenantProfileMeta();
        if (profileNameLabel != null) {
            profileNameLabel.setText(profileMeta.name);
        }
        if (profileVerificationLabel != null) {
            profileVerificationLabel.setText("Status - " + (profileMeta.verified ? "Verified" : "Unverified"));
        }
        if (profileRoleLabel != null) {
            profileRoleLabel.setText("Role - " + sanitizeRole(profileMeta.role));
        }

        String phone = profileMeta.phone == null ? "" : profileMeta.phone.trim();
        String email = profileMeta.email == null ? "" : profileMeta.email.trim();
        boolean hasPhone = !phone.isBlank() && !"-".equals(phone);
        boolean hasEmail = !email.isBlank() && !"-".equals(email);

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
    }

    private TenantProfileMeta resolveTenantProfileMeta() {
        String fallbackName = "Tenant";
        String fallbackRole = "Tenant";
        String fallbackEmail = "-";
        String fallbackPhone = "-";

        if (DataStore.currentUser == null) {
            return new TenantProfileMeta(fallbackName, fallbackRole, fallbackEmail, fallbackPhone, false);
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

                    if (dbName != null && !dbName.isBlank()) {
                        name = dbName;
                    }
                    if (dbRole != null && !dbRole.isBlank()) {
                        role = dbRole;
                    }
                    if (dbEmail != null && !dbEmail.isBlank()) {
                        email = dbEmail;
                    }
                    if (dbPhone != null && !dbPhone.isBlank()) {
                        phone = dbPhone;
                    }

                    verified = rs.getInt("verified") == 1;
                }
            }
        } catch (SQLException e) {
            verified = false;
        }

        return new TenantProfileMeta(name, role, email, phone, verified);
    }

    private String sanitizeRole(String role) {
        if (role == null) {
            return "-";
        }
        String cleaned = role.replaceAll("(?i)\\s*\\((verified|unverified)\\)\\s*", "").trim();
        return cleaned.isBlank() ? "-" : cleaned;
    }

    private static class TenantProfileMeta {
        private final String name;
        private final String role;
        private final String email;
        private final String phone;
        private final boolean verified;

        private TenantProfileMeta(String name, String role, String email, String phone, boolean verified) {
            this.name = name;
            this.role = role;
            this.email = email;
            this.phone = phone;
            this.verified = verified;
        }
    }

    private void loadReviewHouseOptions() {
        reviewHouseSelector.getItems().clear();

        int tenantId = resolveCurrentTenantId();
        if (tenantId <= 0) {
            return;
        }

        String sql = "SELECT DISTINCT h.id, "
                + "COALESCE(NULLIF(TRIM(h.title), ''), h.location, 'Untitled House') AS title, "
                + "COALESCE(h.location, '-') AS location, "
                + "COALESCE(h.rent, 0) AS rent "
                + "FROM rent_requests r "
                + "JOIN houses h ON h.id = r.house_id "
                + "WHERE r.tenant_id = ? "
                + "AND lower(trim(COALESCE(r.status, ''))) = 'approved' "
                + "AND date(COALESCE(NULLIF(TRIM(r.accepted_at), ''), r.request_date)) <= date('now', '-1 month') "
                + "ORDER BY h.id DESC";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tenantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int houseId = rs.getInt("id");
                    String label = rs.getString("title") + " - " + rs.getString("location")
                            + " (BDT " + (int) rs.getDouble("rent") + ")";
                    reviewHouseSelector.getItems().add(new ReviewHouseOption(houseId, label));
                }
            }
        } catch (SQLException e) {
            // Keep dropdown empty if query fails.
        }

        if (!reviewHouseSelector.getItems().isEmpty()) {
            reviewHouseSelector.getSelectionModel().selectFirst();
        }
    }

    private int resolveCurrentTenantId() {
        if (DataStore.currentUser == null) {
            return -1;
        }
        if (DataStore.currentUser.getId() > 0) {
            return DataStore.currentUser.getId();
        }

        String sql = "SELECT id FROM users WHERE lower(email) = lower(?) OR name = ? COLLATE NOCASE LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    private void loadHousesAsync() {
        Task<List<House>> task = new Task<>() {
            @Override
            protected List<House> call() {
                return new ArrayList<>(DataStore.getHouses());
            }
        };

        task.setOnSucceeded(e -> {
            allHouses = task.getValue() != null ? task.getValue() : new ArrayList<>();
            updateListedHousesKpi(allHouses.size());
            renderCurrentViewProperties();
        });

        task.setOnFailed(e -> {
            allHouses = new ArrayList<>();
            updateListedHousesKpi(0);
            renderCurrentViewProperties();
        });

        Thread loaderThread = new Thread(task, "tenant-houses-loader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private void loadCurrentRentedHouseAsync() {
        Task<House> task = new Task<>() {
            @Override
            protected House call() {
                Integer tenantId = DataStore.currentUser != null ? getCurrentUserId() : null;
                if (tenantId == null) {
                    return null;
                }

                String query = "SELECT h.id, h.title, h.location, h.type, h.rent, h.image, h.bedrooms, h.bathrooms, h.area, u.name "
                        + "FROM houses h "
                        + "JOIN users u ON h.owner_id = u.id "
                        + "JOIN rent_requests r ON r.house_id = h.id "
                        + "WHERE r.tenant_id = ? AND lower(r.status) = 'approved' "
                        + "ORDER BY r.id DESC LIMIT 1";

                try (Connection conn = DatabaseConnection.connect();
                        PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setInt(1, tenantId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        return new House(
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getString("location"),
                            rs.getString("type"),
                            rs.getDouble("rent"),
                            rs.getString("name"),
                            rs.getString("image"),
                            rs.getInt("bedrooms"),
                            rs.getInt("bathrooms"),
                            rs.getDouble("area"));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            House currentHouse = task.getValue();
            if (currentHouse != null && currentRentedHouseContainer != null) {
                currentRentedHouseGrid.getChildren().clear();
                currentRentedHouseGrid.getChildren().add(createPropertyCard(currentHouse));
                currentRentedHouseContainer.setVisible(true);
                currentRentedHouseContainer.setManaged(true);
            } else if (currentRentedHouseContainer != null) {
                currentRentedHouseContainer.setVisible(false);
                currentRentedHouseContainer.setManaged(false);
            }
        });

        task.setOnFailed(e -> {
            if (currentRentedHouseContainer != null) {
                currentRentedHouseContainer.setVisible(false);
                currentRentedHouseContainer.setManaged(false);
            }
        });

        Thread loaderThread = new Thread(task, "tenant-current-house-loader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private Integer getCurrentUserId() {
        if (DataStore.currentUser == null) {
            return null;
        }
        String query = "SELECT id FROM users WHERE email = ? OR name = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, DataStore.currentUser.getEmail());
            pstmt.setString(2, DataStore.currentUser.getUsername());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void startAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(20), e -> loadHousesAsync()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.getKeyFrames().setAll(new KeyFrame(Duration.seconds(20), e -> {
            loadHousesAsync();
            refreshWishlistIdsAsync();
            refreshListedHousesCountAsync();
            refreshTenantBookingKpisAsync();
            refreshPendingRequestStatusAsync();
            refreshTenantNotificationsAsync();
            loadCurrentRentedHouseAsync();
        }));
        refreshTimeline.play();

        propertiesGrid.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && refreshTimeline != null) {
                refreshTimeline.stop();
            }
        });
    }

    private void toggleFilter(String filter, boolean isSelected) {
        if (isSelected)
            activeFilters.add(filter);
        else
            activeFilters.remove(filter);
        applyFilters();
    }

    private void applyFilters() {
        if (allHouses == null) {
            return;
        }

        String query = searchField.getText();
        String search = query == null ? "" : query.toLowerCase();

        List<House> filtered = allHouses.stream()
                .filter(h -> {
                    // Search Match
                    boolean matchSearch = h.getLocation().toLowerCase().contains(search) ||
                            h.getType().toLowerCase().contains(search);

                    // Filter Match (Simple logic: if "Family" is active, show only Family)
                    boolean matchFilter = activeFilters.isEmpty() || activeFilters.contains(h.getType());

                    return matchSearch && matchFilter;
                })
                .collect(Collectors.toList());

        renderProperties(filtered);
    }

    private void refreshWishlistIdsAsync() {
        Task<Set<Integer>> task = new Task<>() {
            @Override
            protected Set<Integer> call() {
                Set<Integer> ids = new HashSet<>();
                int tenantId = resolveCurrentTenantId();
                if (tenantId <= 0) {
                    return ids;
                }

                String sql = "SELECT house_id FROM tenant_wishlist WHERE tenant_id = ?";
                try (Connection conn = DatabaseConnection.connect();
                        PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, tenantId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            ids.add(rs.getInt("house_id"));
                        }
                    }
                } catch (SQLException e) {
                    return ids;
                }
                return ids;
            }
        };

        task.setOnSucceeded(e -> {
            wishlistedHouseIds.clear();
            Set<Integer> loaded = task.getValue();
            if (loaded != null) {
                wishlistedHouseIds.addAll(loaded);
            }
            renderCurrentViewProperties();
        });
        task.setOnFailed(e -> {
            wishlistedHouseIds.clear();
            renderCurrentViewProperties();
        });

        Thread wishlistLoader = new Thread(task, "tenant-wishlist-loader");
        wishlistLoader.setDaemon(true);
        wishlistLoader.start();
    }

    private void renderCurrentViewProperties() {
        if (allHouses == null || propertiesGrid == null) {
            return;
        }

        String baseFxml = getCurrentBaseFxml();
        String normalized = baseFxml == null ? "" : baseFxml.toLowerCase();
        if (normalized.contains("tenant-wishlist")) {
            List<House> wishlistHouses = allHouses.stream()
                    .filter(h -> wishlistedHouseIds.contains(h.getId()))
                    .collect(Collectors.toList());
            renderProperties(wishlistHouses);
            return;
        }

        if (searchField != null) {
            applyFilters();
            return;
        }

        renderProperties(allHouses);
    }

    private void updateListedHousesKpi(int listedCount) {
        if (listedHousesLabel == null) {
            return;
        }
        listedHousesLabel.setText(String.valueOf(Math.max(0, listedCount)));
    }

    private void updateSavedHomesKpi(int count) {
        if (savedHomesLabel == null) {
            return;
        }
        savedHomesLabel.setText(String.valueOf(Math.max(0, count)));
    }

    private void updateBookedRequestsKpi(int count) {
        if (bookedRequestsLabel == null) {
            return;
        }
        bookedRequestsLabel.setText(String.valueOf(Math.max(0, count)));
    }

    private void refreshTenantBookingKpisAsync() {
        if (savedHomesLabel == null && bookedRequestsLabel == null) {
            return;
        }

        Task<int[]> task = new Task<>() {
            @Override
            protected int[] call() {
                int tenantId = resolveCurrentTenantId();
                if (tenantId <= 0) {
                    return new int[] { 0, 0 };
                }

                String sql = "SELECT "
                        + "(SELECT COUNT(*) FROM tenant_wishlist w WHERE w.tenant_id = ?) AS saved_homes, "
                        + "COUNT(*) AS booked_requests "
                        + "FROM rent_requests WHERE tenant_id = ?";

                try (Connection conn = DatabaseConnection.connect();
                        PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, tenantId);
                    pstmt.setInt(2, tenantId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            return new int[] { rs.getInt("saved_homes"), rs.getInt("booked_requests") };
                        }
                    }
                } catch (SQLException e) {
                    return new int[] { 0, 0 };
                }

                return new int[] { 0, 0 };
            }
        };

        task.setOnSucceeded(e -> {
            int[] result = task.getValue();
            if (result == null || result.length < 2) {
                updateSavedHomesKpi(0);
                updateBookedRequestsKpi(0);
                return;
            }
            updateSavedHomesKpi(result[0]);
            updateBookedRequestsKpi(result[1]);
        });
        task.setOnFailed(e -> {
            updateSavedHomesKpi(0);
            updateBookedRequestsKpi(0);
        });

        Thread bookingKpiLoader = new Thread(task, "tenant-booking-kpi-loader");
        bookingKpiLoader.setDaemon(true);
        bookingKpiLoader.start();
    }

    private void refreshListedHousesCountAsync() {
        if (listedHousesLabel == null) {
            return;
        }

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                String sql = "SELECT COUNT(*) AS total FROM houses "
                        + "WHERE lower(trim(COALESCE(approval_status, 'pending'))) = 'approved'";
                try (Connection conn = DatabaseConnection.connect();
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                } catch (SQLException e) {
                    // Fallback to already loaded list if DB count query fails.
                    if (allHouses != null) {
                        return allHouses.size();
                    }
                }
                return 0;
            }
        };

        task.setOnSucceeded(e -> updateListedHousesKpi(task.getValue() == null ? 0 : task.getValue()));
        task.setOnFailed(e -> updateListedHousesKpi(allHouses == null ? 0 : allHouses.size()));

        Thread countLoader = new Thread(task, "tenant-listed-count-loader");
        countLoader.setDaemon(true);
        countLoader.start();
    }

    private void renderProperties(List<House> houses) {
        if (propertiesGrid == null) {
            return;
        }
        propertiesGrid.getChildren().clear();

        if (totalCountLabel != null) {
            totalCountLabel.setText(houses.size() + " properties");
        }

        int maxToRender = isDashboardView() ? 2 : houses.size();
        int renderCount = Math.min(maxToRender, houses.size());
        if (renderCount == 0) {
            Label emptyStateLabel = new Label("No properties available");
            emptyStateLabel.getStyleClass().add("table-title");
            emptyStateLabel.setStyle("-fx-text-fill: #9ca3af; -fx-padding: 12 8 12 8;");
            propertiesGrid.getChildren().add(emptyStateLabel);
            return;
        }

        for (int i = 0; i < renderCount; i++) {
            House h = houses.get(i);
            propertiesGrid.getChildren().add(createPropertyCard(h));
        }
    }

    private boolean isDashboardView() {
        String baseFxml = getCurrentBaseFxml();
        return baseFxml != null && baseFxml.toLowerCase().contains("tenant-view");
    }

    private Node createPropertyCard(House h) {
        VBox card = new VBox();
        card.getStyleClass().add("property-card");
        card.setPrefWidth(PROPERTY_CARD_WIDTH);
        card.setMinWidth(PROPERTY_CARD_WIDTH);

        // Image
        ImageView imgView = new ImageView();
        try {
            imgView.setImage(loadHouseImage(h.getImage()));
        } catch (Exception e) {
            /* fallback image if URL fails */ }
        imgView.setFitWidth(PROPERTY_CARD_WIDTH);
        imgView.setFitHeight(200);

        // Clip image to rounded top corners
        Rectangle clip = new Rectangle(PROPERTY_CARD_WIDTH, 200);
        clip.setArcWidth(12);
        clip.setArcHeight(12);
        imgView.setClip(clip);

        Button wishlistBtn = new Button();
        updateWishlistButtonState(wishlistBtn, wishlistedHouseIds.contains(h.getId()));
        wishlistBtn.setOnAction(e -> toggleWishlist(h, wishlistBtn));

        StackPane imageContainer = new StackPane(imgView);
        imageContainer.setPrefWidth(PROPERTY_CARD_WIDTH);
        imageContainer.setMinWidth(PROPERTY_CARD_WIDTH);
        imageContainer.getChildren().add(wishlistBtn);
        StackPane.setAlignment(wishlistBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(wishlistBtn, new Insets(10, 10, 0, 0));

        // Content
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        // Type Badge
        Label typeBadge = new Label(h.getType());
        typeBadge.getStyleClass().add("card-badge");

        HBox header = new HBox(typeBadge);

        Label title = new Label(nonBlankOrFallback(h.getTitle(), h.getBedrooms() + "BR Apartment"));
        title.getStyleClass().add("card-title");

        Label location = new Label("📍 " + h.getLocation());
        location.getStyleClass().add("card-location");

        Label price = new Label("BDT " + (int) h.getRent() + "/mo");
        price.getStyleClass().add("card-price");

        // Stats Row
        HBox stats = new HBox(15);
        stats.getChildren().addAll(
                new Label(h.getBedrooms() + " Beds"),
                new Label(h.getBathrooms() + " Baths"),
                new Label((int) h.getArea() + " sqft"));
        stats.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");

        // Book Button
        Button bookBtn = new Button("View Details");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        bookBtn.setOnAction(e -> onBookRequest(h));

        Button rentBtn = new Button("Rent House");
        rentBtn.setMaxWidth(Double.MAX_VALUE);
        rentBtn.setStyle(
            "-fx-background-color: #0f766e; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        rentBtn.setOnAction(e -> onRentHouse(h));

        content.getChildren().addAll(header, title, location, stats, new Separator(), price, bookBtn, rentBtn);
        card.getChildren().addAll(imageContainer, content);

        return card;
    }

    private void updateWishlistButtonState(Button button, boolean wishlisted) {
        if (button == null) {
            return;
        }

        if (wishlisted) {
            button.setText("♥");
            button.setStyle(
                    "-fx-background-color: rgba(190, 24, 93, 0.9); -fx-text-fill: #ffffff; -fx-background-radius: 999; -fx-border-radius: 999; -fx-font-size: 14px; -fx-cursor: hand;");
        } else {
            button.setText("♡");
            button.setStyle(
                    "-fx-background-color: rgba(15, 23, 42, 0.72); -fx-text-fill: #f8fafc; -fx-background-radius: 999; -fx-border-radius: 999; -fx-font-size: 14px; -fx-cursor: hand;");
        }
    }

    private void toggleWishlist(House house, Button button) {
        if (house == null || house.getId() <= 0) {
            new Alert(Alert.AlertType.WARNING, "Could not update wishlist for this house.").show();
            return;
        }

        int tenantId = resolveCurrentTenantId();
        if (tenantId <= 0) {
            new Alert(Alert.AlertType.WARNING, "Please sign in again to manage wishlist.").show();
            return;
        }

        boolean currentlyWishlisted = wishlistedHouseIds.contains(house.getId());
        String sql = currentlyWishlisted
                ? "DELETE FROM tenant_wishlist WHERE tenant_id = ? AND house_id = ?"
                : "INSERT OR IGNORE INTO tenant_wishlist (tenant_id, house_id, created_at) VALUES (?, ?, ?)";

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                try (Connection conn = DatabaseConnection.connect();
                        PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, tenantId);
                    pstmt.setInt(2, house.getId());
                    if (!currentlyWishlisted) {
                        pstmt.setString(3, LocalDate.now().toString());
                    }
                    return pstmt.executeUpdate() >= 0;
                } catch (SQLException e) {
                    return false;
                }
            }
        };

        task.setOnSucceeded(e -> {
            if (!Boolean.TRUE.equals(task.getValue())) {
                new Alert(Alert.AlertType.ERROR, "Could not update wishlist. Please try again.").show();
                return;
            }

            if (currentlyWishlisted) {
                wishlistedHouseIds.remove(house.getId());
            } else {
                wishlistedHouseIds.add(house.getId());
            }
            updateWishlistButtonState(button, !currentlyWishlisted);
            animateWishlistButton(button);
            refreshTenantBookingKpisAsync();
            renderCurrentViewProperties();
        });

        task.setOnFailed(e -> new Alert(Alert.AlertType.ERROR, "Could not update wishlist. Please try again.").show());

        Thread toggleThread = new Thread(task, "tenant-wishlist-toggle");
        toggleThread.setDaemon(true);
        toggleThread.start();
    }

    private void animateWishlistButton(Button button) {
        if (button == null) {
            return;
        }
        ScaleTransition pop = new ScaleTransition(Duration.millis(3050), button);
        pop.setFromX(1.0);
        pop.setFromY(1.0);
        pop.setToX(1.5);
        pop.setToY(1.5);
        pop.setCycleCount(2);
        pop.setAutoReverse(true);
        pop.play();
    }

    private Image loadHouseImage(String imageSource) {
        String source = imageSource;
        if (source == null || source.isBlank()) {
            source = getClass().getResource("images/house1.png").toExternalForm();
        }

        Image cached = IMAGE_CACHE.get(source);
        if (cached != null) {
            return cached;
        }

        Image image;
        if (source.startsWith("db-image://house/")) {
            image = loadDbHouseImage(source);
        } else {
            image = new Image(source, 300, 200, true, true, true);
        }

        if (image == null || image.isError()) {
            image = new Image(getClass().getResource("images/house1.png").toExternalForm(), 300, 200, true, true, true);
        }

        IMAGE_CACHE.put(source, image);
        return image;
    }

    private Image loadDbHouseImage(String source) {
        String prefix = "db-image://house/";
        String raw = source.substring(prefix.length());
        String[] parts = raw.split("/");
        if (parts.length == 0) {
            return null;
        }

        int houseId;
        int sortOrder = 1;
        try {
            houseId = Integer.parseInt(parts[0]);
            if (parts.length > 1) {
                sortOrder = Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT image_data FROM house_images WHERE house_id = ? AND sort_order = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, houseId);
            pstmt.setInt(2, sortOrder);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    byte[] bytes = rs.getBytes("image_data");
                    if (bytes != null && bytes.length > 0) {
                        return new Image(new ByteArrayInputStream(bytes), 300, 200, true, true);
                    }
                }
            }
        } catch (SQLException e) {
            return null;
        }

        return null;
    }

    private void refreshPendingRequestStatusAsync() {
        if (bookingStatusCard == null) {
            return;
        }

        Task<RequestStatusInfo> task = new Task<>() {
            @Override
            protected RequestStatusInfo call() {
                if (DataStore.currentUser == null) {
                    return RequestStatusInfo.none();
                }

                // Check for approved requests first
                String approvedSql = "SELECT h.location "
                        + "FROM rent_requests r "
                        + "JOIN users u ON r.tenant_id = u.id "
                        + "JOIN houses h ON r.house_id = h.id "
                        + "WHERE (lower(COALESCE(u.email, '')) = lower(?) OR lower(u.name) = lower(?)) "
                        + "AND lower(trim(COALESCE(r.status, ''))) = 'approved' "
                        + "ORDER BY COALESCE(r.request_date, '') DESC, r.id DESC "
                        + "LIMIT 1";

                try (Connection conn = DatabaseConnection.connect();
                        PreparedStatement pstmt = conn.prepareStatement(approvedSql)) {
                    pstmt.setString(1, DataStore.currentUser.getEmail());
                    pstmt.setString(2, DataStore.currentUser.getUsername());

                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            String location = rs.getString("location");
                            return RequestStatusInfo.approved(location);
                        }
                    }
                } catch (SQLException e) {
                    // fall through to pending check
                }

                // Check for pending requests
                String pendingSql = "SELECT h.location "
                        + "FROM rent_requests r "
                        + "JOIN users u ON r.tenant_id = u.id "
                        + "JOIN houses h ON r.house_id = h.id "
                        + "WHERE (lower(COALESCE(u.email, '')) = lower(?) OR lower(u.name) = lower(?)) "
                        + "AND lower(trim(COALESCE(r.status, ''))) = 'pending' "
                        + "ORDER BY COALESCE(r.request_date, '') DESC, r.id DESC "
                        + "LIMIT 1";

                try (Connection conn = DatabaseConnection.connect();
                        PreparedStatement pstmt = conn.prepareStatement(pendingSql)) {
                    pstmt.setString(1, DataStore.currentUser.getEmail());
                    pstmt.setString(2, DataStore.currentUser.getUsername());

                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            String location = rs.getString("location");
                            return RequestStatusInfo.pending(location);
                        }
                    }
                } catch (SQLException e) {
                    return RequestStatusInfo.none();
                }

                // Check for denied requests and include owner + denial timestamp.
                String deniedSql = "SELECT h.location, COALESCE(o.name, 'Owner') AS owner_name, "
                        + "COALESCE((SELECT MAX(n.created_at) FROM notifications n "
                    + "WHERE n.user_id = u.id AND n.house_id = h.id AND lower(COALESCE(n.type, '')) = 'denial'), '') AS denied_at, "
                    + "COALESCE((SELECT n.id FROM notifications n "
                    + "WHERE n.user_id = u.id AND n.house_id = h.id AND lower(COALESCE(n.type, '')) = 'denial' "
                    + "ORDER BY n.id DESC LIMIT 1), 0) AS denied_notification_id, "
                    + "COALESCE((SELECT n.read FROM notifications n "
                    + "WHERE n.user_id = u.id AND n.house_id = h.id AND lower(COALESCE(n.type, '')) = 'denial' "
                    + "ORDER BY n.id DESC LIMIT 1), 0) AS denied_read "
                        + "FROM rent_requests r "
                        + "JOIN users u ON r.tenant_id = u.id "
                        + "JOIN houses h ON r.house_id = h.id "
                        + "LEFT JOIN users o ON h.owner_id = o.id "
                        + "WHERE (lower(COALESCE(u.email, '')) = lower(?) OR lower(u.name) = lower(?)) "
                        + "AND lower(trim(COALESCE(r.status, ''))) = 'denied' "
                        + "ORDER BY COALESCE(denied_at, '') DESC, COALESCE(r.request_date, '') DESC, r.id DESC "
                        + "LIMIT 1";

                try (Connection conn = DatabaseConnection.connect();
                        PreparedStatement pstmt = conn.prepareStatement(deniedSql)) {
                    pstmt.setString(1, DataStore.currentUser.getEmail());
                    pstmt.setString(2, DataStore.currentUser.getUsername());

                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            String location = rs.getString("location");
                            String ownerName = rs.getString("owner_name");
                            String deniedAt = rs.getString("denied_at");
                            int deniedNotificationId = rs.getInt("denied_notification_id");
                            boolean deniedRead = rs.getInt("denied_read") == 1;
                            return RequestStatusInfo.denied(location, ownerName, deniedAt, deniedNotificationId, deniedRead);
                        }
                    }
                } catch (SQLException e) {
                    return RequestStatusInfo.none();
                }

                return RequestStatusInfo.none();
            }
        };

        task.setOnSucceeded(e -> applyRequestStatus(task.getValue()));
        task.setOnFailed(e -> applyRequestStatus(RequestStatusInfo.none()));

        Thread statusLoader = new Thread(task, "tenant-request-status-loader");
        statusLoader.setDaemon(true);
        statusLoader.start();
    }

    private void applyRequestStatus(RequestStatusInfo info) {
        if (bookingStatusCard == null) {
            return;
        }

        boolean hasStatus = info != null && info.hasStatus;
        bookingStatusCard.setManaged(hasStatus);
        bookingStatusCard.setVisible(hasStatus);

        if (!hasStatus) {
            return;
        }

        String locationText = (info.location == null || info.location.isBlank())
                ? "your selected property"
                : info.location;

        if (bookingStatusTitleLabel != null) {
            if ("APPROVED".equals(info.status)) {
                bookingStatusTitleLabel.setText("Booking Confirmed ✓");
                bookingStatusTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f9548;");
            } else if ("PENDING".equals(info.status)) {
                bookingStatusTitleLabel.setText("Booking Pending");
                bookingStatusTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #854d0e;");
            } else if ("DENIED".equals(info.status)) {
                bookingStatusTitleLabel.setText("Request Denied");
                bookingStatusTitleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #b91c1c;");
            }
        }

        if (bookingStatusMessageLabel != null) {
            if ("APPROVED".equals(info.status)) {
                bookingStatusMessageLabel.setText("Your request for " + locationText + " has been approved!");
                bookingStatusMessageLabel.setStyle("-fx-text-fill: #0f9548;");
            } else if ("PENDING".equals(info.status)) {
                bookingStatusMessageLabel.setText("Your request for " + locationText + " is being reviewed.");
                bookingStatusMessageLabel.setStyle("-fx-text-fill: #a16207;");
            } else if ("DENIED".equals(info.status)) {
                String ownerName = (info.ownerName == null || info.ownerName.isBlank()) ? "The owner" : info.ownerName;
                String deniedAtText = formatNotificationTimestamp(info.deniedAt);
                String message = ownerName + " denied your request for " + locationText + " at " + deniedAtText + ".";
                bookingStatusMessageLabel.setText(message);
                bookingStatusMessageLabel.setStyle("-fx-text-fill: #b91c1c;");

                String notificationKey = ownerName + "|" + locationText + "|" + deniedAtText;
                if (!info.deniedRead && !notificationKey.equals(lastShownDeniedNotificationKey)) {
                    lastShownDeniedNotificationKey = notificationKey;
                    showStatusPopup("Request Denied", message, "OK");
                    if (info.deniedNotificationId > 0) {
                        DataStore.markNotificationAsRead(info.deniedNotificationId);
                    }
                }
            }
        }
    }

    private void refreshTenantNotificationsAsync() {
        if (tenantNotificationsContainer == null) {
            return;
        }

        Task<List<Map<String, String>>> task = new Task<>() {
            @Override
            protected List<Map<String, String>> call() {
                int tenantId = resolveCurrentTenantId();
                if (tenantId <= 0) {
                    return List.of();
                }
                return new ArrayList<>(DataStore.getTenantNotifications(tenantId));
            }
        };

        task.setOnSucceeded(e -> applyTenantNotifications(task.getValue()));
        task.setOnFailed(e -> applyTenantNotifications(List.of()));

        Thread notificationsLoader = new Thread(task, "tenant-notifications-loader");
        notificationsLoader.setDaemon(true);
        notificationsLoader.start();
    }

    private void applyTenantNotifications(List<Map<String, String>> notifications) {
        if (tenantNotificationsContainer == null) {
            return;
        }

        tenantNotificationsContainer.getChildren().clear();
        if (notifications == null || notifications.isEmpty()) {
            Label empty = new Label("No notifications yet.");
            empty.getStyleClass().add("table-text");
            tenantNotificationsContainer.getChildren().add(empty);
            return;
        }

        List<Integer> unreadIds = new ArrayList<>();
        for (Map<String, String> notification : notifications) {
            int notificationId = parseIntSafe(notification.get("id"));
            boolean isRead = "1".equals(notification.get("is_read"));

            VBox row = new VBox(4);
            row.getStyleClass().add("table-row");
            row.setStyle(rowStyleForReadState(isRead));

            Label title = new Label(nonBlankOrFallback(notification.get("title"), "Notification"));
            title.getStyleClass().add("table-text");
            title.setStyle((isRead ? "-fx-text-fill: #7b8794; " : "-fx-text-fill: #d4af37; ") + "-fx-font-weight: 700;");

            Label message = new Label(nonBlankOrFallback(notification.get("message"), "-"));
            message.getStyleClass().add("table-text");
            message.setWrapText(true);
            message.setStyle(isRead ? "-fx-text-fill: #8f9bab;" : "-fx-text-fill: #d4af37;");

            String createdAt = formatNotificationTimestamp(notification.get("created_at"));
            Label meta = new Label((isRead ? "Read" : "Unread") + " - " + createdAt);
            meta.getStyleClass().add("table-text");
            meta.setStyle(isRead ? "-fx-text-fill: #8f9bab;" : "-fx-text-fill: #f59e0b;");

            row.getChildren().addAll(title, message, meta);
            tenantNotificationsContainer.getChildren().add(row);

            if (!isRead && notificationId > 0) {
                unreadIds.add(notificationId);
            }
        }

        if (!unreadIds.isEmpty()) {
            Thread markReadThread = new Thread(() -> {
                for (Integer id : unreadIds) {
                    DataStore.markNotificationAsRead(id);
                }
            }, "tenant-notifications-mark-read");
            markReadThread.setDaemon(true);
            markReadThread.start();
        }
    }

    private String rowStyleForReadState(boolean read) {
        if (read) {
            return "-fx-border-color: #6b7280; -fx-background-color: rgba(127,127,127,0.12);";
        }
        return "-fx-border-color: #f59e0b; -fx-background-color: rgba(245,158,11,0.12);";
    }

    private int parseIntSafe(String raw) {
        if (raw == null || raw.isBlank()) {
            return -1;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private String formatNotificationTimestamp(String rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp.isBlank()) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        String candidate = rawTimestamp.trim();
        DateTimeFormatter dbFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try {
            LocalDateTime parsed = LocalDateTime.parse(candidate, dbFormat);
            return parsed.format(dbFormat);
        } catch (Exception ignored) {
            return candidate;
        }
    }

    private static class RequestStatusInfo {
        private final boolean hasStatus;
        private final String status; // PENDING, APPROVED, DENIED
        private final String location;
        private final String ownerName;
        private final String deniedAt;
        private final int deniedNotificationId;
        private final boolean deniedRead;

        private RequestStatusInfo(boolean hasStatus, String status, String location, String ownerName, String deniedAt,
                int deniedNotificationId, boolean deniedRead) {
            this.hasStatus = hasStatus;
            this.status = status;
            this.location = location;
            this.ownerName = ownerName;
            this.deniedAt = deniedAt;
            this.deniedNotificationId = deniedNotificationId;
            this.deniedRead = deniedRead;
        }

        private static RequestStatusInfo none() {
            return new RequestStatusInfo(false, null, null, null, null, -1, true);
        }

        private static RequestStatusInfo pending(String location) {
            return new RequestStatusInfo(true, "PENDING", location, null, null, -1, true);
        }

        private static RequestStatusInfo approved(String location) {
            return new RequestStatusInfo(true, "APPROVED", location, null, null, -1, true);
        }

        private static RequestStatusInfo denied(String location, String ownerName, String deniedAt,
                int deniedNotificationId, boolean deniedRead) {
            return new RequestStatusInfo(true, "DENIED", location, ownerName, deniedAt, deniedNotificationId,
                    deniedRead);
        }
    }

    private static class HouseDetailsViewData {
        private int id;
        private String title;
        private String location;
        private String ownerName;
        private String availability;
        private String contact;
        private String type;
        private String shortDetail;
        private String description;
        private String tags;
        private double rent;
        private int bedrooms;
        private int bathrooms;
        private double area;
        private boolean familyAllowed;
        private boolean bachelorAllowed;
        private boolean gasAvailable;
        private boolean parkingAvailable;
        private boolean furnished;
        private boolean petFriendly;
        private boolean waterAvailable;
        private List<String> imageSources = new ArrayList<>();
    }

    private void initializeHouseDetailsView() {
        loadHouseDetailsAsync();

        if (detailsImageView != null) {
            detailsImageView.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null && detailCarouselTimeline != null) {
                    detailCarouselTimeline.stop();
                }
            });
        }
    }

    private void loadHouseDetailsAsync() {
        if (detailsTitleLabel == null) {
            return;
        }

        int houseId = DataStore.selectedHouseId;
        if (houseId <= 0) {
            applyHouseDetailsData(null);
            return;
        }

        Task<HouseDetailsViewData> task = new Task<>() {
            @Override
            protected HouseDetailsViewData call() {
                return fetchHouseDetailsData(houseId);
            }
        };

        task.setOnSucceeded(e -> applyHouseDetailsData(task.getValue()));
        task.setOnFailed(e -> applyHouseDetailsData(null));

        Thread detailsLoader = new Thread(task, "tenant-house-details-loader");
        detailsLoader.setDaemon(true);
        detailsLoader.start();
    }

    private HouseDetailsViewData fetchHouseDetailsData(int houseId) {
        HouseDetailsViewData data = null;
        String mainImage = null;
        String sql = "SELECT h.id, "
                + "COALESCE(NULLIF(TRIM(h.title), ''), 'House Listing') AS title, "
                + "COALESCE(h.location, '-') AS location, "
                + "COALESCE(h.rent, 0) AS rent, "
                + "COALESCE(u.name, '-') AS owner_name, "
                + "COALESCE(h.type, '-') AS type, "
                + "COALESCE(h.bedrooms, 0) AS bedrooms, "
                + "COALESCE(h.bathrooms, 0) AS bathrooms, "
                + "COALESCE(h.area, 0) AS area, "
                + "COALESCE(h.short_detail, '') AS short_detail, "
                + "COALESCE(h.details, '') AS details, "
                + "COALESCE(h.tags, '') AS tags, "
                + "COALESCE(h.availability, '') AS availability, "
                + "COALESCE(h.contact_info, '') AS contact_info, "
                + "COALESCE(h.family_allowed, 0) AS family_allowed, "
                + "COALESCE(h.bachelor_allowed, 0) AS bachelor_allowed, "
                + "COALESCE(h.gas_available, 0) AS gas_available, "
                + "COALESCE(h.parking_available, 0) AS parking_available, "
                + "COALESCE(h.furnished, 0) AS furnished, "
                + "COALESCE(h.pet_friendly, 0) AS pet_friendly, "
                + "COALESCE(h.water_available, 0) AS water_available, "
                + "COALESCE(h.image, '') AS image "
                + "FROM houses h "
                + "LEFT JOIN users u ON u.id = h.owner_id "
                + "WHERE h.id = ? "
                + "LIMIT 1";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, houseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                data = new HouseDetailsViewData();
                data.id = rs.getInt("id");
                data.title = rs.getString("title");
                data.location = rs.getString("location");
                data.rent = rs.getDouble("rent");
                data.ownerName = rs.getString("owner_name");
                data.type = rs.getString("type");
                data.bedrooms = rs.getInt("bedrooms");
                data.bathrooms = rs.getInt("bathrooms");
                data.area = rs.getDouble("area");
                data.shortDetail = rs.getString("short_detail");
                data.description = rs.getString("details");
                data.tags = rs.getString("tags");
                data.availability = rs.getString("availability");
                data.contact = rs.getString("contact_info");
                data.familyAllowed = rs.getInt("family_allowed") == 1;
                data.bachelorAllowed = rs.getInt("bachelor_allowed") == 1;
                data.gasAvailable = rs.getInt("gas_available") == 1;
                data.parkingAvailable = rs.getInt("parking_available") == 1;
                data.furnished = rs.getInt("furnished") == 1;
                data.petFriendly = rs.getInt("pet_friendly") == 1;
                data.waterAvailable = rs.getInt("water_available") == 1;

                mainImage = rs.getString("image");
            }

            if (data != null) {
                List<String> gallerySources = new ArrayList<>();
                String imageSql = "SELECT sort_order FROM house_images WHERE house_id = ? ORDER BY sort_order ASC";
                try (PreparedStatement imageStmt = conn.prepareStatement(imageSql)) {
                    imageStmt.setInt(1, houseId);
                    try (ResultSet imageRs = imageStmt.executeQuery()) {
                        while (imageRs.next()) {
                            int sortOrder = imageRs.getInt("sort_order");
                            gallerySources.add("db-image://house/" + houseId + "/" + sortOrder);
                        }
                    }
                }

                if (!gallerySources.isEmpty()) {
                    data.imageSources.addAll(gallerySources);
                } else if (mainImage != null && !mainImage.isBlank()) {
                    data.imageSources.add(mainImage.trim());
                }
            }
        } catch (SQLException e) {
            return null;
        }

        return data;
    }

    private void applyHouseDetailsData(HouseDetailsViewData data) {
        if (data == null) {
            if (detailsTitleLabel != null) {
                detailsTitleLabel.setText("House details unavailable");
            }
            if (detailsDescriptionLabel != null) {
                detailsDescriptionLabel.setText("Could not load this listing. Please return to Properties and try again.");
            }
            renderTagChips(null);
            updateDetailCarousel(List.of());
            return;
        }

        detailsTitleLabel.setText(nonBlankOrFallback(data.title, "House Listing"));
        detailsLocationLabel.setText(nonBlankOrFallback(data.location, "-"));
        detailsRentLabel.setText("BDT " + (int) data.rent + " / month");
        detailsOwnerLabel.setText(nonBlankOrFallback(data.ownerName, "-"));
        detailsAvailabilityLabel.setText(nonBlankOrFallback(data.availability, "Not specified"));
        detailsContactLabel.setText(nonBlankOrFallback(data.contact, "Not provided"));

        detailsTypeLabel.setText(nonBlankOrFallback(data.type, "-"));
        detailsBedroomsLabel.setText(String.valueOf(Math.max(0, data.bedrooms)));
        detailsBathroomsLabel.setText(String.valueOf(Math.max(0, data.bathrooms)));
        detailsAreaLabel.setText((int) Math.max(0, data.area) + " sqft");

        detailsShortDetailLabel.setText(nonBlankOrFallback(data.shortDetail, "No short summary provided."));
        detailsDescriptionLabel.setText(nonBlankOrFallback(data.description, "No detailed description provided."));
        renderTagChips(data.tags);
        detailsAmenitiesLabel.setText(buildAmenitiesText(data));

        updateDetailCarousel(data.imageSources);
    }

    private String buildAmenitiesText(HouseDetailsViewData data) {
        List<String> amenities = new ArrayList<>();
        if (data.familyAllowed)
            amenities.add("Family");
        if (data.bachelorAllowed)
            amenities.add("Bachelor");
        if (data.gasAvailable)
            amenities.add("Gas");
        if (data.parkingAvailable)
            amenities.add("Parking");
        if (data.furnished)
            amenities.add("Furnished");
        if (data.petFriendly)
            amenities.add("Pet Friendly");
        if (data.waterAvailable)
            amenities.add("Water Available");

        if (amenities.isEmpty()) {
            return "No amenities marked by owner.";
        }
        return String.join("  |  ", amenities);
    }

    private void renderTagChips(String rawTags) {
        if (detailsTagsContainer == null) {
            return;
        }

        detailsTagsContainer.getChildren().clear();
        List<String> tags = parseTagValues(rawTags);
        String chipTextColor = DataStore.darkMode ? "#D4AF37" : "#f9fafb";
        String chipStyle = "-fx-background-color: #95a0b8; -fx-border-color: #6b7280; -fx-background-radius: 999; "
            + "-fx-border-radius: 999; -fx-padding: 6 12 6 12; -fx-text-fill: " + chipTextColor
            + "; -fx-font-weight: 600;";

        if (tags.isEmpty()) {
            Button noTagsChip = new Button("No tags");
            noTagsChip.getStyleClass().add("tag-chip");
            noTagsChip.setStyle(chipStyle);
            noTagsChip.setFocusTraversable(false);
            noTagsChip.setMouseTransparent(true);
            detailsTagsContainer.getChildren().add(noTagsChip);
            return;
        }

        for (String tag : tags) {
            Button chip = new Button(tag);
            chip.getStyleClass().add("tag-chip");
            chip.setStyle(chipStyle);
            chip.setFocusTraversable(false);
            chip.setMouseTransparent(true);
            detailsTagsContainer.getChildren().add(chip);
        }
    }

    private List<String> parseTagValues(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return List.of();
        }

        String[] tokens = rawTags.split("[,|;/\\n]");
        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>();
        for (String token : tokens) {
            String cleaned = token == null ? "" : token.trim();
            if (!cleaned.isEmpty()) {
                normalizedTags.add(cleaned);
            }
        }

        return new ArrayList<>(normalizedTags);
    }

    private String nonBlankOrFallback(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private void updateDetailCarousel(List<String> imageSources) {
        detailCarouselImages.clear();
        if (detailCarouselTimeline != null) {
            detailCarouselTimeline.stop();
        }

        if (imageSources != null) {
            for (String source : imageSources) {
                try {
                    detailCarouselImages.add(loadHouseImage(source));
                } catch (Exception ignored) {
                    // Skip bad image entries.
                }
            }
        }

        if (detailCarouselImages.isEmpty()) {
            detailCarouselImages.add(loadHouseImage(null));
        }

        detailCarouselIndex = 0;
        showDetailCarouselImage(false);

        if (detailCarouselImages.size() > 1) {
            detailCarouselTimeline = new Timeline(new KeyFrame(Duration.seconds(3.5), e -> onNextDetailImage()));
            detailCarouselTimeline.setCycleCount(Timeline.INDEFINITE);
            detailCarouselTimeline.play();
        }
    }

    @FXML
    private void onPreviousDetailImage() {
        if (detailCarouselImages.isEmpty()) {
            return;
        }
        detailCarouselIndex = (detailCarouselIndex - 1 + detailCarouselImages.size()) % detailCarouselImages.size();
        showDetailCarouselImage(true);
    }

    @FXML
    private void onNextDetailImage() {
        if (detailCarouselImages.isEmpty()) {
            return;
        }
        detailCarouselIndex = (detailCarouselIndex + 1) % detailCarouselImages.size();
        showDetailCarouselImage(true);
    }

    private void showDetailCarouselImage(boolean animate) {
        if (detailsImageView == null || detailCarouselImages.isEmpty()) {
            return;
        }

        detailsImageView.setImage(detailCarouselImages.get(detailCarouselIndex));
        if (detailsImageCounterLabel != null) {
            detailsImageCounterLabel
                    .setText((detailCarouselIndex + 1) + " / " + detailCarouselImages.size());
        }

        if (animate) {
            detailsImageView.setOpacity(0.0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(260), detailsImageView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        } else {
            detailsImageView.setOpacity(1.0);
        }
    }

    private void onBookRequest(House h) {
        if (h == null) {
            return;
        }

        if (h.getId() <= 0) {
            new Alert(Alert.AlertType.WARNING, "Could not open details for this property.").show();
            return;
        }

        DataStore.selectedHouseId = h.getId();
        try {
            Stage stage = (Stage) pageRoot.getScene().getWindow();
            loadScene(stage, "tenant-house-details.fxml");
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Could not open property details.").show();
        }
    }

    private void onRentHouse(House h) {
        if (h == null || h.getId() <= 0) {
            new Alert(Alert.AlertType.WARNING, "Could not identify this house for booking.").show();
            return;
        }

        int tenantId = resolveCurrentTenantId();
        if (tenantId <= 0) {
            new Alert(Alert.AlertType.WARNING, "Please sign in again to continue.").show();
            return;
        }

        submitRentRequest(h.getId(), tenantId, h.getLocation());
    }

    @FXML
    private void onBookNowFromDetails() {
        int houseId = DataStore.selectedHouseId;
        if (houseId <= 0) {
            new Alert(Alert.AlertType.WARNING, "No house selected.").show();
            return;
        }

        int tenantId = resolveCurrentTenantId();
        if (tenantId <= 0) {
            new Alert(Alert.AlertType.WARNING, "Please sign in again to continue.").show();
            return;
        }

        String location = detailsLocationLabel == null ? "this house" : detailsLocationLabel.getText();
        submitRentRequest(houseId, tenantId, location);
    }

    private void submitRentRequest(int houseId, int tenantId, String locationLabel) {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                String checkSql = "SELECT COUNT(*) AS cnt FROM rent_requests "
                        + "WHERE tenant_id = ? "
                        + "AND lower(trim(COALESCE(status, ''))) IN ('pending', 'approved')";
                String insertSql = "INSERT INTO rent_requests (house_id, tenant_id, request_date, move_in_date, status) "
                        + "VALUES (?, ?, ?, ?, 'pending')";
                String today = LocalDate.now().toString();
                String moveIn = LocalDate.now().plusDays(14).toString();

                try (Connection conn = DatabaseConnection.connect()) {
                    try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                        checkStmt.setInt(1, tenantId);
                        try (ResultSet rs = checkStmt.executeQuery()) {
                            if (rs.next() && rs.getInt("cnt") > 0) {
                                return 1;
                            }
                        }
                    }

                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, houseId);
                        insertStmt.setInt(2, tenantId);
                        insertStmt.setString(3, today);
                        insertStmt.setString(4, moveIn);
                        return insertStmt.executeUpdate() > 0 ? 2 : 0;
                    }
                } catch (SQLException e) {
                    return 0;
                }
            }
        };

        task.setOnSucceeded(e -> {
            Integer status = task.getValue();
            if (status == null || status == 0) {
                showStatusPopup("Error", "Could not submit rent request.\nPlease try again.", "OK");
                return;
            }

            if (status == 1) {
                showStatusPopup("Active Request", "You already have an active request.\nComplete or wait for a response on your current request.", "OK");
                return;
            }

            showStatusPopup("Success", "Rent request sent for\n" + locationLabel + ".", "OK");
            refreshPendingRequestStatusAsync();
            refreshTenantBookingKpisAsync();
        });

        task.setOnFailed(e -> showStatusPopup("Error", "Could not submit rent request.\nPlease try again.", "OK"));

        Thread requestThread = new Thread(task, "tenant-rent-request-submit");
        requestThread.setDaemon(true);
        requestThread.start();
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

    @FXML
    private void onLogout(javafx.event.ActionEvent event) throws IOException {
        if (showLogoutConfirmation()) {
            DataStore.currentUser = null;
            DataStore.clearRememberedSession();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            loadScene(stage, "login-view.fxml");
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

    @FXML
    private void onThemeToggle() {
        if (themeToggle == null) {
            return;
        }
        DataStore.darkMode = themeToggle.isSelected();
        try {
            Stage stage = (Stage) themeToggle.getScene().getWindow();
            loadScene(stage, getCurrentBaseFxml());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onOpenDashboard(javafx.event.ActionEvent event) throws IOException {
        loadFromEvent(event, "tenant-view.fxml");
    }

    @FXML
    private void onOpenSearch(javafx.event.ActionEvent event) throws IOException {
        loadFromEvent(event, "tenant-search.fxml");
    }

    @FXML
    private void onOpenWishlist(javafx.event.ActionEvent event) throws IOException {
        loadFromEvent(event, "tenant-wishlist.fxml");
    }

    @FXML
    private void onOpenReview(javafx.event.ActionEvent event) throws IOException {
        loadFromEvent(event, "tenant-review.fxml");
    }

    @FXML
    private void onSubmitReview(javafx.event.ActionEvent event) {
        if (reviewTextArea == null || reviewHouseSelector == null) {
            return;
        }

        ReviewHouseOption selected = reviewHouseSelector.getValue();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a house to review.").show();
            return;
        }

        String text = reviewTextArea.getText() == null ? "" : reviewTextArea.getText().trim();
        if (text.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Please write a review before submitting.").show();
            return;
        }

        int tenantId = resolveCurrentTenantId();
        if (!saveReviewToDatabase(selected.houseId, tenantId, text, "submitted")) {
            new Alert(Alert.AlertType.ERROR, "Could not save review. Please try again.").show();
            return;
        }

        addReviewCard("Submitted", selected.label, text,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
        reviewTextArea.clear();
        new Alert(Alert.AlertType.INFORMATION, "Review submitted successfully.").show();
    }

    @FXML
    private void onSaveDraft(javafx.event.ActionEvent event) {
        if (reviewTextArea == null || reviewHouseSelector == null) {
            return;
        }

        ReviewHouseOption selected = reviewHouseSelector.getValue();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Please select a house for this draft.").show();
            return;
        }

        String text = reviewTextArea.getText() == null ? "" : reviewTextArea.getText().trim();
        if (text.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Draft is empty. Write something first.").show();
            return;
        }

        int tenantId = resolveCurrentTenantId();
        if (!saveReviewToDatabase(selected.houseId, tenantId, text, "draft")) {
            new Alert(Alert.AlertType.ERROR, "Could not save draft. Please try again.").show();
            return;
        }

        addReviewCard("Draft", selected.label, text,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
        new Alert(Alert.AlertType.INFORMATION, "Draft saved successfully.").show();
    }

    private boolean saveReviewToDatabase(int houseId, int tenantId, String reviewText, String status) {
        if (houseId <= 0 || tenantId <= 0 || reviewText == null || reviewText.isBlank()) {
            return false;
        }

        if (!isEligibleForReview(houseId, tenantId)) {
            new Alert(Alert.AlertType.WARNING,
                    "You can review only houses approved for you at least 1 month ago.").show();
            return false;
        }

        String updateSql = "UPDATE house_reviews SET review_text = ?, status = ?, updated_at = ? "
                + "WHERE house_id = ? AND tenant_id = ?";
        String insertSql = "INSERT INTO house_reviews (house_id, tenant_id, review_text, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String today = LocalDate.now().toString();

        try (Connection conn = DatabaseConnection.connect()) {
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, reviewText);
                updateStmt.setString(2, status);
                updateStmt.setString(3, today);
                updateStmt.setInt(4, houseId);
                updateStmt.setInt(5, tenantId);
                int updatedRows = updateStmt.executeUpdate();
                if (updatedRows > 0) {
                    return true;
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, houseId);
                insertStmt.setInt(2, tenantId);
                insertStmt.setString(3, reviewText);
                insertStmt.setString(4, status);
                insertStmt.setString(5, today);
                insertStmt.setString(6, today);
                return insertStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean isEligibleForReview(int houseId, int tenantId) {
        String sql = "SELECT 1 FROM rent_requests "
                + "WHERE house_id = ? AND tenant_id = ? "
                + "AND lower(trim(COALESCE(status, ''))) = 'approved' "
                + "AND date(COALESCE(NULLIF(TRIM(accepted_at), ''), request_date)) <= date('now', '-1 month') "
                + "LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, houseId);
            pstmt.setInt(2, tenantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void loadRecentReviews() {
        if (reviewsContainer == null) {
            return;
        }

        reviewsContainer.getChildren().clear();
        int tenantId = resolveCurrentTenantId();
        if (tenantId <= 0) {
            return;
        }

        String sql = "SELECT hr.status, hr.review_text, COALESCE(hr.updated_at, hr.created_at) AS review_date, "
                + "COALESCE(NULLIF(TRIM(h.title), ''), h.location, 'Untitled House') AS house_title, "
                + "COALESCE(h.location, '-') AS location, COALESCE(h.rent, 0) AS rent "
                + "FROM house_reviews hr "
                + "JOIN houses h ON h.id = hr.house_id "
                + "WHERE hr.tenant_id = ? "
                + "ORDER BY COALESCE(hr.updated_at, hr.created_at) DESC, hr.id DESC";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tenantId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String status = normalizeReviewStatus(rs.getString("status"));
                    String houseLabel = rs.getString("house_title") + " - " + rs.getString("location")
                            + " (BDT " + (int) rs.getDouble("rent") + ")";
                    String text = rs.getString("review_text");
                    String dateText = rs.getString("review_date");
                    addReviewCard(status, houseLabel, text, dateText == null ? "" : dateText);
                }
            }
        } catch (SQLException e) {
            // Keep review area empty if loading fails.
        }
    }

    private String normalizeReviewStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Submitted";
        }
        return "draft".equalsIgnoreCase(status.trim()) ? "Draft" : "Submitted";
    }

    private void addReviewCard(String status, String selectedHouse, String reviewText, String reviewDateText) {
        if (reviewsContainer == null) {
            return;
        }

        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: rgba(127,139,160,0.25); -fx-background-radius: 10; -fx-border-color: rgba(127,139,160,0.8); -fx-border-radius: 10;");

        Label header = new Label(status + " • " + selectedHouse + " • " + reviewDateText);
        header.setStyle("-fx-font-weight: bold;");

        Label body = new Label(reviewText);
        body.setWrapText(true);

        card.getChildren().addAll(header, body);
        reviewsContainer.getChildren().add(0, card);
    }

    private void loadFromEvent(javafx.event.ActionEvent event, String baseFxml) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        loadScene(stage, baseFxml);
    }

    private String getCurrentBaseFxml() {
        if (pageRoot != null && pageRoot.getUserData() instanceof String) {
            String baseFxml = (String) pageRoot.getUserData();
            if (baseFxml != null && !baseFxml.isBlank()) {
                return baseFxml;
            }
        }
        return "tenant-view.fxml";
    }

    private void applyActiveNav(String baseFxml) {
        if (navDashboardButton == null || navSearchButton == null || navWishlistButton == null
                || navReviewButton == null) {
            return;
        }
        navDashboardButton.getStyleClass().remove("nav-button-active");
        navSearchButton.getStyleClass().remove("nav-button-active");
        navWishlistButton.getStyleClass().remove("nav-button-active");
        navReviewButton.getStyleClass().remove("nav-button-active");

        String normalized = baseFxml == null ? "" : baseFxml.toLowerCase();
        if (normalized.contains("tenant-search")) {
            navSearchButton.getStyleClass().add("nav-button-active");
        } else if (normalized.contains("tenant-wishlist")) {
            navWishlistButton.getStyleClass().add("nav-button-active");
        } else if (normalized.contains("tenant-review")) {
            navReviewButton.getStyleClass().add("nav-button-active");
        } else {
            navDashboardButton.getStyleClass().add("nav-button-active");
        }
    }

    private void loadScene(Stage stage, String baseFxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(
                DataStore.resolveFxml(baseFxml)));
        Scene scene = stage.getScene();
        if (scene == null) {
            stage.setScene(new Scene(root));
        } else {
            DataStore.prepareSceneForRootSwap(scene);
            scene.setRoot(root);
        }
        DataStore.applyTheme(stage.getScene());
        stage.show();
    }
}