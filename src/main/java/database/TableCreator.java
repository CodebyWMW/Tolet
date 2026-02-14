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

        String requestTable = "CREATE TABLE IF NOT EXISTS rent_requests ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "house_id INTEGER,"
                + "tenant_id INTEGER,"
                + "request_date TEXT,"
                + "move_in_date TEXT,"
                + "status TEXT,"
                + "FOREIGN KEY(house_id) REFERENCES houses(id),"
                + "FOREIGN KEY(tenant_id) REFERENCES users(id))";

        try (Connection conn = DatabaseConnection.connect();
                Statement stmt = conn.createStatement()) {
            stmt.execute(userTable);
            stmt.execute(houseTable);
            stmt.execute(requestTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}