package com.tolet;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class StatusPopupHelper {
    private StatusPopupHelper() {
    }

    public static boolean showStatusPopup(Stage ownerStage, String message, boolean error) {
        if (message == null || message.isBlank()) {
            return true;
        }

        try {
            FXMLLoader loader = new FXMLLoader(StatusPopupHelper.class.getResource(
                    DataStore.resolveFxml("status-popup.fxml")));
            Parent root = loader.load();

            Label titleLabel = (Label) root.lookup("#popupTitleLabel");
            Label messageLabel = (Label) root.lookup("#popupMessageLabel");
            Label iconLabel = (Label) root.lookup("#popupIconLabel");
            Button okButton = (Button) root.lookup("#popupOkButton");

            boolean dark = DataStore.darkMode;

            if (titleLabel != null) {
                titleLabel.setStyle(error
                        ? (dark
                                ? "-fx-text-fill: #ffc9c9; -fx-font-size: 20; -fx-font-weight: 800;"
                                : "-fx-text-fill: #8f1f2d; -fx-font-size: 20; -fx-font-weight: 800;")
                        : (dark
                                ? "-fx-text-fill: #d8ffea; -fx-font-size: 20; -fx-font-weight: 800;"
                                : "-fx-text-fill: #114f3b; -fx-font-size: 20; -fx-font-weight: 800;"));
                titleLabel.setText(error ? "Error" : "Success");
            }

            if (messageLabel != null) {
                messageLabel.setText(message);
            }

            if (iconLabel != null) {
                iconLabel.setText(error ? "!" : "✓");
                iconLabel.setStyle(error
                        ? (dark
                                ? "-fx-background-color: #6b2028; -fx-text-fill: #ffe2e2; -fx-font-size: 22; -fx-font-weight: 800; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40; -fx-background-radius: 999;"
                                : "-fx-background-color: #ffe3e6; -fx-text-fill: #9d1b2a; -fx-font-size: 22; -fx-font-weight: 800; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40; -fx-background-radius: 999;")
                        : (dark
                                ? "-fx-background-color: #1e5d42; -fx-text-fill: #ddffed; -fx-font-size: 22; -fx-font-weight: 800; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40; -fx-background-radius: 999;"
                                : "-fx-background-color: #ddf8ec; -fx-text-fill: #136c4a; -fx-font-size: 22; -fx-font-weight: 800; -fx-alignment: center; -fx-min-width: 40; -fx-min-height: 40; -fx-max-width: 40; -fx-max-height: 40; -fx-background-radius: 999;"));
            }

            if (okButton != null) {
                okButton.setStyle(error
                        ? (dark
                                ? "-fx-background-color: linear-gradient(to right, #c53f53, #e35d74); -fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 10 24 10 24; -fx-cursor: hand;"
                                : "-fx-background-color: linear-gradient(to right, #d73f5b, #eb5e7a); -fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 10 24 10 24; -fx-cursor: hand;")
                        : (dark
                                ? "-fx-background-color: linear-gradient(to right, #2fa964, #49c978); -fx-text-fill: #0c1e17; -fx-font-size: 13; -fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 10 24 10 24; -fx-cursor: hand;"
                                : "-fx-background-color: linear-gradient(to right, #159a62, #26bd7a); -fx-text-fill: #ffffff; -fx-font-size: 13; -fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 10 24 10 24; -fx-cursor: hand;"));
            }

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);
            popupStage.initModality(Modality.APPLICATION_MODAL);
            if (ownerStage != null) {
                popupStage.initOwner(ownerStage);
            }

            Rectangle clip = new Rectangle();
            clip.setArcWidth(40);
            clip.setArcHeight(40);
            clip.widthProperty().bind(root.layoutBoundsProperty().map(bounds -> bounds.getWidth()));
            clip.heightProperty().bind(root.layoutBoundsProperty().map(bounds -> bounds.getHeight()));
            root.setClip(clip);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);

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

            if (okButton != null) {
                okButton.setOnAction(e -> popupStage.close());
            }

            popupStage.showAndWait();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}