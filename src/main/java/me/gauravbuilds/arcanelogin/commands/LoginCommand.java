package me.gauravbuilds.arcanelogin.commands;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import me.gauravbuilds.arcanelogin.managers.ConfigManager;
import me.gauravbuilds.arcanelogin.utils.PasswordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginCommand implements CommandExecutor {

    private final ArcaneLogin plugin;

    public LoginCommand(ArcaneLogin plugin) {
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

        if (args.length != 1) {
            player.sendMessage(cm.getMessage("usage-login"));
            return true;
        }

        String password = args[0];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!plugin.getDatabaseManager().isRegistered(player.getUniqueId())) {
                player.sendMessage(cm.getMessage("not-registered"));
                return;
            }

            String storedHash = plugin.getDatabaseManager().getPasswordHash(player.getUniqueId());
            if (PasswordUtils.checkPassword(password, storedHash)) {
                String ip = player.getAddress().getAddress().getHostAddress();
                plugin.getDatabaseManager().updateLogin(player.getUniqueId(), ip);
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                   plugin.getSessionManager().login(player);
                   player.sendMessage(cm.getMessage("login-success"));
                });
            } else {
                player.sendMessage(cm.getMessage("wrong-password"));
            }
        });

        return true;
    }
}
