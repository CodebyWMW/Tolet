package com.tolet;

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
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    private List<House> allHouses;
    private List<String> activeFilters = new ArrayList<>();
    private final String[] FILTERS = { "Family", "Bachelor", "Gas Available", "Parking", "Furnished" };

    @FXML
    public void initialize() {
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

        // Load Data
        allHouses = DataStore.getHouses();
        renderProperties(allHouses);

        // Search Listener
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void toggleFilter(String filter, boolean isSelected) {
        if (isSelected)
            activeFilters.add(filter);
        else
            activeFilters.remove(filter);
        applyFilters();
    }

    private void applyFilters() {
        String search = searchField.getText().toLowerCase();

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
            imgView.setImage(new Image(h.getImage(), 300, 200, true, true));
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

    private void onBookRequest(House h) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Booking request sent to owner of " + h.getLocation());
        alert.show();
    }

    @FXML
    private void onLogout(javafx.event.ActionEvent event) throws IOException {
        DataStore.currentUser = null;
        Parent root = FXMLLoader.load(getClass().getResource("login-view.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        DataStore.applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }
}