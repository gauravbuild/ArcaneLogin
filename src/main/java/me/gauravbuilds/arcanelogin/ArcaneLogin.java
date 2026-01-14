package me.gauravbuilds.arcanelogin;

import me.gauravbuilds.arcanelogin.commands.*;
import me.gauravbuilds.arcanelogin.database.DatabaseManager;
import me.gauravbuilds.arcanelogin.listeners.DevMechanicsListener;
import me.gauravbuilds.arcanelogin.listeners.PlayerListener;
import me.gauravbuilds.arcanelogin.managers.ConfigManager;
import me.gauravbuilds.arcanelogin.managers.SessionManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class ArcaneLogin extends JavaPlugin {

    private static ArcaneLogin instance;
    private DatabaseManager databaseManager;
    private SessionManager sessionManager;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize Managers
        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.sessionManager = new SessionManager(this);

        try {
            this.databaseManager.initialize();
        } catch (SQLException e) {
            getLogger().severe("Failed to initialize database! Disabling plugin...");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register Listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new DevMechanicsListener(), this);

        // Register Commands
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("changepassword").setExecutor(new ChangePasswordCommand(this));
        getCommand("arcanelogin").setExecutor(new AdminCommand(this));
        getCommand("unregister").setExecutor(new UnregisterCommand(this));

        getLogger().info("ArcaneLogin has been enabled!");
    }

    @Override
    public void onDisable() {
        if (this.databaseManager != null) {
            this.databaseManager.closeConnection();
        }
        getLogger().info("ArcaneLogin has been disabled!");
    }

    public static ArcaneLogin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
