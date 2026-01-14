package me.gauravbuilds.arcanelogin.managers;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import net.md_5.bungee.api.ChatColor; // Import Bungee ChatColor for HEX support
import org.bukkit.configuration.file.FileConfiguration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {

    private final ArcaneLogin plugin;
    private FileConfiguration config;

    // Pattern to find hex codes like &#FFFFFF
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public ConfigManager(ArcaneLogin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // New color translation method supporting Hex and Standard codes
    private String translateColors(String message) {
        if (message == null) return "";

        // 1. Replace Hex Codes (&#RRGGBB)
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            try {
                String hexCode = "#" + matcher.group(1);
                matcher.appendReplacement(buffer, ChatColor.of(hexCode).toString());
            } catch (Exception e) {
                // Fallback if version is too old (unlikely on 1.21)
                matcher.appendReplacement(buffer, "");
            }
        }
        matcher.appendTail(buffer);

        // 2. Replace Standard Codes (&a, &l, etc.)
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public String getMessage(String path) {
        String msg = config.getString("messages." + path);
        if (msg == null) return "Message not found: " + path;
        // Apply prefix + message colors
        return translateColors(getPrefix() + msg);
    }

    public String getRawMessage(String path) {
        String msg = config.getString("messages." + path);
        if (msg == null) return "Message not found: " + path;
        return translateColors(msg);
    }

    public String getPrefix() {
        String prefix = config.getString("messages.prefix");
        return prefix != null ? translateColors(prefix) : "";
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