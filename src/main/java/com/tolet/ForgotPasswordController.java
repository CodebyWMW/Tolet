package com.tolet;

import java.io.IOException;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ForgotPasswordController {
    private static final double FORGOT_WINDOW_WIDTH = 400;
    private static final double FORGOT_WINDOW_HEIGHT = 900;
    private static final int OTP_COOLDOWN_SECONDS = 60;


    @FXML
    private TextField emailField;
    @FXML
    private Button sendOtpButton;
    @FXML
    private TextField otpField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label statusLabel;
    @FXML
    private Label stepLabel;
    @FXML
    private ToggleButton themeToggle;
    @FXML
    private VBox emailStep;
    @FXML
    private VBox otpStep;
    @FXML
    private VBox passwordStep;

    private String generatedOTP;
    private String userContact;
    private int currentStep = 1; // 1 = email, 2 = otp, 3 = password
    private long otpCooldownEndsAtMillis;
    private Timeline otpCooldownTimeline;

    @FXML
    public void initialize() {
        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }

        // Match signup behavior: keep a fixed width and controlled minimum height.
        emailField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                if (newScene.getWindow() instanceof Stage existingStage) {
                    applyForgotWindowLock(existingStage);
                }
                newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                    if (newWin instanceof Stage stg) {
                        applyForgotWindowLock(stg);
                    }
                });
            }
        });

        updateUI();
        updateSendOtpButtonState();
    }

    @FXML
    private void onSendOTP(ActionEvent event) {
        long now = System.currentTimeMillis();
        if (now < otpCooldownEndsAtMillis) {
            long remainingSeconds = Math.max(1, (otpCooldownEndsAtMillis - now + 999) / 1000);
            setStatus("Please wait " + remainingSeconds + " seconds before requesting OTP again.", false);
            return;
        }

        userContact = emailField.getText().trim();

        if (userContact.isEmpty()) {
            setStatus("Please enter your email or phone number.", false);
            return;
        }

        if (!DataStore.contactExists(userContact)) {
            setStatus("Email or phone number not found in our system.", false);
            return;
        }

        // Generate and "send" OTP
        generatedOTP = String.valueOf((int) (Math.random() * 9000) + 1000);
        System.out.println("DEBUG OTP for " + userContact + ": " + generatedOTP);

        currentStep = 2;
        setStatus("OTP sent to " + userContact, true);
        updateUI();
        startOtpCooldown();
    }

    private void startOtpCooldown() {
        otpCooldownEndsAtMillis = System.currentTimeMillis() + (OTP_COOLDOWN_SECONDS * 1000L);

        if (otpCooldownTimeline != null) {
            otpCooldownTimeline.stop();
        }

        otpCooldownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateSendOtpButtonState()));
        otpCooldownTimeline.setCycleCount(Timeline.INDEFINITE);
        otpCooldownTimeline.play();

        updateSendOtpButtonState();
    }

    private void updateSendOtpButtonState() {
        if (sendOtpButton == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long remainingSeconds = Math.max(0, (otpCooldownEndsAtMillis - now + 999) / 1000);

        if (remainingSeconds > 0) {
            sendOtpButton.setDisable(true);
            sendOtpButton.setText("Resend OTP (" + remainingSeconds + "s)");
        } else {
            sendOtpButton.setDisable(false);
            sendOtpButton.setText("Send OTP");
            if (otpCooldownTimeline != null) {
                otpCooldownTimeline.stop();
                otpCooldownTimeline = null;
            }
        }
    }

    @FXML
    private void onVerifyOTP(ActionEvent event) {
        String enteredOTP = otpField.getText().trim();

        if (enteredOTP.isEmpty()) {
            setStatus("Please enter the OTP.", false);
            return;
        }

        if (!enteredOTP.equals(generatedOTP)) {
            setStatus("Invalid OTP. Try again.", false);
            return;
        }

        currentStep = 3;
        setStatus("OTP verified. Enter your new password.", true);
        updateUI();
    }

    @FXML
    private void onResetPassword(ActionEvent event) {
        if (currentStep < 3) {
            setStatus("Please verify OTP first.", false);
            return;
        }

        String newPassword = newPasswordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            setStatus("Please fill all password fields.", false);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            setStatus("Passwords do not match.", false);
            return;
        }

        boolean updated = DataStore.updatePasswordByContact(userContact, newPassword);
        if (!updated) {
            setStatus("Could not reset password for that email/phone. Please try again.", false);
            return;
        }
        setStatus("Password reset successfully! Redirecting to login...", true);

        // Navigate back to login after 1.5 seconds
        javafx.application.Platform.runLater(() -> {
            try {
                Thread.sleep(1500);
                navigate(event, DataStore.resolveFxml("login-view.fxml"));
            } catch (InterruptedException e) {
                navigate(event, DataStore.resolveFxml("login-view.fxml"));
            }
        });
    }

    @FXML
    private void onBackToLogin(ActionEvent event) {
        navigate(event, DataStore.resolveFxml("login-view.fxml"));
    }

    @FXML
    private void onThemeToggle(ActionEvent event) {
        if (themeToggle == null) {
            return;
        }
        DataStore.darkMode = themeToggle.isSelected();
        navigate(event, DataStore.resolveFxml("forgot-password-view.fxml"));
    }

    private void updateUI() {
        // Keep contact input visible across all steps so users can clearly see where OTP/password fields appear.
        setStepVisible(emailStep, true);
        setStepVisible(otpStep, false);
        setStepVisible(passwordStep, false);

        switch (currentStep) {
            case 1:
                stepLabel.setText("Reset Your Password");
                break;
            case 2:
                stepLabel.setText("Enter OTP");
                setStepVisible(otpStep, true);
                break;
            case 3:
                stepLabel.setText("Set New Password");
                setStepVisible(otpStep, true);
                setStepVisible(passwordStep, true);
                break;
        }
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
            if (!fxml.contains("forgot-password-view")) {
                clearForgotWindowLock(stage);
            }
            if (fxml.contains("forgot-password-view")) {
                applyForgotWindowLock(stage);
            }
            stage.show();
        } catch (IOException e) {
            setStatus("Could not navigate.", false);
            e.printStackTrace();
        }
    }

    private void setStatus(String text, boolean success) {
        if (success) {
            statusLabel.setText(text);
            statusLabel.setStyle("-fx-text-fill: #7dff9b;");
            return;
        }

        // Error messages are shown via popup only, so clear inline status text.
        statusLabel.setText("");
        statusLabel.setStyle("-fx-text-fill: #ff8a8a;");

        {
            Stage ownerStage = resolveOwnerStage();
            StatusPopupHelper.showStatusPopup(ownerStage, text, true);
        }
    }

    private Stage resolveOwnerStage() {
        if (themeToggle != null && themeToggle.getScene() != null
                && themeToggle.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        if (statusLabel != null && statusLabel.getScene() != null
                && statusLabel.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    private void applyForgotWindowLock(Stage stage) {
        stage.setWidth(FORGOT_WINDOW_WIDTH);
        stage.setHeight(FORGOT_WINDOW_HEIGHT);
        // Lock width only, allow vertical resize above a minimum.
        stage.setMinWidth(FORGOT_WINDOW_WIDTH);
        stage.setMaxWidth(FORGOT_WINDOW_WIDTH);
        stage.setMinHeight(FORGOT_WINDOW_HEIGHT);
        stage.setMaxHeight(Double.MAX_VALUE);
        stage.setResizable(true);

        Platform.runLater(() -> {
            stage.setMinWidth(FORGOT_WINDOW_WIDTH);
            stage.setMaxWidth(FORGOT_WINDOW_WIDTH);
            stage.setMinHeight(FORGOT_WINDOW_HEIGHT);
            stage.setMaxHeight(Double.MAX_VALUE);
            stage.setResizable(true);
        });
    }

    private void clearForgotWindowLock(Stage stage) {
        stage.setMinWidth(0);
        stage.setMinHeight(0);
        stage.setMaxWidth(Double.MAX_VALUE);
        stage.setMaxHeight(Double.MAX_VALUE);
        stage.setResizable(true);
    }

    private void setStepVisible(VBox step, boolean visible) {
        if (step != null) {
            step.setVisible(visible);
            step.setManaged(visible);
        }
    }
}
