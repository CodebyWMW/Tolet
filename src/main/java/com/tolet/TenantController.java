package com.tolet;

import javafx.animation.PauseTransition;
import javafx.animation.KeyFrame;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TenantController {
    private static final double PROPERTY_CARD_WIDTH = 350;

    @FXML
    private Label welcomeLabel, totalCountLabel;
    @FXML
    private TextField searchField;
    @FXML
    private FlowPane filterContainer;
    @FXML
    private FlowPane propertiesGrid;
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

    private List<House> allHouses;
    private List<String> activeFilters = new ArrayList<>();
    private final String[] FILTERS = { "Family", "Bachelor", "Gas Available", "Parking", "Furnished" };
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();
    private Timeline refreshTimeline;

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
            refreshPendingRequestStatusAsync();
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
            renderProperties(allHouses);
        });

        task.setOnFailed(e -> {
            allHouses = new ArrayList<>();
            renderProperties(allHouses);
        });

        Thread loaderThread = new Thread(task, "tenant-houses-loader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private void startAutoRefresh() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }

        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(20), e -> loadHousesAsync()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.getKeyFrames().setAll(new KeyFrame(Duration.seconds(20), e -> {
            loadHousesAsync();
            refreshPendingRequestStatusAsync();
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

    private void renderProperties(List<House> houses) {
        if (propertiesGrid == null || totalCountLabel == null) {
            return;
        }
        propertiesGrid.getChildren().clear();
        totalCountLabel.setText(houses.size() + " properties");

        for (House h : houses) {
            propertiesGrid.getChildren().add(createPropertyCard(h));
        }
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

        // Content
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));

        // Type Badge
        Label typeBadge = new Label(h.getType());
        typeBadge.getStyleClass().add("card-badge");

        HBox header = new HBox(typeBadge);

        Label title = new Label(h.getBedrooms() + "BR Apartment");
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

        content.getChildren().addAll(header, title, location, stats, new Separator(), price, bookBtn);
        card.getChildren().addAll(imgView, content);

        return card;
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

        Task<PendingStatusInfo> task = new Task<>() {
            @Override
            protected PendingStatusInfo call() {
                if (DataStore.currentUser == null) {
                    return PendingStatusInfo.none();
                }

                String sql = "SELECT h.location "
                        + "FROM rent_requests r "
                        + "JOIN users u ON r.tenant_id = u.id "
                        + "JOIN houses h ON r.house_id = h.id "
                        + "WHERE (lower(COALESCE(u.email, '')) = lower(?) OR lower(u.name) = lower(?)) "
                        + "AND lower(trim(COALESCE(r.status, ''))) = 'pending' "
                        + "ORDER BY COALESCE(r.request_date, '') DESC, r.id DESC "
                        + "LIMIT 1";

                try (Connection conn = DatabaseConnection.connect();
                        PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, DataStore.currentUser.getEmail());
                    pstmt.setString(2, DataStore.currentUser.getUsername());

                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            String location = rs.getString("location");
                            return PendingStatusInfo.pending(location);
                        }
                    }
                } catch (SQLException e) {
                    return PendingStatusInfo.none();
                }

                return PendingStatusInfo.none();
            }
        };

        task.setOnSucceeded(e -> applyPendingStatus(task.getValue()));
        task.setOnFailed(e -> applyPendingStatus(PendingStatusInfo.none()));

        Thread statusLoader = new Thread(task, "tenant-pending-status-loader");
        statusLoader.setDaemon(true);
        statusLoader.start();
    }

    private void applyPendingStatus(PendingStatusInfo info) {
        if (bookingStatusCard == null) {
            return;
        }

        boolean hasPending = info != null && info.hasPending;
        bookingStatusCard.setManaged(hasPending);
        bookingStatusCard.setVisible(hasPending);

        if (!hasPending) {
            return;
        }

        if (bookingStatusTitleLabel != null) {
            bookingStatusTitleLabel.setText("Booking Pending");
        }
        if (bookingStatusMessageLabel != null) {
            String locationText = (info.location == null || info.location.isBlank())
                    ? "your selected property"
                    : info.location;
            bookingStatusMessageLabel.setText("Your request for " + locationText + " is being reviewed.");
        }
    }

    private static class PendingStatusInfo {
        private final boolean hasPending;
        private final String location;

        private PendingStatusInfo(boolean hasPending, String location) {
            this.hasPending = hasPending;
            this.location = location;
        }

        private static PendingStatusInfo none() {
            return new PendingStatusInfo(false, null);
        }

        private static PendingStatusInfo pending(String location) {
            return new PendingStatusInfo(true, location);
        }
    }

    private void onBookRequest(House h) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Booking request sent to owner of " + h.getLocation());
        alert.show();
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
        stage.show();
    }
}