package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.tolet.DataStore;
import com.tolet.House;

import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import network.ClientConnection;

public class PropertiesController {

    @FXML
    private TableView<House> propertyTable;

    @FXML
    private TableColumn<House, Integer> idColumn;

    @FXML
    private TableColumn<House, String> locationColumn;

    @FXML
    private TableColumn<House, String> statusColumn;

    public void initialize() {

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("approvalStatus"));

        loadOwnerProperties();
    }

    private void loadOwnerProperties() {

        try {
            if (DataStore.currentUser == null) {
                propertyTable.getItems().clear();
                return;
            }

            String command = "GET_OWNER_HOUSES|" + DataStore.currentUser.getId();
            List<String> rows = ClientConnection.sendCommandForLines(command, "END");

            List<House> houses = new ArrayList<>();
            for (String row : rows) {
                if ("NO_HOUSES".equals(row)) {
                    continue;
                }

                String[] parts = row.split("\\|", 3);
                if (parts.length < 3) {
                    continue;
                }

                int houseId;
                try {
                    houseId = Integer.parseInt(parts[0]);
                } catch (NumberFormatException ex) {
                    continue;
                }

                House house = new House(
                        houseId,
                        "",
                        parts[1],
                        "",
                        0.0,
                        "",
                        "",
                        0,
                        0,
                        0.0);
                house.setApprovalStatus(parts[2]);
                houses.add(house);
            }

            propertyTable.getItems().setAll(houses);

        } catch (IOException e) {
            propertyTable.getItems().clear();
        }

    }
}