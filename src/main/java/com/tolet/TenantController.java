package com.tolet;

import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.io.IOException;

public class TenantController {
    @FXML
    private TextField searchField;
    @FXML
    private CheckBox familyCheck, bachelorCheck;
    @FXML
    private TableView<House> searchTable;
    @FXML
    private TableColumn<House, String> tLoc, tType;
    @FXML
    private TableColumn<House, Double> tRent;
    @FXML
    private ToggleButton themeToggle;

    @FXML
    public void initialize() {
        tLoc.setCellValueFactory(new PropertyValueFactory<>("location"));
        tType.setCellValueFactory(new PropertyValueFactory<>("type"));
        tRent.setCellValueFactory(new PropertyValueFactory<>("rent"));

        // Wrap listing in a FilteredList
        FilteredList<House> filteredData = new FilteredList<>(DataStore.houses, p -> true);
        searchTable.setItems(filteredData);

        // Add Listener to Search Box
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(house -> {
                if (newValue == null || newValue.isEmpty())
                    return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return house.getLocation().toLowerCase().contains(lowerCaseFilter);
            });
        });

        if (themeToggle != null) {
            themeToggle.setSelected(DataStore.darkMode);
        }
        Platform.runLater(() -> DataStore.applyTheme(searchTable.getScene()));
    }

    @FXML
    protected void onBookRequest() {
        House selected = searchTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Booking");
            alert.setHeaderText("Request Sent!");
            alert.setContentText("Owner " + selected.getOwnerName() + " has been notified.");
            alert.showAndWait();
        }
    }

    @FXML
    private void onBack(ActionEvent event) throws IOException {
        switchToLogin(event);
    }

    @FXML
    private void onLogout(ActionEvent event) throws IOException {
        DataStore.currentUser = null;
        switchToLogin(event);
    }

    private void switchToLogin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("login-view.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        DataStore.applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void onThemeToggle() {
        DataStore.darkMode = themeToggle.isSelected();
        DataStore.applyTheme(themeToggle.getScene());
    }
}