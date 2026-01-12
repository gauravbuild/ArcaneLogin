package me.gauravbuilds.arcanelogin.commands;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import me.gauravbuilds.arcanelogin.managers.ConfigManager;
import me.gauravbuilds.arcanelogin.utils.PasswordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AdminCommand implements CommandExecutor {

    private final ArcaneLogin plugin;

    public AdminCommand(ArcaneLogin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arcanelogin.admin")) {
            sender.sendMessage("§cYou do not have permission to execute this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /al <reload|resetpass>");
            return true;
        }

        ConfigManager cm = plugin.getConfigManager();

        if (args[0].equalsIgnoreCase("reload")) {
            cm.reloadConfig();
            sender.sendMessage(cm.getMessage("admin-reload"));
            return true;
        }

        if (args[0].equalsIgnoreCase("resetpass")) {
            if (args.length != 3) {
                sender.sendMessage("§cUsage: /al resetpass <player> <newpass>");
                return true;
            }

            String targetName = args[1];
            String newPass = args[2];

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (!plugin.getDatabaseManager().userExists(targetName)) {
                     sender.sendMessage(cm.getMessage("admin-player-not-found").replace("%player%", targetName));
                     return;
                }

                String newHash = PasswordUtils.hashPassword(newPass);
                plugin.getDatabaseManager().updatePassword(targetName, newHash);
                sender.sendMessage(cm.getMessage("admin-reset-success").replace("%player%", targetName));
            });
            return true;
        }

        return true;
    }
}
