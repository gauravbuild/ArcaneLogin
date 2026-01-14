package me.gauravbuilds.arcanelogin.commands;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import me.gauravbuilds.arcanelogin.managers.ConfigManager;
import me.gauravbuilds.arcanelogin.utils.PasswordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnregisterCommand implements CommandExecutor {

    private final ArcaneLogin plugin;

    public UnregisterCommand(ArcaneLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        ConfigManager cm = plugin.getConfigManager();

        // Must be logged in to unregister
        if (!plugin.getSessionManager().isAuthenticated(player)) {
            player.sendMessage(cm.getMessage("welcome-login"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cUsage: /unregister <password>");
            return true;
        }

        String password = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String storedHash = plugin.getDatabaseManager().getPasswordHash(player.getUniqueId());

            if (PasswordUtils.checkPassword(password, storedHash)) {

                // 1. Save Location (Critical step for "Ghost" feature)
                Location currentLoc = player.getLocation();
                plugin.getDatabaseManager().saveLocation(player.getUniqueId(), currentLoc);

                // 2. Soft Delete (Remove password only)
                plugin.getDatabaseManager().softDeleteUser(player.getUniqueId());

                Bukkit.getScheduler().runTask(plugin, () -> {
                    // 3. Kick to Limbo (Ghost Mode)
                    plugin.getSessionManager().logout(player);
                    plugin.getSessionManager().startLimbo(player);
                    player.sendMessage("§aUnregistered successfully! You are now a ghost.");
                    player.sendMessage("§7Use /register to restore your account and position.");
                });

            } else {
                player.sendMessage(cm.getMessage("wrong-password"));
            }
        });

        return true;
    }
}