package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DatabaseConnection {

    private static final Path DB_FILE = Paths.get("Data", "rental.db");
    private static final String URL = "jdbc:sqlite:" + DB_FILE.toString();

    static {
        try {
            Class.forName("org.sqlite.JDBC"); // Ensure driver loads

            // Ensure the database directory exists before opening SQLite file.
            Path parent = DB_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found!");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Failed to initialize database directory.");
            e.printStackTrace();
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}