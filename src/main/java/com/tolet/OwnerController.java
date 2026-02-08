package com.tolet;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class OwnerController {
    @FXML private TextField locField, rentField;
    @FXML private ComboBox<String> typeBox;
    @FXML private TableView<House> houseTable;
    @FXML private TableColumn<House, String> colLoc, colType;
    @FXML private TableColumn<House, Double> colRent;

    @FXML
    public void initialize() {
        typeBox.getItems().addAll("Family", "Bachelor (M)", "Bachelor (F)", "Office");
        
        // Bind columns to House class
        colLoc.setCellValueFactory(new PropertyValueFactory<>("location"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colRent.setCellValueFactory(new PropertyValueFactory<>("rent"));
        
        // Show all houses (In real app, filter by logged in owner)
        houseTable.setItems(DataStore.houses);
    }

    @FXML
    protected void onAddListing() {
        try {
            String loc = locField.getText();
            String type = typeBox.getValue();
            double rent = Double.parseDouble(rentField.getText());
            
            House newHouse = new House(loc, type, rent, DataStore.currentUser.getUsername());
            DataStore.houses.add(newHouse);
            
            locField.clear(); rentField.clear();
        } catch (Exception e) {
            System.out.println("Invalid Input");
        }
    }
}