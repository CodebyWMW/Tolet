package database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    // private static final String URL = "jdbc:sqlite:Data/rental.db";
    private static final String URL = "jdbc:sqlite:Data/rental.db";

    public static Connection connect() {
        try {
            return DriverManager.getConnection(URL);
        } catch (Exception e) {
            System.out.println("Database connection failed");
            e.printStackTrace();
            return null;
        }
    }
}