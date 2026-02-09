package database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

//    private static final String URL = "jdbc:sqlite:Data/rental.db";
private static final String URL =
        "jdbc:sqlite:C:/Users/User/OneDrive/Desktop/HOmerental/Tolet/Data/rental.db";

    public static Connection connect() {
        try {
            Connection conn = DriverManager.getConnection(URL);
            System.out.println("✅ Database connected successfully");
            return conn;
        } catch (Exception e) {
            System.out.println("❌ Database connection failed");
            e.printStackTrace();
            return null;
        }
    }
}
