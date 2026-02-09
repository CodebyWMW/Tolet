package database;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class TableCreator {
    public static void createTables() {
        String userTable = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT,"
                + "email TEXT UNIQUE,"
                + "phone TEXT,"
                + "password TEXT,"
                + "role TEXT)";

        // Updated houses table with new columns
        String houseTable = "CREATE TABLE IF NOT EXISTS houses ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "address TEXT,"
                + "city TEXT,"
                + "type TEXT,"
                + "rent REAL,"
                + "owner_id INTEGER,"
                + "image TEXT,"
                + "bedrooms INTEGER,"
                + "bathrooms INTEGER,"
                + "area REAL,"
                + "FOREIGN KEY(owner_id) REFERENCES users(id))";

        try (Connection conn = DatabaseConnection.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(userTable);
            stmt.execute(houseTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}