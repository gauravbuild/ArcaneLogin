package me.gauravbuilds.arcanelogin.listeners;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import me.gauravbuilds.arcanelogin.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    private final ArcaneLogin plugin;

    public PlayerListener(ArcaneLogin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ConfigManager cm = plugin.getConfigManager();

        // Async check
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean isRegistered = plugin.getDatabaseManager().isRegistered(player.getUniqueId());
            String lastIp = plugin.getDatabaseManager().getLastIp(player.getUniqueId());
            String currentIp = player.getAddress().getAddress().getHostAddress();

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!isRegistered) {
                    // Scenario A: New Player
                    plugin.getSessionManager().startLimbo(player);
                    player.sendMessage(cm.getMessage("welcome-register"));
                } else {
                    if (lastIp != null && lastIp.equals(currentIp)) {
                        // Scenario B: Returning Player (IP MATCH) -> Auto Login
                        plugin.getSessionManager().login(player);
                        plugin.getDatabaseManager().updateLogin(player.getUniqueId(), currentIp); // Update timestamp
                        player.sendMessage(cm.getMessage("session-resumed"));
                    } else {
                        // Scenario C: Returning Player (IP MISMATCH)
                        plugin.getSessionManager().startLimbo(player);
                        player.sendMessage(cm.getMessage("welcome-login"));
                    }
                }
            });
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getSessionManager().isAuthenticated(event.getPlayer())) {
            Location from = event.getFrom();
            Location to = event.getTo();

            if (to == null) return;

            // Allow head rotation (pitch/yaw changes) but deny X/Y/Z changes
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                // Teleport back to 'from' but keep the 'to' rotation to allow looking around
                Location newLoc = from.clone();
                newLoc.setYaw(to.getYaw());
                newLoc.setPitch(to.getPitch());
                event.setTo(newLoc);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getSessionManager().isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!plugin.getSessionManager().isAuthenticated(event.getPlayer())) {
            String msg = event.getMessage().toLowerCase();
            if (!msg.startsWith("/register") && !msg.startsWith("/login") && !msg.startsWith("/al") && !msg.startsWith("/arcanelogin")) {
                 event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player && !plugin.getSessionManager().isAuthenticated((Player) event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player && !plugin.getSessionManager().isAuthenticated((Player) event.getWhoClicked())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getSessionManager().isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(PlayerPickupItemEvent event) {
        if (!plugin.getSessionManager().isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getSessionManager().isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getSessionManager().isAuthenticated(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getSessionManager().logout(event.getPlayer());
    }
}
