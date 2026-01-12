package me.gauravbuilds.arcanelogin.commands;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import me.gauravbuilds.arcanelogin.managers.ConfigManager;
import me.gauravbuilds.arcanelogin.utils.PasswordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChangePasswordCommand implements CommandExecutor {

    private final ArcaneLogin plugin;

    public ChangePasswordCommand(ArcaneLogin plugin) {
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

        if (!plugin.getSessionManager().isAuthenticated(player)) {
            player.sendMessage(cm.getMessage("welcome-login"));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(cm.getMessage("usage-changepassword"));
            return true;
        }

        String oldPass = args[0];
        String newPass = args[1];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String storedHash = plugin.getDatabaseManager().getPasswordHash(player.getUniqueId());
            
            if (PasswordUtils.checkPassword(oldPass, storedHash)) {
                
                // Validate new password
                if (newPass.length() < cm.getMinPasswordLength()) {
                    player.sendMessage(cm.getMessage("password-too-short")
                            .replace("%min%", String.valueOf(cm.getMinPasswordLength())));
                    return;
                }
                if (newPass.length() > cm.getMaxPasswordLength()) {
                    player.sendMessage(cm.getMessage("password-too-long")
                            .replace("%max%", String.valueOf(cm.getMaxPasswordLength())));
                    return;
                }
                if (!newPass.matches(cm.getPasswordRegex())) {
                    player.sendMessage(cm.getMessage("password-invalid-chars"));
                    return;
                }
                
                String newHash = PasswordUtils.hashPassword(newPass);
                plugin.getDatabaseManager().updatePassword(player.getUniqueId(), newHash);
                player.sendMessage(cm.getMessage("password-changed"));
            } else {
                player.sendMessage(cm.getMessage("wrong-password"));
            }
        });

        return true;
    }
}
