package com.tolet;

import database.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SplashController {
    private static final String SPLASH_IMAGE_KEY = "default_splash";
    private static final Path SPLASH_IMAGE_FILE = Paths.get("pics", "openingn.png");

    @FXML
    private ImageView splashImage;
    @FXML
    private StackPane rootPane;
    @FXML
    private Label titleLabel;
    @FXML
    private ToggleButton themeToggle;

    @FXML
    public void initialize() {
        loadSplashImageFromDatabase();

        if (rootPane != null && splashImage != null) {
            splashImage.fitWidthProperty().bind(rootPane.widthProperty());
            splashImage.fitHeightProperty().bind(rootPane.heightProperty());
            splashImage.setPreserveRatio(false);
        }

        // Keep the text from FXML so UI edits show without code changes.

        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }
    }

    private void loadSplashImageFromDatabase() {
        byte[] imageBytes = fetchSplashImageBytes();
        if (imageBytes == null || imageBytes.length == 0) {
            seedSplashImageIfMissing();
            imageBytes = fetchSplashImageBytes();
        }

        if (imageBytes != null && imageBytes.length > 0) {
            splashImage.setImage(new Image(new ByteArrayInputStream(imageBytes)));
        }
    }

    private byte[] fetchSplashImageBytes() {
        String sql = "SELECT image_data FROM app_images WHERE image_key = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.connect()) {
            ensureAppImagesTable(conn);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, SPLASH_IMAGE_KEY);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getBytes("image_data");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void seedSplashImageIfMissing() {
        if (!Files.exists(SPLASH_IMAGE_FILE)) {
            return;
        }

        String sql = "INSERT OR REPLACE INTO app_images (image_key, image_data, updated_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseConnection.connect()) {
            ensureAppImagesTable(conn);
            byte[] bytes = Files.readAllBytes(SPLASH_IMAGE_FILE);
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, SPLASH_IMAGE_KEY);
                pstmt.setBytes(2, bytes);
                pstmt.executeUpdate();
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private void ensureAppImagesTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS app_images ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "image_key TEXT NOT NULL UNIQUE,"
                + "image_data BLOB NOT NULL,"
                + "created_at TEXT DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at TEXT DEFAULT CURRENT_TIMESTAMP)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.execute();
        }
    }

    @FXML
    private void onContinue() throws IOException {
        Stage stage = (Stage) splashImage.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource(
                DataStore.resolveFxml("Overview.fxml")));
        switchSceneRoot(stage, root);
    }

    @FXML
    private void onThemeToggle() {
        if (themeToggle == null) {
            return;
        }
        DataStore.darkMode = themeToggle.isSelected();
        try {
            Parent root = FXMLLoader.load(getClass().getResource(
                    DataStore.resolveFxml("splash-view.fxml")));
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
