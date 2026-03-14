package com.tolet;

import java.io.IOException;
import java.time.Year;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.application.Platform;
import javafx.stage.Stage;

public class SignupPageController {
    private static final double SIGNUP_WINDOW_WIDTH = 400;
    private static final double SIGNUP_WINDOW_HEIGHT = 900;

    @FXML
    private TextField emailField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ToggleButton tenantRoleButton;
    @FXML
    private ToggleButton ownerRoleButton;
    @FXML
    private ComboBox<String> monthCombo;
    @FXML
    private ComboBox<String> dayCombo;
    @FXML
    private ComboBox<String> yearCombo;
    @FXML
    private Label statusLabel;
    @FXML
    private ToggleButton themeToggle;

    private final ToggleGroup accountTypeGroup = new ToggleGroup();

    @FXML
    public void initialize() {
        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }

        // Force signup to a fixed window size regardless of opener page behavior.
        emailField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                if (newScene.getWindow() instanceof Stage existingStage) {
                    applySignupWindowLock(existingStage);
                }
                newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                    if (newWin instanceof Stage stg) {
                        applySignupWindowLock(stg);
                    }
                });
            }
        });

        configureRoleButtons();

        monthCombo.getItems().add("MM");
        for (int m = 1; m <= 12; m++) {
            monthCombo.getItems().add(String.format("%02d", m));
        }
        monthCombo.setValue("MM");

        dayCombo.getItems().add("DD");
        for (int d = 1; d <= 31; d++) {
            dayCombo.getItems().add(String.format("%02d", d));
        }
        dayCombo.setValue("DD");

        yearCombo.getItems().add("YYYY");
        int thisYear = Year.now().getValue();
        for (int y = thisYear; y >= 1950; y--) {
            yearCombo.getItems().add(String.valueOf(y));
        }
        yearCombo.setValue("YYYY");
    }

    @FXML
    private void onCreateAccount() {
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String selectedRole = getSelectedRole();
        String month = monthCombo.getValue();
        String day = dayCombo.getValue();
        String year = yearCombo.getValue();

        if (email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            setStatus("Please fill all required fields.", false);
            return;
        }

        if ("MM".equals(month) || "DD".equals(day) || "YYYY".equals(year)) {
            setStatus("Please select a valid date of birth.", false);
            return;
        }

        if (selectedRole == null || selectedRole.isBlank()) {
            setStatus("Please select account type.", false);
            return;
        }

        if (DataStore.usernameExists(username)) {
            setStatus("Username already taken.", false);
            return;
        }

        if (email.contains("@") && DataStore.emailExists(email)) {
            setStatus("Email already registered.", false);
            return;
        }

        if (!email.contains("@") && DataStore.phoneExists(email)) {
            setStatus("Phone already registered.", false);
            return;
        }

        String birthdate = year + "-" + month + "-" + day;
        boolean created = DataStore.registerUser(username, email, password, selectedRole, birthdate);
        if (created) {
            setStatus("Account created successfully. Redirecting to login...", true);
            try {
                Parent root = FXMLLoader.load(getClass().getResource(DataStore.resolveFxml("login-view.fxml")));
                Stage stage = (Stage) statusLabel.getScene().getWindow();
                Scene scene = stage.getScene();
                if (scene == null) {
                    stage.setScene(new Scene(root));
                } else {
                    DataStore.prepareSceneForRootSwap(scene);
                    scene.setRoot(root);
                }
                clearSignupWindowLock(stage);
                stage.show();
            } catch (IOException e) {
                setStatus("Could not open login page.", false);
                e.printStackTrace();
            }
        } else {
            setStatus("Registration failed. Try another email/username.", false);
        }
    }

    @FXML
    private void onBackToSignIn(ActionEvent event) {
        navigate(event, DataStore.resolveFxml("login-view.fxml"));
    }

    @FXML
    private void onThemeToggle(ActionEvent event) {
        if (themeToggle == null) {
            return;
        }
        DataStore.darkMode = themeToggle.isSelected();
        navigate(event, DataStore.resolveFxml("signup-view.fxml"));
    }

    private void navigate(ActionEvent event, String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root));
            } else {
                DataStore.prepareSceneForRootSwap(scene);
                scene.setRoot(root);
            }
            if (!fxml.contains("signup-view")) {
                clearSignupWindowLock(stage);
            }
            if (fxml.contains("signup-view")) {
                applySignupWindowLock(stage);
            }
            stage.show();
        } catch (IOException e) {
            setStatus("Could not open page.", false);
            e.printStackTrace();
        }
    }

    private void setStatus(String text, boolean success) {
        if (statusLabel != null) {
            statusLabel.setText("");
        }

        Stage ownerStage = null;
        if (themeToggle != null && themeToggle.getScene() != null) {
            ownerStage = (Stage) themeToggle.getScene().getWindow();
        } else if (statusLabel != null && statusLabel.getScene() != null) {
            ownerStage = (Stage) statusLabel.getScene().getWindow();
        }

        if (!StatusPopupHelper.showStatusPopup(ownerStage, text, !success) && statusLabel != null) {
            statusLabel.setText(text);
            statusLabel.setStyle(success ? "-fx-text-fill: #159a62;" : "-fx-text-fill: #d92d20;");
        }
    }

    private void configureRoleButtons() {
        tenantRoleButton.setToggleGroup(accountTypeGroup);
        ownerRoleButton.setToggleGroup(accountTypeGroup);
        tenantRoleButton.setSelected(true);
        accountTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                tenantRoleButton.setSelected(true);
            }
        });
    }

    private String getSelectedRole() {
        if (ownerRoleButton != null && ownerRoleButton.isSelected()) {
            return "House Owner";
        }
        if (tenantRoleButton != null && tenantRoleButton.isSelected()) {
            return "Tenant";
        }
        return null;
    }

    private void applySignupWindowLock(Stage stage) {
        stage.setWidth(SIGNUP_WINDOW_WIDTH);
        stage.setHeight(SIGNUP_WINDOW_HEIGHT);
        // Lock width only — height stays free for vertical resize
        stage.setMinWidth(SIGNUP_WINDOW_WIDTH);
        stage.setMaxWidth(SIGNUP_WINDOW_WIDTH);
        stage.setMinHeight(SIGNUP_WINDOW_HEIGHT);
        stage.setMaxHeight(Double.MAX_VALUE);
        stage.setResizable(true);

        Platform.runLater(() -> {
            stage.setMinWidth(SIGNUP_WINDOW_WIDTH);
            stage.setMaxWidth(SIGNUP_WINDOW_WIDTH);
            stage.setMinHeight(SIGNUP_WINDOW_HEIGHT);
            stage.setMaxHeight(Double.MAX_VALUE);
            stage.setResizable(true);
        });
    }

    private void clearSignupWindowLock(Stage stage) {
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        stage.setMaxWidth(Double.MAX_VALUE);
        stage.setMaxHeight(Double.MAX_VALUE);
        stage.setResizable(true);
    }
}
