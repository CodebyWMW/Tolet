package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;

import dao.HouseDAO;
import dao.UserDAO;
import database.DatabaseConnection;
import models.House;
import models.User;
public class ClientHandler implements Runnable {

    private final Socket socket;
    private Integer currentUserId; // session tracking

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())); PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true);) {

            HouseDAO houseDAO = new HouseDAO();
            UserDAO userDAO = new UserDAO();
            String command;

            while ((command = in.readLine()) != null) {
                if (command.isBlank()) {
                    out.println("UNKNOWN_COMMAND");
                    continue;
                }

                String[] parts = command.split("\\|");
                String action = parts[0];

                switch (action) {
                    case "GET_ALL_HOUSES" -> {
                        List<House> houses = houseDAO.getAllHouses();

                        if (houses.isEmpty()) {
                            out.println("NO_HOUSES");
                        } else {
                            for (House h : houses) {
                                out.println(h.getId() + " " + h.getLocation());
                            }
                        }
                        out.println("END");
                    }

                    case "GET_APPROVED_HOUSES" -> {
                        List<House> houses = houseDAO.getAllHouses();
                        if (houses.isEmpty()) {
                            out.println("NO_HOUSES");
                        } else {
                            for (House h : houses) {
                                String safeTitle = h.getTitle() == null ? "" : h.getTitle().replace("|", " ");
                                String safeLocation = h.getLocation() == null ? "" : h.getLocation().replace("|", " ");
                                out.println(h.getId() + "|" + safeTitle + "|" + safeLocation + "|" + h.getRent());
                            }
                        }
                        out.println("END");
                    }

                    case "GET_OWNER_HOUSES" -> {
                        if (parts.length < 2) {
                            out.println("ERROR:INVALID_FORMAT");
                            out.println("END");
                            continue;
                        }

                        int ownerId;
                        try {
                            ownerId = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ex) {
                            out.println("ERROR:INVALID_OWNER_ID");
                            out.println("END");
                            continue;
                        }

                        List<House> houses = houseDAO.getHousesByOwnerId(ownerId);
                        if (houses.isEmpty()) {
                            out.println("NO_HOUSES");
                        } else {
                            for (House h : houses) {
                                out.println(h.getId() + "|" + h.getLocation() + "|" + h.getApprovalStatus());
                            }
                        }
                        out.println("END");
                    }

                    case "LOGIN" -> {
                        if (parts.length < 3) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }

                        String email = parts[1];
                        String password = parts[2];
                        User user = userDAO.loginUser(email, password);
                        if (user == null) {
                            out.println("LOGIN_FAILED");
                        } else {
                            currentUserId = user.getId();
                            out.println("LOGIN_SUCCESS|" + user.getId() + "|" + user.getRole());
                        }
                    }

                    case "ADD_HOUSE" -> {

                        if (currentUserId == null) {
                            out.println("ERROR:LOGIN_REQUIRED");
                            continue;
                        }

                        if (parts.length < 3) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }

                        String location = parts[1];
                        int rent;
                        try {
                            rent = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException ex) {
                            out.println("ERROR:INVALID_RENT");
                            continue;
                        }

                        House house = new House();
                        house.setOwnerId(currentUserId);
                        house.setLocation(location);
                        house.setTitle("House Listing");
                        house.setRent(rent);
                        boolean added = houseDAO.addHouse(house);

                        out.println(added ? "SUCCESS" : "FAILED");
                    }

                    case "SIGNUP" -> {
                        if (parts.length < 6) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }

                        String name = parts[1];
                        String email = parts[2];
                        String password = parts[3];
                        String role = parts[4];
                        String phone = parts[5];
                        String birthdate = parts.length >= 7 ? parts[6] : null;

                        // Validate that required fields are not empty
                        if (name == null || name.trim().isEmpty()) {
                            out.println("ERROR:Username_is_required");
                            continue;
                        }
                        if (password == null || password.trim().isEmpty()) {
                            out.println("ERROR:Password_is_required");
                            continue;
                        }
                        if (role == null || role.trim().isEmpty()) {
                            out.println("ERROR:Role_is_required");
                            continue;
                        }
                        
                        // Either email OR phone is required, not both
                        boolean emailProvided = email != null && !email.trim().isEmpty();
                        boolean phoneProvided = phone != null && !phone.trim().isEmpty();
                        if (!emailProvided && !phoneProvided) {
                            out.println("ERROR:Email_or_phone_is_required");
                            continue;
                        }

                        // Check for duplicate username
                        if (userDAO.usernameExists(name)) {
                            out.println("ERROR:Username_already_exists");
                            continue;
                        }

                        // Check for duplicate email (only if email was provided)
                        if (emailProvided && userDAO.emailExists(email)) {
                            out.println("ERROR:Email_already_exists");
                            continue;
                        }

                        // Check for duplicate phone (only if phone was provided)
                        if (phoneProvided && userDAO.phoneExists(phone)) {
                            out.println("ERROR:Phone_number_already_exists");
                            continue;
                        }

                        System.out.println("SIGNUP: Processing registration for email=" + email + ", name=" + name + ", role=" + role);
                        User user = new User(name, email, password, role, phone, birthdate);
                        
                        try {
                            boolean success = userDAO.registerUser(user);

                            if (success) {
                                System.out.println("SIGNUP: Registration successful for " + email);
                                out.println("SUCCESS");
                            } else {
                                System.out.println("SIGNUP: Registration failed for " + email + " (DAO returned false)");
                                out.println("ERROR:Database_insertion_failed");
                            }
                        } catch (Exception e) {
                            System.err.println("SIGNUP: Exception during registration for " + email + ": " + e.getMessage());
                            e.printStackTrace();
                            out.println("ERROR:Registration_error_" + e.getClass().getSimpleName());
                        }
                    }

                    case "CHECK_USERNAME_EXISTS" -> {
                        if (parts.length < 2) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }
                        out.println(userDAO.usernameExists(parts[1]) ? "EXISTS" : "NOT_FOUND");
                    }

                    case "CHECK_EMAIL_EXISTS" -> {
                        if (parts.length < 2) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }
                        out.println(userDAO.emailExists(parts[1]) ? "EXISTS" : "NOT_FOUND");
                    }

                    case "CHECK_PHONE_EXISTS" -> {
                        if (parts.length < 2) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }
                        out.println(userDAO.phoneExists(parts[1]) ? "EXISTS" : "NOT_FOUND");
                    }

                    case "CHECK_CONTACT_EXISTS" -> {
                        if (parts.length < 2) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }
                        out.println(userDAO.contactExists(parts[1]) ? "EXISTS" : "NOT_FOUND");
                    }

                    case "UPDATE_PASSWORD_BY_CONTACT" -> {
                        if (parts.length < 3) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }
                        boolean updated = userDAO.updatePasswordByContact(parts[1], parts[2]);
                        out.println(updated ? "SUCCESS" : "FAILED");
                    }

                    case "RESOLVE_USER_ID" -> {
                        if (parts.length < 3) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }

                        String email = decode(parts[1]);
                        String username = decode(parts[2]);
                        int userId = resolveUserId(email, username);
                        if (userId > 0) {
                            out.println("FOUND|" + userId);
                        } else {
                            out.println("NOT_FOUND");
                        }
                    }

                    case "GET_USER_PROFILE" -> {
                        if (parts.length < 4) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }

                        int userId;
                        try {
                            userId = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ex) {
                            out.println("ERROR:INVALID_USER_ID");
                            continue;
                        }

                        String fallbackName = decode(parts[2]);
                        String fallbackEmail = decode(parts[3]);
                        String profile = loadUserProfile(userId, fallbackName, fallbackEmail);
                        out.println(profile == null ? "NOT_FOUND" : profile);
                    }

                    case "UPDATE_USER_PROFILE" -> {
                        if (parts.length < 8) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }

                        int userId;
                        try {
                            userId = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException ex) {
                            out.println("ERROR:INVALID_USER_ID");
                            continue;
                        }

                        String name = decode(parts[2]);
                        String email = decode(parts[3]);
                        String phone = decode(parts[4]);
                        String birthdate = decode(parts[5]);
                        String profileImage = decode(parts[6]);
                        String password = decode(parts[7]);

                        String updateResult = updateUserProfile(userId, name, email, phone, birthdate, profileImage, password);
                        out.println(updateResult);
                    }

                    case "CREATE_OWNER_HOUSE" -> {
                        if (parts.length < 19) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }

                        try {
                            int ownerId = Integer.parseInt(parts[1]);
                            String shortDetail = decode(parts[2]);
                            String location = decode(parts[3]);
                            String details = decode(parts[4]);
                            String tags = decode(parts[5]);
                            String availability = decode(parts[6]);
                            String type = decode(parts[7]);
                            int beds = Integer.parseInt(parts[8]);
                            int baths = Integer.parseInt(parts[9]);
                            double area = Double.parseDouble(parts[10]);
                            int gas = Integer.parseInt(parts[11]);
                            int water = Integer.parseInt(parts[12]);
                            int current = Integer.parseInt(parts[13]);
                            double rent = Double.parseDouble(parts[14]);
                            String contact = decode(parts[15]);
                            int family = Integer.parseInt(parts[16]);
                            int bachelor = Integer.parseInt(parts[17]);
                            String title = decode(parts[18]);

                            int houseId = createOwnerHouse(ownerId, shortDetail, location, details, tags, availability, type,
                                    beds, baths, area, gas, water, current, rent, contact, family, bachelor, title);
                            if (houseId > 0) {
                                out.println("SUCCESS|" + houseId);
                            } else {
                                out.println("FAILED");
                            }
                        } catch (NumberFormatException ex) {
                            out.println("ERROR:INVALID_NUMBER");
                        }
                    }

                    case "ADD_HOUSE_IMAGE" -> {
                        if (parts.length < 5) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }

                        try {
                            int houseId = Integer.parseInt(parts[1]);
                            String imageName = decode(parts[2]);
                            int sortOrder = Integer.parseInt(parts[3]);
                            byte[] imageData = Base64.getDecoder().decode(parts[4]);

                            boolean saved = addHouseImage(houseId, imageName, sortOrder, imageData);
                            out.println(saved ? "SUCCESS" : "FAILED");
                        } catch (IllegalArgumentException ex) {
                            out.println("ERROR:INVALID_IMAGE_DATA");
                        }
                    }

                    case "SET_HOUSE_COVER" -> {
                        if (parts.length < 3) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }

                        try {
                            int houseId = Integer.parseInt(parts[1]);
                            String cover = decode(parts[2]);
                            boolean updated = updateHouseCover(houseId, cover);
                            out.println(updated ? "SUCCESS" : "FAILED");
                        } catch (NumberFormatException ex) {
                            out.println("ERROR:INVALID_HOUSE_ID");
                        }
                    }

                    case "GET_OWNER_HOUSE_DETAILS" -> {
                        if (parts.length < 3) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }

                        try {
                            int houseId = Integer.parseInt(parts[1]);
                            int ownerId = Integer.parseInt(parts[2]);
                            String details = getOwnerHouseDetails(houseId, ownerId);
                            out.println(details == null ? "NOT_FOUND" : details);
                        } catch (NumberFormatException ex) {
                            out.println("ERROR:INVALID_NUMBER");
                        }
                    }

                    case "GET_HOUSE_IMAGES" -> {
                        if (parts.length < 2) {
                            out.println("ERROR:INVALID_FORMAT");
                            out.println("END");
                            continue;
                        }

                        try {
                            int houseId = Integer.parseInt(parts[1]);
                            List<String> images = getHouseImagesBase64(houseId);
                            if (images.isEmpty()) {
                                out.println("NO_IMAGES");
                            } else {
                                for (String line : images) {
                                    out.println(line);
                                }
                            }
                            out.println("END");
                        } catch (NumberFormatException ex) {
                            out.println("ERROR:INVALID_HOUSE_ID");
                            out.println("END");
                        }
                    }

                    case "CLEAR_HOUSE_IMAGES" -> {
                        if (parts.length < 2) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }
                        try {
                            int houseId = Integer.parseInt(parts[1]);
                            out.println(clearHouseImages(houseId) ? "SUCCESS" : "FAILED");
                        } catch (NumberFormatException ex) {
                            out.println("ERROR:INVALID_HOUSE_ID");
                        }
                    }

                    case "UPDATE_OWNER_HOUSE" -> {
                        if (parts.length < 20) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }

                        try {
                            int houseId = Integer.parseInt(parts[1]);
                            int ownerId = Integer.parseInt(parts[2]);
                            String title = decode(parts[3]);
                            String shortDetail = decode(parts[4]);
                            String location = decode(parts[5]);
                            String details = decode(parts[6]);
                            String tags = decode(parts[7]);
                            String availability = decode(parts[8]);
                            String type = decode(parts[9]);
                            int bedrooms = Integer.parseInt(parts[10]);
                            int bathrooms = Integer.parseInt(parts[11]);
                            double area = Double.parseDouble(parts[12]);
                            boolean gas = "1".equals(parts[13]);
                            boolean water = "1".equals(parts[14]);
                            boolean current = "1".equals(parts[15]);
                            double rent = Double.parseDouble(parts[16]);
                            String contact = decode(parts[17]);
                            boolean familyAllowed = "1".equals(parts[18]);
                            boolean bachelorAllowed = "1".equals(parts[19]);

                            boolean updated = houseDAO.updateHouseDetails(houseId, ownerId, title, shortDetail, location,
                                    details, tags, availability, type, bedrooms, bathrooms, area, gas, water, current,
                                    rent, contact, familyAllowed, bachelorAllowed);
                            out.println(updated ? "SUCCESS" : "FAILED");
                        } catch (NumberFormatException ex) {
                            out.println("ERROR:INVALID_NUMBER");
                        }
                    }

                    case "DELETE_OWNER_HOUSE" -> {
                        if (parts.length < 3) {
                            out.println("ERROR:INVALID_FORMAT");
                            continue;
                        }
                        try {
                            int houseId = Integer.parseInt(parts[1]);
                            int ownerId = Integer.parseInt(parts[2]);
                            boolean deleted = houseDAO.deleteHouseById(houseId, ownerId);
                            out.println(deleted ? "SUCCESS" : "FAILED");
                        } catch (NumberFormatException ex) {
                            out.println("ERROR:INVALID_NUMBER");
                        }
                    }

                    case "EXIT" -> {
                        socket.close();
                        return;
                    }

                    default -> out.println("UNKNOWN_COMMAND");
                }
            }

        } catch (Exception e) {
            System.err.println("Client handler error: " + e.getMessage());
        }
    }

    private String decode(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("<NL>", "\n").trim();
    }

    private int resolveUserId(String email, String username) {
        String sql = "SELECT id FROM users WHERE lower(ifnull(email, '')) = lower(?) OR name = ? COLLATE NOCASE LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email == null ? "" : email);
            pstmt.setString(2, username == null ? "" : username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException ignored) {
            return -1;
        }

        return -1;
    }

    private String loadUserProfile(int userId, String fallbackName, String fallbackEmail) {
        String queryById = "SELECT id, name, email, phone, role, birthdate, verified, profile_image, password FROM users WHERE id = ? LIMIT 1";
        String queryByIdentity = "SELECT id, name, email, phone, role, birthdate, verified, profile_image, password FROM users WHERE name = ? COLLATE NOCASE OR lower(ifnull(email, '')) = lower(?) LIMIT 1";

        try (Connection conn = DatabaseConnection.connect()) {
            ResultSet rs;
            PreparedStatement pstmt;
            if (userId > 0) {
                pstmt = conn.prepareStatement(queryById);
                pstmt.setInt(1, userId);
            } else {
                pstmt = conn.prepareStatement(queryByIdentity);
                pstmt.setString(1, fallbackName);
                pstmt.setString(2, fallbackEmail);
            }

            try (PreparedStatement statement = pstmt) {
                rs = statement.executeQuery();
                if (!rs.next()) {
                    return null;
                }

                return "FOUND|"
                        + rs.getInt("id") + "|"
                        + safeOut(rs.getString("name")) + "|"
                        + safeOut(rs.getString("email")) + "|"
                        + safeOut(rs.getString("phone")) + "|"
                        + safeOut(rs.getString("role")) + "|"
                        + safeOut(rs.getString("birthdate")) + "|"
                        + (rs.getInt("verified") == 1 ? "1" : "0") + "|"
                        + safeOut(rs.getString("profile_image")) + "|"
                        + safeOut(rs.getString("password"));
            }
        } catch (SQLException ignored) {
            return null;
        }
    }

    private String updateUserProfile(int userId, String name, String email, String phone, String birthdate,
            String profileImage, String password) {
        String nameCheck = "SELECT 1 FROM users WHERE id <> ? AND name = ? COLLATE NOCASE LIMIT 1";
        String emailCheck = "SELECT 1 FROM users WHERE id <> ? AND lower(ifnull(email, '')) = lower(?) LIMIT 1";
        String phoneCheck = "SELECT 1 FROM users WHERE id <> ? AND ifnull(phone, '') = ? LIMIT 1";
        String update = "UPDATE users SET name = ?, email = ?, phone = ?, birthdate = ?, profile_image = ?, password = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.connect()) {
            if (exists(conn, nameCheck, userId, name)) {
                return "ERROR:DUPLICATE_NAME";
            }
            if (email != null && !email.isBlank() && exists(conn, emailCheck, userId, email)) {
                return "ERROR:DUPLICATE_EMAIL";
            }
            if (phone != null && !phone.isBlank() && exists(conn, phoneCheck, userId, phone)) {
                return "ERROR:DUPLICATE_PHONE";
            }

            try (PreparedStatement pstmt = conn.prepareStatement(update)) {
                pstmt.setString(1, name == null || name.isBlank() ? null : name);
                pstmt.setString(2, email == null || email.isBlank() ? null : email);
                pstmt.setString(3, phone == null || phone.isBlank() ? null : phone);
                pstmt.setString(4, birthdate == null || birthdate.isBlank() ? null : birthdate);
                pstmt.setString(5, profileImage == null || profileImage.isBlank() ? null : profileImage);
                pstmt.setString(6, password == null ? "" : password);
                pstmt.setInt(7, userId);
                return pstmt.executeUpdate() > 0 ? "SUCCESS" : "FAILED";
            }
        } catch (SQLException ignored) {
            return "FAILED";
        }
    }

    private boolean exists(Connection conn, String sql, int userId, String value) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, value == null ? "" : value);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int createOwnerHouse(int ownerId, String shortDetail, String location, String details, String tags,
            String availability, String type, int beds, int baths, double area, int gas, int water, int current,
            double rent, String contact, int family, int bachelor, String title) {
        String sql = "INSERT INTO houses (title, short_detail, location, details, tags, availability, type, bedrooms, bathrooms, area, gas_available, water_available, current_available, rent, contact_info, owner_id, image, approval_status, family_allowed, bachelor_allowed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.setString(2, shortDetail);
            pstmt.setString(3, location);
            pstmt.setString(4, details);
            pstmt.setString(5, tags);
            pstmt.setString(6, availability);
            pstmt.setString(7, type);
            pstmt.setInt(8, beds);
            pstmt.setInt(9, baths);
            pstmt.setDouble(10, area);
            pstmt.setInt(11, gas);
            pstmt.setInt(12, water);
            pstmt.setInt(13, current);
            pstmt.setDouble(14, rent);
            pstmt.setString(15, contact);
            pstmt.setInt(16, ownerId);
            pstmt.setString(17, null);
            pstmt.setString(18, "pending");
            pstmt.setInt(19, family);
            pstmt.setInt(20, bachelor);
            pstmt.executeUpdate();

            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException ignored) {
            return -1;
        }

        return -1;
    }

    private boolean addHouseImage(int houseId, String imageName, int sortOrder, byte[] imageData) {
        String sql = "INSERT INTO house_images (house_id, image_name, image_data, sort_order) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, houseId);
            pstmt.setString(2, imageName);
            pstmt.setBytes(3, imageData);
            pstmt.setInt(4, sortOrder);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean updateHouseCover(int houseId, String cover) {
        String sql = "UPDATE houses SET image = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cover);
            pstmt.setInt(2, houseId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private String safeOut(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", " ").replace("\r", "").replace("\n", "<NL>");
    }

    private String getOwnerHouseDetails(int houseId, int ownerId) {
        String query = "SELECT id, owner_id, title, short_detail, location, details, tags, availability, type, bedrooms, bathrooms, area, gas_available, water_available, current_available, rent, contact_info, approval_status, family_allowed, bachelor_allowed FROM houses WHERE id = ? AND owner_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, houseId);
            pstmt.setInt(2, ownerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return "FOUND|"
                        + safeOut(rs.getString("title")) + "|"
                        + safeOut(rs.getString("short_detail")) + "|"
                        + safeOut(rs.getString("location")) + "|"
                        + safeOut(rs.getString("details")) + "|"
                        + safeOut(rs.getString("tags")) + "|"
                        + safeOut(rs.getString("availability")) + "|"
                        + safeOut(rs.getString("type")) + "|"
                        + rs.getInt("bedrooms") + "|"
                        + rs.getInt("bathrooms") + "|"
                        + rs.getDouble("area") + "|"
                        + rs.getDouble("rent") + "|"
                        + (rs.getInt("gas_available") == 1 ? "1" : "0") + "|"
                        + (rs.getInt("water_available") == 1 ? "1" : "0") + "|"
                        + (rs.getInt("current_available") == 1 ? "1" : "0") + "|"
                        + (rs.getInt("family_allowed") == 1 ? "1" : "0") + "|"
                        + (rs.getInt("bachelor_allowed") == 1 ? "1" : "0") + "|"
                        + safeOut(rs.getString("contact_info")) + "|"
                        + safeOut(rs.getString("approval_status"));
            }
        } catch (SQLException ignored) {
            return null;
        }
    }

    private List<String> getHouseImagesBase64(int houseId) {
        List<String> lines = new java.util.ArrayList<>();
        String sql = "SELECT image_name, image_data, sort_order FROM house_images WHERE house_id = ? ORDER BY sort_order ASC, id ASC LIMIT 3";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, houseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String name = safeOut(rs.getString("image_name"));
                    int order = rs.getInt("sort_order");
                    byte[] bytes = rs.getBytes("image_data");
                    if (bytes != null && bytes.length > 0) {
                        lines.add(name + "|" + order + "|" + Base64.getEncoder().encodeToString(bytes));
                    }
                }
            }
        } catch (SQLException ignored) {
            return lines;
        }

        return lines;
    }

    private boolean clearHouseImages(int houseId) {
        String sql = "DELETE FROM house_images WHERE house_id = ?";
        try (Connection conn = DatabaseConnection.connect();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, houseId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }
}
