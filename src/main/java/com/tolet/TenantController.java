package com.tolet;

import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class TenantController {
    @FXML private TextField searchField;
    @FXML private CheckBox familyCheck, bachelorCheck;
    @FXML private TableView<House> searchTable;
    @FXML private TableColumn<House, String> tLoc, tType;
    @FXML private TableColumn<House, Double> tRent;

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
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return house.getLocation().toLowerCase().contains(lowerCaseFilter);
            });
        });
    }
    
    @FXML
    protected void onBookRequest() {
        House selected = searchTable.getSelectionModel().getSelectedItem();
        if(selected != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Booking");
            alert.setHeaderText("Request Sent!");
            alert.setContentText("Owner " + selected.getOwnerName() + " has been notified.");
            alert.showAndWait();
        }
    }
}