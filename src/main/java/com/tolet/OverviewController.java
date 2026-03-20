package com.tolet;

import java.io.IOException;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import services.UserService;

public class OverviewController {

    @FXML
    private Button seePopularButton;
    @FXML
    private Button joinButton;
    @FXML
    private Button loginButton;
    @FXML
    private ToggleButton themeToggle;
    @FXML
    private FlowPane houseCardsPane;

    // Sample house data with images
    private static final String[][] SAMPLE_HOUSES = {
            { "৳ 12,000/month", "2 Bed • 1 Bath • 750 sqft", "Mirpur-10, Dhaka", "house1.png" },
            { "৳ 18,500/month", "3 Bed • 2 Bath • 1200 sqft", "Dhanmondi, Dhaka", "house2.jpg" },
            { "৳ 25,000/month", "4 Bed • 3 Bath • 1800 sqft", "Gulshan-1, Dhaka", "house3.jpg" },
            { "৳ 15,000/month", "2 Bed • 2 Bath • 950 sqft", "Uttara Sector-7, Dhaka", "house4.jpg" },
            { "৳ 10,500/month", "1 Bed • 1 Bath • 550 sqft", "Banani, Dhaka", "house5.jpg" }
    };

    @FXML
    public void initialize() {
        System.out.println("OverviewController.initialize() called");

        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
            System.out.println("Theme toggle initialized");
        } else {
            System.out.println("WARNING: themeToggle is null");
        }

        // Load house cards
        if (houseCardsPane != null) {
            System.out.println("Loading house cards...");
            loadHouseCards();
        } else {
            System.out.println("WARNING: houseCardsPane is null in initialize()");
        }
    }

    private void loadHouseCards() {
        houseCardsPane.getChildren().clear();

        // If a tenant is logged in, load houses from DB; otherwise show sample data
        if (DataStore.currentUser != null && DataStore.currentUser.getRole() != null
                && DataStore.currentUser.getRole().toLowerCase().contains("tenant")) {
            ObservableList<House> houses = DataStore.getHouses();
            for (House h : houses) {
                String price = "৳ " + (int) h.getRent() + "/month";
                String details = h.getBedrooms() + " Bed • " + h.getBathrooms() + " Bath • " + (int) h.getArea()
                        + " sqft";
                String location = h.getLocation();
                String imageName = h.getImage();
                VBox card = createHouseCard(price, details, location, imageName);
                houseCardsPane.getChildren().add(card);
            }
            System.out.println("Loaded " + houses.size() + " house cards from DB");
        } else {
            // Add all 5 sample cards
            for (int i = 0; i < SAMPLE_HOUSES.length; i++) {
                VBox card = createHouseCard(SAMPLE_HOUSES[i][0], SAMPLE_HOUSES[i][1], SAMPLE_HOUSES[i][2],
                        SAMPLE_HOUSES[i][3]);
                houseCardsPane.getChildren().add(card);
            }
            System.out.println("Loaded " + SAMPLE_HOUSES.length + " sample house cards");
        }
    }

    private VBox createHouseCard(String price, String details, String location, String imageName) {
        VBox card = new VBox(10);
        card.setPrefWidth(265);
        card.setPadding(new Insets(10));
        card.getStyleClass().add("house-card");

        // Different colors for light/dark mode
        String bgColor = DataStore.darkMode ? "#30302e" : "#99a3b4"; // Gray in light mode, dark in dark mode
        String priceColor = DataStore.darkMode ? "#ff7547" : "#D4AF37";
        String detailsColor = DataStore.darkMode ? "#e8fff5" : "#ffffff";
        String locationColor = DataStore.darkMode ? "#faa387" : "#1F2933"; // Dark navy text in light mode
        String placeholderColor = DataStore.darkMode ? "#474743" : "#1F2933"; // Dark navy placeholder in light mode

        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 12;");

        // House image
        ImageView imageView = new ImageView();
        imageView.setFitWidth(245);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(false);

        try {
            Image image = new Image(getClass().getResourceAsStream("images/" + imageName));
            imageView.setImage(image);
            imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 2);");
        } catch (Exception e) {
            // Fallback to rectangle if image not found
            System.out.println("Image not found: " + imageName + ", using placeholder");
            Rectangle placeholder = new Rectangle(245, 160);
            placeholder.setArcWidth(8);
            placeholder.setArcHeight(8);
            placeholder.setStyle("-fx-fill: " + placeholderColor + ";");

            VBox infoBox = new VBox(5);
            Label priceLabel = new Label(price);
            priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + priceColor + ";");
            Label detailsLabel = new Label(details);
            detailsLabel.setStyle("-fx-text-fill: " + detailsColor + ";");
            Label locationLabel = new Label(location);
            locationLabel.setStyle("-fx-text-fill: " + locationColor + ";");
            infoBox.getChildren().addAll(priceLabel, detailsLabel, locationLabel);
            card.getChildren().addAll(placeholder, infoBox);
            return card;
        }

        // Info container
        VBox infoBox = new VBox(5);

        Label priceLabel = new Label(price);
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + priceColor + ";");

        Label detailsLabel = new Label(details);
        detailsLabel.setStyle("-fx-text-fill: " + detailsColor + ";");

        Label locationLabel = new Label(location);
        locationLabel.setStyle("-fx-text-fill: " + locationColor + ";");

        infoBox.getChildren().addAll(priceLabel, detailsLabel, locationLabel);
        card.getChildren().addAll(imageView, infoBox);

        return card;
    }

    @FXML
    private void onSeePopularHouses() {
        navigateToLogin();
    }

    @FXML
    private void onJoinForFree() {
        showSignUpDialog();
    }

    @FXML
    private void onLoginClick() {
        navigateToLogin();
    }

    @FXML
    private void onBrowseRents() {
        navigateToLogin();
    }

    @FXML
    private void onViewAllClick() {
        navigateToLogin();
    }

    private void navigateToLogin() {
        try {
            Stage stage = (Stage) seePopularButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource(
                    DataStore.resolveFxml("login-view.fxml")));
            switchSceneRoot(stage, root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSignUpDialog() {
        try {
            Stage stage = (Stage) seePopularButton.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource(
                    DataStore.resolveFxml("signup-view.fxml")));
            switchSceneRoot(stage, root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onThemeToggle() {
        if (themeToggle == null) {
            return;
        }
        DataStore.darkMode = themeToggle.isSelected();
        try {
            Parent root = FXMLLoader.load(getClass().getResource(
                    DataStore.resolveFxml("Overview.fxml")));
            Stage stage = (Stage) themeToggle.getScene().getWindow();
            switchSceneRoot(stage, root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void switchSceneRoot(Stage stage, Parent root) {
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
