package me.gauravbuilds.arcanelogin.database;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final ArcaneLogin plugin;
    private Connection connection;

    public DatabaseManager(ArcaneLogin plugin) {
        this.plugin = plugin;
    }

    public void initialize() throws SQLException {
        File dataFolder = new File(plugin.getDataFolder(), "arcanelogin.db");
        if (!dataFolder.getParentFile().exists()) {
            dataFolder.getParentFile().mkdirs();
        }
        
        try {
            if (!dataFolder.exists()) {
                dataFolder.createNewFile();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "File write error: arcanelogin.db");
        }

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }

        createTable();
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "username VARCHAR(16) NOT NULL," +
                "password_hash VARCHAR(255) NOT NULL," +
                "last_ip VARCHAR(45)," +
                "last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isRegistered(UUID uuid) {
        String sql = "SELECT 1 FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void registerUser(UUID uuid, String username, String passwordHash, String ip) {
        String sql = "INSERT INTO users(uuid, username, password_hash, last_ip, last_login) VALUES(?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, username);
            pstmt.setString(3, passwordHash);
            pstmt.setString(4, ip);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateLogin(UUID uuid, String ip) {
        String sql = "UPDATE users SET last_ip = ?, last_login = CURRENT_TIMESTAMP WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ip);
            pstmt.setString(2, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updatePassword(UUID uuid, String newHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newHash);
            pstmt.setString(2, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void updatePassword(String username, String newHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newHash);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getPasswordHash(UUID uuid) {
        String sql = "SELECT password_hash FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getLastIp(UUID uuid) {
        String sql = "SELECT last_ip FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("last_ip");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public boolean userExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
