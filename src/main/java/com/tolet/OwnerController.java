package com.tolet;

import javafx.application.Platform;
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

public class OwnerController {
    @FXML private TextField locField, rentField;
    @FXML private ComboBox<String> typeBox;
    @FXML private TableView<House> houseTable;
    @FXML private TableColumn<House, String> colLoc, colType;
    @FXML private TableColumn<House, Double> colRent;
    @FXML private ToggleButton themeToggle;

    @FXML
    public void initialize() {
        typeBox.getItems().addAll("Family", "Bachelor (M)", "Bachelor (F)", "Office");

        colLoc.setCellValueFactory(new PropertyValueFactory<>("location"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colRent.setCellValueFactory(new PropertyValueFactory<>("rent"));

        // FIXED: Load from DB instead of static list
        loadHouseData();

        if (themeToggle != null) themeToggle.setSelected(DataStore.darkMode);
        Platform.runLater(() -> DataStore.applyTheme(houseTable.getScene()));
    }

    private void loadHouseData() {
        houseTable.setItems(DataStore.getHouses());
    }

    @FXML
    protected void onAddListing() {
        try {
            String loc = locField.getText();
            String type = typeBox.getValue();
            double rent = Double.parseDouble(rentField.getText());

            // FIXED: Save to DB
            DataStore.addHouse(loc, type, rent);
            
            // Refresh Table
            loadHouseData();

            locField.clear();
            rentField.clear();
        } catch (Exception e) {
            System.out.println("Invalid Input");
        }
    }

    @FXML private void onBack(ActionEvent event) throws IOException { switchToLogin(event); }
    @FXML private void onLogout(ActionEvent event) throws IOException { DataStore.currentUser = null; switchToLogin(event); }

    private void switchToLogin(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("login-view.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Scene scene = new Scene(root);
        DataStore.applyTheme(scene);
        stage.setScene(scene);
        stage.show();
    }

    @FXML private void onThemeToggle() {
        DataStore.darkMode = themeToggle.isSelected();
        DataStore.applyTheme(themeToggle.getScene());
    }
}