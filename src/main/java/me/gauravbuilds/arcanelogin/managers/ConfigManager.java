package me.gauravbuilds.arcanelogin.managers;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final ArcaneLogin plugin;
    private FileConfiguration config;

    public ConfigManager(ArcaneLogin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getMessage(String path) {
        String msg = config.getString("messages." + path);
        if (msg == null) return "Message not found: " + path;
        return ChatColor.translateAlternateColorCodes('&', getPrefix() + msg);
    }
    
    public String getRawMessage(String path) {
        String msg = config.getString("messages." + path);
        if (msg == null) return "Message not found: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String getPrefix() {
        String prefix = config.getString("messages.prefix");
        return prefix != null ? ChatColor.translateAlternateColorCodes('&', prefix) : "";
    }

    public int getMinPasswordLength() {
        return config.getInt("security.min-password-length", 6);
    }

    public int getMaxPasswordLength() {
        return config.getInt("security.max-password-length", 32);
    }

    public String getPasswordRegex() {
        return config.getString("security.password-regex", "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]*$");
    }
}
