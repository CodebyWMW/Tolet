package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // private static final String URL = "jdbc:sqlite:Data/rental.db";
    private static final String URL =
        "jdbc:sqlite:C:/Users/User/OneDrive/Desktop/HOmerental/Data/rental.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC"); // Ensure driver loads
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found!");
            e.printStackTrace();
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}