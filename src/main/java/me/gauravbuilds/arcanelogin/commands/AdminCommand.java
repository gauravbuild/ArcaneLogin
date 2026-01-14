package me.gauravbuilds.arcanelogin.commands;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import me.gauravbuilds.arcanelogin.managers.ConfigManager;
import me.gauravbuilds.arcanelogin.utils.PasswordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor {

    private final ArcaneLogin plugin;

    public AdminCommand(ArcaneLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arcanelogin.admin")) {
            sender.sendMessage("§cYou do not have permission.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /al <reload|resetpass|setpremium|unregister>");
            return true;
        }

        ConfigManager cm = plugin.getConfigManager();

        // ... existing reload ...
        if (args[0].equalsIgnoreCase("reload")) {
            cm.reloadConfig();
            sender.sendMessage(cm.getMessage("admin-reload"));
            return true;
        }

        // ... existing resetpass ...
        if (args[0].equalsIgnoreCase("resetpass")) {
            if (args.length != 3) {
                sender.sendMessage("§cUsage: /al resetpass <player> <newpass>");
                return true;
            }
            // (Keep your existing code here)
            return true;
        }

        // NEW: Set Premium
        if (args[0].equalsIgnoreCase("setpremium")) {
            if (args.length != 3) {
                sender.sendMessage("§cUsage: /al setpremium <player> <true|false>");
                return true;
            }
            String target = args[1];
            boolean status = Boolean.parseBoolean(args[2]);

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (!plugin.getDatabaseManager().userExists(target)) {
                    sender.sendMessage("§cPlayer not found.");
                    return;
                }
                plugin.getDatabaseManager().setPremium(target, status);
                sender.sendMessage("§aUpdated premium status for " + target + " to " + status);
            });
            return true;
        }

        // NEW: Force Unregister
        if (args[0].equalsIgnoreCase("unregister")) {
            if (args.length != 2) {
                sender.sendMessage("§cUsage: /al unregister <player>");
                return true;
            }
            String targetName = args[1];

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                // We need UUID to unregister properly, slightly complex if offline
                // For now, let's assume online or we'd need a Name->UUID fetcher.
                // Simplified: Only works if we can find them or you add a fetcher.
                Player target = Bukkit.getPlayer(targetName);
                if (target != null) {
                    plugin.getDatabaseManager().softDeleteUser(target.getUniqueId());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getSessionManager().logout(target);
                        plugin.getSessionManager().startLimbo(target);
                        target.sendMessage("§cYou have been unregistered by an admin.");
                    });
                    sender.sendMessage("§aUnregistered " + targetName);
                } else {
                    sender.sendMessage("§cPlayer must be online to unregister (or implement UUID fetcher).");
                }
            });
            return true;
        }

        return true;
    }
}