
package com.tolet;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.House;
import models.User;
import models.UserAudit;

public class AdminController {
    @FXML
    private javafx.scene.shape.Rectangle refreshFillRect;
    private final javafx.scene.shape.Rectangle refreshFillClip = new javafx.scene.shape.Rectangle();
    @FXML
    private Button refreshStatsButton;

    @FXML
    private void onRefreshStatisticsAnimated() {
        if (refreshStatsButton == null || refreshFillRect == null) {
            loadStatistics();
            return;
        }

        double buttonWidth = refreshStatsButton.getWidth();
        double buttonHeight = refreshStatsButton.getHeight();
        if (buttonWidth <= 0 || buttonHeight <= 0) {
            loadStatistics();
            return;
        }

        refreshFillRect.setWidth(buttonWidth);
        refreshFillRect.setHeight(buttonHeight);
        refreshFillRect.setArcWidth(12);
        refreshFillRect.setArcHeight(12);
        refreshFillRect.setOpacity(0.34);

        refreshFillClip.setWidth(0);
        refreshFillClip.setHeight(buttonHeight);
        refreshFillClip.setArcWidth(12);
        refreshFillClip.setArcHeight(12);
        refreshFillRect.setClip(refreshFillClip);

        javafx.animation.KeyFrame fillStart = new javafx.animation.KeyFrame(
                javafx.util.Duration.ZERO,
                new javafx.animation.KeyValue(refreshFillClip.widthProperty(), 0));
        javafx.animation.KeyFrame fillEnd = new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(520),
                new javafx.animation.KeyValue(refreshFillClip.widthProperty(), buttonWidth));
        javafx.animation.Timeline fillTimeline = new javafx.animation.Timeline(fillStart, fillEnd);

