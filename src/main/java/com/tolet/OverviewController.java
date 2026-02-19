package com.tolet;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import models.User;
import services.UserService;
import java.io.IOException;
import java.util.Optional;

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

        // Add all 5 sample cards
        for (int i = 0; i < SAMPLE_HOUSES.length; i++) {
            VBox card = createHouseCard(SAMPLE_HOUSES[i][0], SAMPLE_HOUSES[i][1], SAMPLE_HOUSES[i][2],
                    SAMPLE_HOUSES[i][3]);
            houseCardsPane.getChildren().add(card);
        }
        System.out.println("Loaded " + SAMPLE_HOUSES.length + " house cards");
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
            Scene scene = new Scene(root);
            stage.setScene(scene);
            DataStore.applyWindowSize(stage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showSignUpDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Account");
        dialog.setHeaderText("Join Project To-Let");

        ButtonType registerBtnType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerBtnType, ButtonType.CANCEL);

        VBox box = new VBox(10);
        TextField nameIn = new TextField();
        nameIn.setPromptText("Username");
        TextField emailIn = new TextField();
        emailIn.setPromptText("Email or Phone");
        PasswordField passIn = new PasswordField();
        passIn.setPromptText("Password");
        ComboBox<String> roleIn = new ComboBox<>();
        roleIn.getItems().addAll("Tenant", "Owner");
        roleIn.setValue("Tenant");

        box.getChildren().addAll(new Label("Username:"), nameIn, new Label("Email/Phone:"), emailIn,
                new Label("Password:"), passIn, new Label("Role (Tenant/Owner):"), roleIn);
        dialog.getDialogPane().setContent(box);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == registerBtnType) {
            String username = nameIn.getText().trim();
            String email = emailIn.getText().trim();
            String password = passIn.getText().trim();
            String role = roleIn.getValue();
            if ("Owner".equalsIgnoreCase(role)) {
                role = "House Owner";
            }

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("All fields are required!");
                alert.showAndWait();
                return;
            }

            // Create User object and register
            UserService userService = new UserService();
            User user = new User(username, email, password, role, "");
            boolean registered = userService.registerUser(user);

            if (registered) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Account created successfully! You can now login.");
                alert.showAndWait();

                // Navigate to login
                navigateToLogin();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText(null);
                alert.setContentText("Registration failed. Please try again.");
                alert.showAndWait();
            }
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
            Scene scene = new Scene(root);
            stage.setScene(scene);
            DataStore.applyWindowSize(stage);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
