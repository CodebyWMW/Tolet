package dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import database.DatabaseConnection;
import models.House;

public class HouseDAO {

    public List<House> getAllHouses() {
        List<House> houses = new ArrayList<>();

        String sql = "SELECT * FROM houses";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                House house = new House();

                house.setId(rs.getInt("id"));
                house.setOwnerId(rs.getInt("owner_id"));
                house.setLocation(rs.getString("location"));
                house.setFamilyAllowed(rs.getInt("family_allowed") == 1);
                house.setBachelorAllowed(rs.getInt("bachelor_allowed") == 1);
                house.setGasAvailable(rs.getInt("gas_available") == 1);
                house.setParkingAvailable(rs.getInt("parking_available") == 1);
                house.setFurnished(rs.getInt("furnished") == 1);
                house.setPetFriendly(rs.getInt("pet_friendly") == 1);

                houses.add(house);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return houses;
    }
}