        fillTimeline.setOnFinished(e -> {
            loadStatistics();
            javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(
                    javafx.util.Duration.millis(260), refreshFillRect);
            fade.setFromValue(0.34);
            fade.setToValue(0);
            fade.setOnFinished(done -> refreshFillRect.setClip(null));
            fade.play();
        });
        fillTimeline.play();
    }
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
    private TableColumn<User, String> userBirthdateCol;
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
    private TableColumn<House, String> houseTitleCol;
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
    private TableColumn<User, String> tenantBirthdateCol;
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

        DataStore.normalizeAdminPublicIds();

        // Populate combo boxes
        roleFilterCombo.getItems().addAll("All", "owner", "tenant");
        roleFilterCombo.getSelectionModel().select(0);

        statusFilterCombo.getItems().addAll("All", "pending", "approved", "rejected");
        statusFilterCombo.getSelectionModel().select("pending");

        setupUserManagement();
        setupHouseListings();
        setupTenantVerification();
        setupAuditLog();

        loadAllData();
        loadStatistics();
    }

    private void setupUserManagement() {
        userIdCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayId()));
        userNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        userEmailCol.setCellValueFactory(data -> {
            String email = data.getValue().getEmail();
            return new SimpleStringProperty(email == null || email.isBlank() ? "N/A" : email);
        });
        userPhoneCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPhone() != null ? data.getValue().getPhone() : "N/A"));
        userBirthdateCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getBirthdate() != null && !data.getValue().getBirthdate().isBlank()
                ? data.getValue().getBirthdate()
                : "N/A"));
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
        houseTitleCol.setCellValueFactory(data -> {
            String title = data.getValue().getTitle();
            if (title == null || title.isBlank()) {
                return new SimpleStringProperty("Untitled House");
            }
            return new SimpleStringProperty(title);
        });
        houseLocationCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLocation()));
        houseOwnerIdCol
            .setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOwnerDisplayId()));
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
        tenantIdCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayId()));
        tenantNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        tenantEmailCol.setCellValueFactory(data -> {
            String email = data.getValue().getEmail();
            return new SimpleStringProperty(email == null || email.isBlank() ? "N/A" : email);
        });
        tenantPhoneCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPhone() != null ? data.getValue().getPhone() : "N/A"));
        tenantBirthdateCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getBirthdate() != null && !data.getValue().getBirthdate().isBlank()
                ? data.getValue().getBirthdate()
                : "N/A"));
        tenantVerifiedCol.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().isVerified() ? "✓ Verified" : "✗ Unverified"));

        tenantActionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button verifyBtn = new Button("Verify Tenant");
            private final Button rejectBtn = new Button("Reject Tenant");
            private final HBox buttons = new HBox(8, verifyBtn, rejectBtn);

            {
                buttons.setAlignment(Pos.CENTER);
                verifyBtn.setStyle(
                        "-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 12;");
                rejectBtn.setStyle(
                        "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 6 12;");

                verifyBtn.setOnAction(e -> {
                    User tenant = getTableView().getItems().get(getIndex());
                    handleUserVerification(tenant, true);
                });

                rejectBtn.setOnAction(e -> {
                    User tenant = getTableView().getItems().get(getIndex());
                    handleUserVerification(tenant, false);
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
                    rejectBtn.setDisable(!tenant.isVerified());
                    setGraphic(buttons);
                }
            }
        });
    }

    private void setupAuditLog() {
        auditUserIdCol.setCellValueFactory(
                data -> {
                    String publicId = data.getValue().getPublicId();
                    if (publicId != null && !publicId.isBlank()) {
                        return new SimpleStringProperty(publicId);
                    }
                    return new SimpleStringProperty(String.valueOf(data.getValue().getUserId()));
                });
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
        DataStore.normalizeAdminPublicIds();
        List<User> users = DataStore.adminGetAllUsers();
        allUsers = FXCollections.observableArrayList(users);
        usersTable.setItems(allUsers);
        userCountLabel.setText("Total Users: " + users.size());

        List<House> houses = DataStore.adminGetAllListingsForAdmin();
        allHouses = FXCollections.observableArrayList(houses);
        filterHouses();

        loadTenants();
        loadAuditLog();
    }

    private void loadAuditLog() {
        List<UserAudit> auditEntries = DataStore.adminGetAuditLog();
        auditTable.setItems(FXCollections.observableArrayList(auditEntries));
    }

    private void loadTenants() {
        List<User> users = DataStore.adminGetAllUsers();
        List<User> tenants = users.stream()
            .filter(u -> "tenant".equals(normalizeRoleFilter(u.getRole())))
            .toList();
        tenantsTable.setItems(FXCollections.observableArrayList(tenants));
        long unverified = tenants.stream().filter(t -> !t.isVerified()).count();
        unverifiedCountLabel.setText("Unverified: " + unverified);
    }

    private void filterUsers() {
        String filter = normalizeRoleFilter(roleFilterCombo.getValue());
        if (filter == null || filter.equals("all")) {
            usersTable.setItems(allUsers);
            userCountLabel.setText("Total Users: " + allUsers.size());
        } else {
            ObservableList<User> filtered = allUsers
                    .filtered(u -> normalizeRoleFilter(u.getRole()).equals(filter));
            usersTable.setItems(filtered);
            userCountLabel.setText("Total Users: " + filtered.size());
        }
    }

    private String normalizeRoleFilter(String role) {
        if (role == null || role.isBlank()) {
            return "all";
        }

        String normalized = role.trim().toLowerCase();
        if (normalized.equals("owner")
                || normalized.equals("house owner")
                || normalized.equals("bariwala")
                || normalized.equals("landlord")) {
            return "owner";
        }
        if (normalized.equals("tenant") || normalized.equals("varatia")) {
            return "tenant";
        }
        if (normalized.equals("all")) {
            return "all";
        }
        return normalized;
    }

    private void filterHouses() {
        if (allHouses == null) {
            housesTable.setItems(FXCollections.observableArrayList());
            houseCountLabel.setText("Total Houses: 0");
            return;
        }

        String filter = statusFilterCombo.getValue();
        if (filter == null || filter.equals("All")) {
            housesTable.setItems(allHouses);
            houseCountLabel.setText("Total Houses: " + allHouses.size());
        } else {
            ObservableList<House> filtered = allHouses.filtered(h -> {
                String status = h.getApprovalStatus() == null ? "" : h.getApprovalStatus();
                return filter.equalsIgnoreCase(status);
            });
            housesTable.setItems(filtered);
            houseCountLabel.setText("Total Houses: " + filtered.size());
        }
    }

    private void handleUserVerification(User user, boolean verify) {
        boolean success = DataStore.adminUpdateUserVerification(user.getId(), verify);
        if (success) {
            showStatusMessage(verify ? "User verified successfully!" : "User unverified!", false);
            loadAllData();
            loadStatistics();
        } else {
            showStatusMessage("Failed to update user verification status.", true);
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
        boolean success = DataStore.adminDeleteUserWithAudit(user.getId(), deletedBy);
        if (success) {
            showStatusMessage("User deleted permanently.", false);
            loadAllData();
            loadStatistics();
        } else {
            showStatusMessage("Failed to delete user.", true);
        }
    }

    private void handleHouseApproval(House house, String status) {
        boolean success = DataStore.adminUpdateHouseStatus(house.getId(), status);
        if (success) {
            String message = status.equals("approved") ? "House listing approved!" : "House listing rejected!";
            showStatusMessage(message, false);
            loadAllData();
            loadStatistics();
        } else {
            showStatusMessage("Failed to update house status.", true);
        }
    }

    @FXML
    private void loadStatistics() {
        List<User> allUsersList = DataStore.adminGetAllUsers();
        List<User> owners = DataStore.adminGetUsersByRole("owner");
        List<User> tenants = DataStore.adminGetUsersByRole("tenant");
        List<House> allHousesList = DataStore.adminGetAllListingsForAdmin();
        List<House> pending = DataStore.adminGetHousesByStatus("pending");

        statsTotalUsersLabel.setText(String.valueOf(allUsersList.size()));
        statsTotalHousesLabel.setText(String.valueOf(allHousesList.size()));
        statsPendingLabel.setText(String.valueOf(pending.size()));
        statsOwnersLabel.setText(String.valueOf(owners.size()));
        statsTenantsLabel.setText(String.valueOf(tenants.size()));

        long unverified = allUsersList.stream().filter(u -> !u.isVerified()).count();
        statsUnverifiedLabel.setText(String.valueOf(unverified));
    }

    private void showStatusMessage(String content, boolean error) {
        Stage ownerStage = getOwnerStage();
        if (!StatusPopupHelper.showStatusPopup(ownerStage, content, error)) {
            Alert fallbackAlert = new Alert(error ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
            fallbackAlert.setTitle(error ? "Error" : "Success");
            fallbackAlert.setHeaderText(null);
            fallbackAlert.setContentText(content);
            fallbackAlert.showAndWait();
        }
    }

    private Stage getOwnerStage() {
        if (adminNameLabel != null && adminNameLabel.getScene() != null) {
            return (Stage) adminNameLabel.getScene().getWindow();
        }
        if (usersTable != null && usersTable.getScene() != null) {
            return (Stage) usersTable.getScene().getWindow();
        }
        return null;
    }

    @FXML
    private void onLogout(ActionEvent event) throws IOException {
        if (showLogoutConfirmation()) {
            DataStore.currentUser = null;
            DataStore.clearRememberedSession();
            switchToLogin(event);
        }
    }

    private boolean showLogoutConfirmation() {
        try {
            AtomicBoolean confirmed = new AtomicBoolean(false);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(DataStore.resolveFxml("logout-popup.fxml")));
            Parent root = loader.load();

            Stage popupStage = new Stage();
            popupStage.initStyle(StageStyle.TRANSPARENT);
            popupStage.initModality(Modality.APPLICATION_MODAL);

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.setArcWidth(40);
            clip.setArcHeight(40);
            clip.widthProperty().bind(root.layoutBoundsProperty().map(bounds -> bounds.getWidth()));
            clip.heightProperty().bind(root.layoutBoundsProperty().map(bounds -> bounds.getHeight()));
            root.setClip(clip);

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            popupStage.setScene(scene);

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
