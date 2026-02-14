package com.tolet;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class OwnerController {
    @FXML
    private ToggleButton themeToggle;
    @FXML
    private Label profileNameLabel;
    @FXML
    private Label profileRoleLabel;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label kpiIncomeLabel;
    @FXML
    private Label kpiOccupancyLabel;
    @FXML
    private Label kpiPendingLabel;
    @FXML
    private VBox requestsContainer;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",
            Locale.ENGLISH);
    private static final DateTimeFormatter REQUEST_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy",
            Locale.ENGLISH);

    @FXML
    public void initialize() {
        if (themeToggle != null)
            themeToggle.setSelected(DataStore.darkMode);

        populateProfile();
        populateKpis();
        populateRequests();
    }

    private void populateProfile() {
        String ownerName = DataStore.currentUser != null ? DataStore.currentUser.getUsername() : "Owner";
        String role = DataStore.currentUser != null ? DataStore.currentUser.getRole() : "Owner";
        if (profileNameLabel != null)
            profileNameLabel.setText(ownerName);
        if (profileRoleLabel != null)
            profileRoleLabel.setText(role);
        if (welcomeLabel != null)
            welcomeLabel.setText("Welcome back, " + ownerName + "!");
        if (dateLabel != null)
            dateLabel.setText(LocalDate.now().format(DATE_FORMAT));
    }

    private void populateKpis() {
        String ownerName = DataStore.currentUser != null ? DataStore.currentUser.getUsername() : null;
        double income = 0;
        int houseCount = 0;
        for (House house : DataStore.getHouses()) {
            if (ownerName == null || ownerName.isBlank() || ownerName.equalsIgnoreCase(house.getOwnerName())) {
                income += house.getRent();
                houseCount++;
            }
        }

        int pendingCount = 0;
        int approvedCount = 0;
        for (BookingRequest request : DataStore.getBookingRequests()) {
            String status = request.getStatus();
            if (status != null) {
                if (status.equalsIgnoreCase("Pending"))
                    pendingCount++;
                if (status.equalsIgnoreCase("Approved"))
                    approvedCount++;
            }
        }

        int occupancy = 0;
        if (houseCount > 0) {
            occupancy = (int) Math.min(100, Math.round((approvedCount * 100.0) / houseCount));
        }

        if (kpiIncomeLabel != null)
            kpiIncomeLabel.setText(formatCurrencyCompact(income));
        if (kpiPendingLabel != null)
            kpiPendingLabel.setText(String.valueOf(pendingCount));
        if (kpiOccupancyLabel != null)
            kpiOccupancyLabel.setText(occupancy + "%");
    }

    private void populateRequests() {
        if (requestsContainer == null)
            return;
        requestsContainer.getChildren().clear();
        for (BookingRequest request : DataStore.getBookingRequests()) {
            requestsContainer.getChildren().add(buildRequestRow(request));
        }
    }

    private HBox buildRequestRow(BookingRequest request) {
        HBox row = new HBox(16);
        row.getStyleClass().add("table-row");

        Label tenant = buildCellLabel(request.getTenantName());
        Label property = buildCellLabel(request.getProperty());
        Label requestDate = buildCellLabel(formatDate(request.getRequestDate()));
        Label moveInDate = buildCellLabel(formatDate(request.getMoveInDate()));
        Label rent = buildCellLabel(formatRent(request.getMonthlyRent()));

        Label status = new Label(normalizeStatus(request.getStatus()));
        status.getStyleClass().add("status-pill");
        status.getStyleClass().add(normalizeStatus(request.getStatus()).toLowerCase(Locale.ENGLISH));

        Button approve = new Button("Approve");
        approve.getStyleClass().add("btn-approve");
        Button deny = new Button("Deny");
        deny.getStyleClass().add("btn-deny");

        HBox actions = new HBox(8, approve, deny);

        row.getChildren().addAll(tenant, property, requestDate, moveInDate, rent, status, actions);
        return row;
    }

    private Label buildCellLabel(String text) {
        Label label = new Label(text == null ? "" : text);
        label.getStyleClass().add("table-text");
        return label;
    }

    private String formatDate(LocalDate date) {
        if (date == null)
            return "";
        return date.format(REQUEST_DATE_FORMAT);
    }

    private String formatRent(double rent) {
        return "৳" + String.format("%,.0f", rent);
    }

    private String formatCurrencyCompact(double amount) {
        if (amount >= 1000) {
            double value = amount / 1000.0;
            String formatted = value >= 10 ? String.format("%.0f", value) : String.format("%.1f", value);
            return "৳" + formatted + "k";
        }
        return "৳" + String.format("%.0f", amount);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank())
            return "Pending";
        String value = status.trim().toLowerCase(Locale.ENGLISH);
        if (value.startsWith("approve"))
            return "Approved";
        if (value.startsWith("deny") || value.startsWith("reject"))
            return "Denied";
        return "Pending";
    }

    @FXML
    private void onLogout(ActionEvent event) throws IOException {
        DataStore.currentUser = null;
        switchToLogin(event);
    }

    private void switchToLogin(ActionEvent event) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        loadScene(stage, "login-view.fxml");
    }

    @FXML
    private void onThemeToggle() {
        if (themeToggle == null) {
            return;
        }
        DataStore.darkMode = themeToggle.isSelected();
        try {
            Stage stage = (Stage) themeToggle.getScene().getWindow();
            loadScene(stage, "owner-view.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadScene(Stage stage, String baseFxml) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(
                DataStore.resolveFxml(baseFxml)));
        Scene scene = new Scene(root);
        stage.setScene(scene);
        DataStore.applyWindowSize(stage);
        stage.show();
    }
}