package com.tolet;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;

public class SplashController {
    private static final String SPLASH_IMAGE_PATH = "D:/Academic/Project/To—•Let/Tolet/pics/opening.png";

    @FXML
    private ImageView splashImage;
    @FXML
    private Label titleLabel;

    @FXML
    public void initialize() {
        File imageFile = new File(SPLASH_IMAGE_PATH);
        if (imageFile.exists()) {
            splashImage.setImage(new Image(imageFile.toURI().toString()));
        }
        titleLabel.setText("Project To-•Let");
    }

    @FXML
    private void onContinue() throws IOException {
        Stage stage = (Stage) splashImage.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("login-view.fxml"));
        Scene scene = new Scene(root, 900, 600);
        DataStore.applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }
}
