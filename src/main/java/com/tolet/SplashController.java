package com.tolet;

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
import javafx.application.Platform;
import java.io.File;
import java.io.IOException;

public class SplashController {
    private static final String SPLASH_IMAGE_PATH = "D:/Academic/Project/To—•Let/Tolet/pics/openingn.png";

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
        File imageFile = new File(SPLASH_IMAGE_PATH);
        if (imageFile.exists()) {
            splashImage.setImage(new Image(imageFile.toURI().toString()));
        }

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
