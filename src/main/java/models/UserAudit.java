package models;

public class UserAudit {
    private final int userId;
    private final String name;
    private final String email;
    private final String phone;
    private final String role;
    private final String deletedAt;
    private final String deletedBy;

    public UserAudit(int userId, String name, String email, String phone, String role, String deletedAt,
            String deletedBy) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getRole() {
        return role;
    }

    public String getDeletedAt() {
        return deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }
}
