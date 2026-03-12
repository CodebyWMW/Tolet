package com.tolet;

import javafx.animation.PauseTransition;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class TenantController {
    @FXML
    private Label welcomeLabel, totalCountLabel;
    @FXML
    private TextField searchField;
    @FXML
    private FlowPane filterContainer;
    @FXML
    private FlowPane propertiesGrid;
    @FXML
    private ToggleButton themeToggle;

    private List<House> allHouses;
    private List<String> activeFilters = new ArrayList<>();
    private final String[] FILTERS = { "Family", "Bachelor", "Gas Available", "Parking", "Furnished" };
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));
    private static final Map<String, Image> IMAGE_CACHE = new HashMap<>();

    @FXML
    public void initialize() {
        allHouses = new ArrayList<>();

        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }

        if (DataStore.currentUser != null) {
            welcomeLabel.setText("Welcome Back, " + DataStore.currentUser.getUsername() + "!");
        }

        // Initialize Filters
        for (String filter : FILTERS) {
            ToggleButton chip = new ToggleButton(filter);
            chip.getStyleClass().add("filter-chip");
            chip.setOnAction(e -> toggleFilter(filter, chip.isSelected()));
            filterContainer.getChildren().add(chip);
        }

        // Load data off the UI thread to keep scene switch responsive.
        loadHousesAsync();

        // Search listener with debounce to avoid rerendering on every keystroke.
        searchDebounce.setOnFinished(e -> applyFilters());
        searchField.textProperty().addListener((obs, old, val) -> searchDebounce.playFromStart());
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
        propertiesGrid.getChildren().clear();
        totalCountLabel.setText(houses.size() + " properties");

        for (House h : houses) {
            propertiesGrid.getChildren().add(createPropertyCard(h));
        }
    }

    private Node createPropertyCard(House h) {
        VBox card = new VBox();
        card.getStyleClass().add("property-card");
        card.setPrefWidth(300);
        card.setMinWidth(300);

        // Image
        ImageView imgView = new ImageView();
        try {
            imgView.setImage(loadHouseImage(h.getImage()));
        } catch (Exception e) {
            /* fallback image if URL fails */ }
        imgView.setFitWidth(300);
        imgView.setFitHeight(200);

        // Clip image to rounded top corners
        Rectangle clip = new Rectangle(300, 200);
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

    private void onBookRequest(House h) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Booking request sent to owner of " + h.getLocation());
        alert.show();
    }

    @FXML
    private void onLogout(javafx.event.ActionEvent event) throws IOException {
        if (showLogoutConfirmation()) {
            DataStore.currentUser = null;
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            loadScene(stage, "login-view.fxml");
        }
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

    @FXML
    private void onThemeToggle() {
        if (themeToggle == null) {
            return;
        }
        DataStore.darkMode = themeToggle.isSelected();
        try {
            Stage stage = (Stage) themeToggle.getScene().getWindow();
            loadScene(stage, "tenant-view.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadScene(Stage stage, String baseFxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(
                DataStore.resolveFxml(baseFxml)));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        DataStore.applyWindowSize(stage);
        stage.show();
    }
}