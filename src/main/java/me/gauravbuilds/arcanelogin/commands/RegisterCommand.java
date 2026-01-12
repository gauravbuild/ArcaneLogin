package me.gauravbuilds.arcanelogin.commands;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import me.gauravbuilds.arcanelogin.managers.ConfigManager;
import me.gauravbuilds.arcanelogin.utils.PasswordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegisterCommand implements CommandExecutor {

    private final ArcaneLogin plugin;

    public RegisterCommand(ArcaneLogin plugin) {
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

        if (plugin.getSessionManager().isAuthenticated(player)) {
            player.sendMessage(cm.getMessage("already-logged-in"));
            return true;
        }

        // Async check if already registered
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (plugin.getDatabaseManager().isRegistered(player.getUniqueId())) {
                player.sendMessage(cm.getMessage("already-registered"));
                return;
            }

            if (args.length != 1) {
                player.sendMessage(cm.getMessage("usage-register"));
                return;
            }

            String password = args[0];

            // Validation
            if (password.length() < cm.getMinPasswordLength()) {
                player.sendMessage(cm.getMessage("password-too-short")
                        .replace("%min%", String.valueOf(cm.getMinPasswordLength())));
                return;
            }
            if (password.length() > cm.getMaxPasswordLength()) {
                player.sendMessage(cm.getMessage("password-too-long")
                        .replace("%max%", String.valueOf(cm.getMaxPasswordLength())));
                return;
            }
            if (!password.matches(cm.getPasswordRegex())) {
                player.sendMessage(cm.getMessage("password-invalid-chars"));
                return;
            }

            String hash = PasswordUtils.hashPassword(password);
            String ip = player.getAddress().getAddress().getHostAddress();

            plugin.getDatabaseManager().registerUser(player.getUniqueId(), player.getName(), hash, ip);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getSessionManager().login(player);
                player.sendMessage(cm.getMessage("register-success"));
            });
        });

        return true;
    }
}
