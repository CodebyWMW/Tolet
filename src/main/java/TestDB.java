import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import database.DatabaseConnection;

public class TestDB {

    public static void main(String[] args) throws SQLException {

        Connection conn = DatabaseConnection.connect();

        if (conn != null) {
            System.out.println("Connection test PASSED ✅");
            printHouseSummary(conn);
        } else {
            System.out.println("Connection test FAILED ❌");
        }
    }

    private static void printHouseSummary(Connection conn) {
        try (PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) AS total FROM houses");
                ResultSet countRs = countStmt.executeQuery()) {
            if (countRs.next()) {
                System.out.println("Total houses in DB: " + countRs.getInt("total"));
            }
        } catch (SQLException e) {
            System.out.println("Could not read house count: " + e.getMessage());
        }

        String sql = "SELECT id, COALESCE(title, '-') AS title, COALESCE(location, '-') AS location, "
                + "COALESCE(approval_status, '-') AS approval_status "
                + "FROM houses ORDER BY id DESC LIMIT 8";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            System.out.println("Latest houses:");
            while (rs.next()) {
                System.out.println("#" + rs.getInt("id")
                        + " | " + rs.getString("title")
                        + " | " + rs.getString("location")
                        + " | " + rs.getString("approval_status"));
            }
        } catch (SQLException e) {
            System.out.println("Could not read latest houses: " + e.getMessage());
        }
    }
}