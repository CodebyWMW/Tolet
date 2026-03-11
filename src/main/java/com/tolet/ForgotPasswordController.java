package com.tolet;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;
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

    private String generatedOTP;
    private String userEmail;
    private int currentStep = 1; // 1 = email, 2 = otp, 3 = password

    @FXML
    public void initialize() {
        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }
        updateUI();
    }

    @FXML
    private void onSendOTP(ActionEvent event) {
        userEmail = emailField.getText().trim();

        if (userEmail.isEmpty()) {
            setStatus("Please enter your email.", false);
            return;
        }

        if (!DataStore.emailExists(userEmail)) {
            setStatus("Email not found in our system.", false);
            return;
        }

        // Generate and "send" OTP
        generatedOTP = String.valueOf((int) (Math.random() * 9000) + 1000);
        System.out.println("DEBUG OTP for " + userEmail + ": " + generatedOTP);

        currentStep = 2;
        setStatus("OTP sent to " + userEmail, true);
        updateUI();
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

        if (newPassword.length() < 6) {
            setStatus("Password must be at least 6 characters.", false);
            return;
        }

        DataStore.updatePassword(userEmail, newPassword);
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
        // Reset visibility
        emailField.setVisible(false);
        emailField.setManaged(false);
        otpField.setVisible(false);
        otpField.setManaged(false);
        newPasswordField.setVisible(false);
        newPasswordField.setManaged(false);
        confirmPasswordField.setVisible(false);
        confirmPasswordField.setManaged(false);

        switch (currentStep) {
            case 1:
                stepLabel.setText("Reset Your Password");
                emailField.setVisible(true);
                emailField.setManaged(true);
                break;
            case 2:
                stepLabel.setText("Enter OTP");
                otpField.setVisible(true);
                otpField.setManaged(true);
                break;
            case 3:
                stepLabel.setText("Set New Password");
                newPasswordField.setVisible(true);
                newPasswordField.setManaged(true);
                confirmPasswordField.setVisible(true);
                confirmPasswordField.setManaged(true);
                break;
        }
    }

    private void navigate(ActionEvent event, String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            DataStore.applyWindowSize(stage);
            stage.show();
        } catch (IOException e) {
            setStatus("Could not navigate.", false);
            e.printStackTrace();
        }
    }

    private void setStatus(String text, boolean success) {
        statusLabel.setText(text);
        statusLabel.setStyle(success ? "-fx-text-fill: #7dff9b;" : "-fx-text-fill: #ff8a8a;");
    }
}
