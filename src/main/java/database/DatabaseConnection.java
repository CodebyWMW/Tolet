package database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final Path DB_FILE = resolveDbFile();
    private static final String URL = "jdbc:sqlite:" + DB_FILE.toString();

    static {
        try {
            Class.forName("org.sqlite.JDBC"); // Ensure driver loads

            // Ensure the database directory exists before opening SQLite file.
            Path parent = DB_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // Print exact DB file used to avoid confusion when multiple Data folders exist.
            System.out.println("[DatabaseConnection] Using SQLite DB: " + DB_FILE);
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

    private static Path resolveDbFile() {
        String configuredPath = System.getProperty("tolet.db.path");
        if (configuredPath == null || configuredPath.isBlank()) {
            configuredPath = System.getenv("TOLET_DB_PATH");
        }

        if (configuredPath != null && !configuredPath.isBlank()) {
            return Paths.get(configuredPath).toAbsolutePath().normalize();
        }

        return Paths.get("Data", "rental.db").toAbsolutePath().normalize();
    }
}