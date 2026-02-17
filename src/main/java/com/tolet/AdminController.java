package com.tolet;

import dao.HouseDAO;
import dao.UserDAO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.House;
import models.User;
import models.UserAudit;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdminController {
    @FXML
    private ToggleButton themeToggle;
    @FXML
    private Label adminNameLabel;

    // User Management
    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, String> userIdCol;
    @FXML
    private TableColumn<User, String> userNameCol;
    @FXML
    private TableColumn<User, String> userEmailCol;
    @FXML
    private TableColumn<User, String> userPhoneCol;
    @FXML
    private TableColumn<User, String> userRoleCol;
    @FXML
    private TableColumn<User, String> userVerifiedCol;
    @FXML
    private TableColumn<User, Void> userActionsCol;
    @FXML
    private TableColumn<User, Void> userDeleteCol;
    @FXML
    private ComboBox<String> roleFilterCombo;
    @FXML
    private Label userCountLabel;

    // House Listings
    @FXML
    private TableView<House> housesTable;
    @FXML
    private TableColumn<House, String> houseIdCol;
    @FXML
    private TableColumn<House, String> houseLocationCol;
    @FXML
    private TableColumn<House, String> houseOwnerIdCol;
    @FXML
    private TableColumn<House, String> houseFeaturesCol;
    @FXML
    private TableColumn<House, String> houseStatusCol;
    @FXML
    private TableColumn<House, Void> houseActionsCol;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private Label houseCountLabel;

    // Tenant Verification
    @FXML
    private TableView<User> tenantsTable;
    @FXML
    private TableColumn<User, String> tenantIdCol;
    @FXML
    private TableColumn<User, String> tenantNameCol;
    @FXML
    private TableColumn<User, String> tenantEmailCol;
    @FXML
    private TableColumn<User, String> tenantPhoneCol;
    @FXML
    private TableColumn<User, String> tenantVerifiedCol;
    @FXML
    private TableColumn<User, Void> tenantActionsCol;
    @FXML
    private Label unverifiedCountLabel;

    // Audit Log
    @FXML
    private TableView<UserAudit> auditTable;
    @FXML
    private TableColumn<UserAudit, String> auditUserIdCol;
    @FXML
    private TableColumn<UserAudit, String> auditNameCol;
    @FXML
    private TableColumn<UserAudit, String> auditEmailCol;
    @FXML
    private TableColumn<UserAudit, String> auditPhoneCol;
    @FXML
    private TableColumn<UserAudit, String> auditRoleCol;
    @FXML
    private TableColumn<UserAudit, String> auditDeletedAtCol;
    @FXML
    private TableColumn<UserAudit, String> auditDeletedByCol;

    // Statistics
    @FXML
    private Label statsTotalUsersLabel;
    @FXML
    private Label statsTotalHousesLabel;
    @FXML
    private Label statsPendingLabel;
    @FXML
    private Label statsOwnersLabel;
    @FXML
    private Label statsTenantsLabel;
    @FXML
    private Label statsUnverifiedLabel;

    private UserDAO userDAO;
    private HouseDAO houseDAO;
    private ObservableList<User> allUsers;
    private ObservableList<House> allHouses;

    @FXML
    public void initialize() {
        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }

        if (DataStore.currentUser != null) {
            adminNameLabel.setText(DataStore.currentUser.getUsername());
        }

        userDAO = new UserDAO();
        houseDAO = new HouseDAO();

        // Populate combo boxes
        roleFilterCombo.getItems().addAll("All", "owner", "tenant");
        roleFilterCombo.getSelectionModel().select(0);

        statusFilterCombo.getItems().addAll("All", "pending", "approved", "rejected");
        statusFilterCombo.getSelectionModel().select(0);

        setupUserManagement();
        setupHouseListings();
        setupTenantVerification();
        setupAuditLog();

        loadAllData();
        loadStatistics();
    }

    private void setupUserManagement() {
        userIdCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        userNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        userEmailCol.setCellValueFactory(data -> {
            String email = data.getValue().getEmail();
            return new SimpleStringProperty(email == null || email.isBlank() ? "N/A" : email);
        });
        userPhoneCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPhone() != null ? data.getValue().getPhone() : "N/A"));
        userRoleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        userVerifiedCol
                .setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isVerified() ? "✓ Yes" : "✗ No"));

        userActionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button verifyBtn = new Button("Verify");
            private final Button unverifyBtn = new Button("Unverify");
            private final HBox buttons = new HBox(8, verifyBtn, unverifyBtn);

            {
                buttons.setAlignment(Pos.CENTER);
                verifyBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand;");
                unverifyBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");

                verifyBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleUserVerification(user, true);
                });

                unverifyBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleUserVerification(user, false);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    verifyBtn.setDisable(user.isVerified());
                    unverifyBtn.setDisable(!user.isVerified());
                    setGraphic(buttons);
                }
            }
        });

        userDeleteCol.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");

            {
                deleteBtn.setStyle("-fx-background-color: #dc2626; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleUserDelete(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });

        roleFilterCombo.setOnAction(e -> filterUsers());
        roleFilterCombo.getSelectionModel().select(0);
    }

    private void setupHouseListings() {
        houseIdCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        houseLocationCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLocation()));
        houseOwnerIdCol
                .setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getOwnerId())));
        houseFeaturesCol.setCellValueFactory(data -> {
            House h = data.getValue();
            StringBuilder features = new StringBuilder();
            if (h.isFamilyAllowed())
                features.append("Family, ");
            if (h.isBachelorAllowed())
                features.append("Bachelor, ");
            if (h.isFurnished())
                features.append("Furnished, ");
            if (h.isParkingAvailable())
                features.append("Parking, ");
            if (h.isGasAvailable())
                features.append("Gas, ");
            if (h.isPetFriendly())
                features.append("Pet-friendly");
            String result = features.toString();
            if (result.endsWith(", "))
                result = result.substring(0, result.length() - 2);
            return new SimpleStringProperty(result.isEmpty() ? "None" : result);
        });
        houseStatusCol.setCellValueFactory(data -> {
            String status = data.getValue().getApprovalStatus();
            return new SimpleStringProperty(status != null ? status.toUpperCase() : "APPROVED");
        });

        houseActionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button approveBtn = new Button("Approve");
            private final Button rejectBtn = new Button("Reject");
            private final HBox buttons = new HBox(8, approveBtn, rejectBtn);

            {
                buttons.setAlignment(Pos.CENTER);
                approveBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand;");
                rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");

                approveBtn.setOnAction(e -> {
                    House house = getTableView().getItems().get(getIndex());
                    handleHouseApproval(house, "approved");
                });

                rejectBtn.setOnAction(e -> {
                    House house = getTableView().getItems().get(getIndex());
                    handleHouseApproval(house, "rejected");
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    House house = getTableView().getItems().get(getIndex());
                    String status = house.getApprovalStatus();
                    approveBtn.setDisable("approved".equals(status));
                    rejectBtn.setDisable("rejected".equals(status));
                    setGraphic(buttons);
                }
            }
        });

        statusFilterCombo.setOnAction(e -> filterHouses());
        statusFilterCombo.getSelectionModel().select(0);
    }

    private void setupTenantVerification() {
        tenantIdCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        tenantNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        tenantEmailCol.setCellValueFactory(data -> {
            String email = data.getValue().getEmail();
            return new SimpleStringProperty(email == null || email.isBlank() ? "N/A" : email);
        });
        tenantPhoneCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPhone() != null ? data.getValue().getPhone() : "N/A"));
        tenantVerifiedCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().isVerified() ? "✓ Verified" : "✗ Unverified"));

        tenantActionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button verifyBtn = new Button("Verify Tenant");

            {
                verifyBtn.setStyle(
                        "-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 12;");
                verifyBtn.setOnAction(e -> {
                    User tenant = getTableView().getItems().get(getIndex());
                    handleUserVerification(tenant, true);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User tenant = getTableView().getItems().get(getIndex());
                    verifyBtn.setDisable(tenant.isVerified());
                    setGraphic(verifyBtn);
                }
            }
        });
    }

    private void setupAuditLog() {
        auditUserIdCol.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getUserId())));
        auditNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        auditEmailCol.setCellValueFactory(data -> {
            String email = data.getValue().getEmail();
            return new SimpleStringProperty(email == null || email.isBlank() ? "N/A" : email);
        });
        auditPhoneCol.setCellValueFactory(data -> {
            String phone = data.getValue().getPhone();
            return new SimpleStringProperty(phone == null || phone.isBlank() ? "N/A" : phone);
        });
        auditRoleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        auditDeletedAtCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeletedAt()));
        auditDeletedByCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDeletedBy()));
    }

    private void loadAllData() {
        List<User> users = userDAO.getAllUsers();
        allUsers = FXCollections.observableArrayList(users);
        usersTable.setItems(allUsers);
        userCountLabel.setText("Total Users: " + users.size());

        List<House> houses = houseDAO.getAllHouses();
        allHouses = FXCollections.observableArrayList(houses);
        housesTable.setItems(allHouses);
        houseCountLabel.setText("Total Houses: " + houses.size());

        loadTenants();
        loadAuditLog();
    }

    private void loadAuditLog() {
        List<UserAudit> audit = userDAO.getUserAuditLog();
        auditTable.setItems(FXCollections.observableArrayList(audit));
    }

    private void loadTenants() {
        List<User> tenants = userDAO.getUsersByRole("tenant");
        tenantsTable.setItems(FXCollections.observableArrayList(tenants));
        long unverified = tenants.stream().filter(t -> !t.isVerified()).count();
        unverifiedCountLabel.setText("Unverified: " + unverified);
    }

    private void filterUsers() {
        String filter = roleFilterCombo.getValue();
        if (filter == null || filter.equals("All")) {
            usersTable.setItems(allUsers);
            userCountLabel.setText("Total Users: " + allUsers.size());
        } else {
            ObservableList<User> filtered = allUsers.filtered(u -> u.getRole().equals(filter));
            usersTable.setItems(filtered);
            userCountLabel.setText("Total Users: " + filtered.size());
        }
    }

    private void filterHouses() {
        String filter = statusFilterCombo.getValue();
        if (filter == null || filter.equals("All")) {
            housesTable.setItems(allHouses);
            houseCountLabel.setText("Total Houses: " + allHouses.size());
        } else {
            ObservableList<House> filtered = allHouses.filtered(h -> filter.equals(h.getApprovalStatus()));
            housesTable.setItems(filtered);
            houseCountLabel.setText("Total Houses: " + filtered.size());
        }
    }

    private void handleUserVerification(User user, boolean verify) {
        boolean success = userDAO.updateUserVerification(user.getId(), verify);
        if (success) {
            showAlert("Success", verify ? "User verified successfully!" : "User unverified!",
                    Alert.AlertType.INFORMATION);
            loadAllData();
            loadStatistics();
        } else {
            showAlert("Error", "Failed to update user verification status.", Alert.AlertType.ERROR);
        }
    }

    private void handleUserDelete(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete user permanently?");
        confirm.setContentText("This action cannot be undone.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        String deletedBy = DataStore.currentUser != null ? DataStore.currentUser.getUsername() : "System";
        boolean success = userDAO.deleteUserWithAudit(user.getId(), deletedBy);
        if (success) {
            showAlert("Success", "User deleted permanently.", Alert.AlertType.INFORMATION);
            loadAllData();
            loadStatistics();
        } else {
            showAlert("Error", "Failed to delete user.", Alert.AlertType.ERROR);
        }
    }

    private void handleHouseApproval(House house, String status) {
        boolean success = houseDAO.updateHouseStatus(house.getId(), status);
        if (success) {
            String message = status.equals("approved") ? "House listing approved!" : "House listing rejected!";
            showAlert("Success", message, Alert.AlertType.INFORMATION);
            loadAllData();
            loadStatistics();
        } else {
            showAlert("Error", "Failed to update house status.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void loadStatistics() {
        List<User> allUsersList = userDAO.getAllUsers();
        List<User> owners = userDAO.getUsersByRole("owner");
        List<User> tenants = userDAO.getUsersByRole("tenant");
        List<House> allHousesList = houseDAO.getAllHouses();
        List<House> pending = houseDAO.getHousesByStatus("pending");

        statsTotalUsersLabel.setText(String.valueOf(allUsersList.size()));
        statsTotalHousesLabel.setText(String.valueOf(allHousesList.size()));
        statsPendingLabel.setText(String.valueOf(pending.size()));
        statsOwnersLabel.setText(String.valueOf(owners.size()));
        statsTenantsLabel.setText(String.valueOf(tenants.size()));

        long unverified = allUsersList.stream().filter(u -> !u.isVerified()).count();
        statsUnverifiedLabel.setText(String.valueOf(unverified));
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void onLogout(ActionEvent event) throws IOException {
        if (showLogoutConfirmation()) {
            DataStore.currentUser = null;
            switchToLogin(event);
        }
    }

    private boolean showLogoutConfirmation() {
        try {
            AtomicBoolean confirmed = new AtomicBoolean(false);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("logout-popup.fxml"));
            Parent root = loader.load();

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.UNDECORATED);
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));

            // Make window draggable
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

            // Get buttons from FXML
            Button yesButton = (Button) root.lookup("#yesButton");
            Button cancelButton = (Button) root.lookup("#cancelButton");

            yesButton.setOnAction(e -> {
                confirmed.set(true);
                popupStage.close();
            });

            cancelButton.setOnAction(e -> popupStage.close());

            popupStage.showAndWait();
            return confirmed.get();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
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
            loadScene(stage, "admin-view-new.fxml");
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
