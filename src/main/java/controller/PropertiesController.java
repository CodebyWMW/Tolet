package controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import com.tolet.House;
import network.ClientConnection;
import com.tolet.DataStore;
import java.util.List;

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

            // send request to server
            ClientConnection.getOut().writeObject("GET_OWNER_HOUSES");
            ClientConnection.getOut().writeObject(DataStore.currentUser.getId());

            List<House> houses =
                    (List<House>) ClientConnection.getIn().readObject();

            propertyTable.getItems().setAll(houses);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}