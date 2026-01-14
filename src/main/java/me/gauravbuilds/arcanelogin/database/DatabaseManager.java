package me.gauravbuilds.arcanelogin.database;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

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
        // Added columns: is_premium, and logout location data
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "username VARCHAR(16) NOT NULL," +
                "password_hash VARCHAR(255)," + // Can be NULL if unregistered
                "last_ip VARCHAR(45)," +
                "is_premium BOOLEAN DEFAULT 0," +
                "loc_world VARCHAR(50)," +
                "loc_x DOUBLE," +
                "loc_y DOUBLE," +
                "loc_z DOUBLE," +
                "loc_yaw FLOAT," +
                "loc_pitch FLOAT," +
                "last_login TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }

        // Quick hack: Try to add columns if they don't exist (for existing DBs)
        // In a real production plugin, you'd use versioning. For now, we ignore errors.
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE users ADD COLUMN is_premium BOOLEAN DEFAULT 0;");
        } catch (SQLException ignored) {}
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE users ADD COLUMN loc_world VARCHAR(50);");
            stmt.execute("ALTER TABLE users ADD COLUMN loc_x DOUBLE;");
            stmt.execute("ALTER TABLE users ADD COLUMN loc_y DOUBLE;");
            stmt.execute("ALTER TABLE users ADD COLUMN loc_z DOUBLE;");
            stmt.execute("ALTER TABLE users ADD COLUMN loc_yaw FLOAT;");
            stmt.execute("ALTER TABLE users ADD COLUMN loc_pitch FLOAT;");
        } catch (SQLException ignored) {}
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

    // --- Core Checks ---

    public boolean isRegistered(UUID uuid) {
        String sql = "SELECT password_hash FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                // Registered only if they exist AND have a password hash (not null)
                return rs.next() && rs.getString("password_hash") != null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isPremium(UUID uuid) {
        String sql = "SELECT is_premium FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getBoolean("is_premium");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- Actions ---

    public void registerUser(UUID uuid, String username, String passwordHash, String ip) {
        // Using REPLACE INTO or INSERT ON CONFLICT to handle re-registering
        String sql = "INSERT OR REPLACE INTO users(uuid, username, password_hash, last_ip, is_premium, loc_world, loc_x, loc_y, loc_z, loc_yaw, loc_pitch, last_login) " +
                "VALUES(?, ?, ?, ?, COALESCE((SELECT is_premium FROM users WHERE uuid=?), 0), " +
                "COALESCE((SELECT loc_world FROM users WHERE uuid=?), NULL), " +
                "COALESCE((SELECT loc_x FROM users WHERE uuid=?), NULL), " +
                "COALESCE((SELECT loc_y FROM users WHERE uuid=?), NULL), " +
                "COALESCE((SELECT loc_z FROM users WHERE uuid=?), NULL), " +
                "COALESCE((SELECT loc_yaw FROM users WHERE uuid=?), NULL), " +
                "COALESCE((SELECT loc_pitch FROM users WHERE uuid=?), NULL), " +
                "CURRENT_TIMESTAMP)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, username);
            pstmt.setString(3, passwordHash);
            pstmt.setString(4, ip);
            // Self-referencing subqueries to preserve data are complex in prepared statements.
            // Simplified approach: Update if exists, Insert if new.
            // Actually, for SQLite:
            registerUserSimple(uuid, username, passwordHash, ip);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void registerUserSimple(UUID uuid, String username, String passwordHash, String ip) throws SQLException {
        // Update password if exists (Re-register), else Insert
        if (userExistsRaw(uuid)) {
            String sql = "UPDATE users SET password_hash = ?, last_ip = ?, username = ? WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, passwordHash);
                pstmt.setString(2, ip);
                pstmt.setString(3, username);
                pstmt.setString(4, uuid.toString());
                pstmt.executeUpdate();
            }
        } else {
            String sql = "INSERT INTO users(uuid, username, password_hash, last_ip) VALUES(?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, username);
                pstmt.setString(3, passwordHash);
                pstmt.setString(4, ip);
                pstmt.executeUpdate();
            }
        }
    }

    public void softDeleteUser(UUID uuid) {
        // "Unregister" -> Set password to NULL, but KEEP location and premium status
        String sql = "UPDATE users SET password_hash = NULL WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setPremium(String username, boolean status) {
        String sql = "UPDATE users SET is_premium = ? WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setBoolean(1, status);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveLocation(UUID uuid, Location loc) {
        String sql = "UPDATE users SET loc_world=?, loc_x=?, loc_y=?, loc_z=?, loc_yaw=?, loc_pitch=? WHERE uuid=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, loc.getWorld().getName());
            pstmt.setDouble(2, loc.getX());
            pstmt.setDouble(3, loc.getY());
            pstmt.setDouble(4, loc.getZ());
            pstmt.setFloat(5, loc.getYaw());
            pstmt.setFloat(6, loc.getPitch());
            pstmt.setString(7, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Location getLastLocation(UUID uuid) {
        String sql = "SELECT * FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getString("loc_world") != null) {
                    World w = Bukkit.getWorld(rs.getString("loc_world"));
                    if (w == null) return null; // World might be unloaded/deleted
                    return new Location(w,
                            rs.getDouble("loc_x"), rs.getDouble("loc_y"), rs.getDouble("loc_z"),
                            rs.getFloat("loc_yaw"), rs.getFloat("loc_pitch"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --- Utilities ---

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
                if (rs.next()) return rs.getString("password_hash");
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
                if (rs.next()) return rs.getString("last_ip");
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

    private boolean userExistsRaw(UUID uuid) {
        String sql = "SELECT 1 FROM users WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { return false; }
    }
}