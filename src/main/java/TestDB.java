import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import database.DatabaseConnection;
import database.TableCreator;

public class TestDB {

    public static void main(String[] args) throws SQLException {

        TableCreator.createTables();

        Connection conn = DatabaseConnection.connect();

        if (conn != null) {
            System.out.println("Connection test PASSED ✅");
            printHouseSummary(conn);
            printReviewSummary(conn);
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

    private static void printReviewSummary(Connection conn) {
        try (PreparedStatement countStmt = conn.prepareStatement("SELECT COUNT(*) AS total FROM house_reviews");
                ResultSet countRs = countStmt.executeQuery()) {
            if (countRs.next()) {
                System.out.println("Total reviews in DB: " + countRs.getInt("total"));
            }
        } catch (SQLException e) {
            System.out.println("Could not read review count: " + e.getMessage());
        }

        String reviewSql = "SELECT hr.id, COALESCE(u.name, '-') AS tenant_name, "
                + "COALESCE(h.location, '-') AS location, COALESCE(hr.status, '-') AS status, "
                + "COALESCE(hr.updated_at, hr.created_at, '-') AS review_date "
                + "FROM house_reviews hr "
                + "LEFT JOIN users u ON u.id = hr.tenant_id "
                + "LEFT JOIN houses h ON h.id = hr.house_id "
                + "ORDER BY hr.id DESC LIMIT 8";
        try (PreparedStatement stmt = conn.prepareStatement(reviewSql);
                ResultSet rs = stmt.executeQuery()) {
            System.out.println("Latest reviews:");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.println("#" + rs.getInt("id")
                        + " | Tenant: " + rs.getString("tenant_name")
                        + " | House: " + rs.getString("location")
                        + " | Status: " + rs.getString("status")
                        + " | Date: " + rs.getString("review_date"));
            }
            if (!any) {
                System.out.println("(none)");
            }
        } catch (SQLException e) {
            System.out.println("Could not read latest reviews: " + e.getMessage());
        }

        String eligibleSql = "SELECT u.name AS tenant_name, COUNT(DISTINCT r.house_id) AS eligible_houses "
                + "FROM rent_requests r "
                + "JOIN users u ON u.id = r.tenant_id "
                + "WHERE lower(trim(COALESCE(r.status, ''))) = 'approved' "
                + "AND date(COALESCE(NULLIF(TRIM(r.accepted_at), ''), r.request_date)) <= date('now', '-1 month') "
                + "GROUP BY r.tenant_id ORDER BY eligible_houses DESC LIMIT 8";
        try (PreparedStatement stmt = conn.prepareStatement(eligibleSql);
                ResultSet rs = stmt.executeQuery()) {
            System.out.println("Eligible houses per tenant (approved for >= 1 month):");
            boolean any = false;
            while (rs.next()) {
                any = true;
                System.out.println(rs.getString("tenant_name") + " -> " + rs.getInt("eligible_houses"));
            }
            if (!any) {
                System.out.println("(none)");
            }
        } catch (SQLException e) {
            System.out.println("Could not read eligibility summary: " + e.getMessage());
        }
    }
}