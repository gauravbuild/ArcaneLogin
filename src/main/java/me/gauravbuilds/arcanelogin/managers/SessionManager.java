package me.gauravbuilds.arcanelogin.managers;

import me.gauravbuilds.arcanelogin.ArcaneLogin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SessionManager {

    private final ArcaneLogin plugin;
    private final Set<UUID> authenticatedPlayers;

    public SessionManager(ArcaneLogin plugin) {
        this.plugin = plugin;
        this.authenticatedPlayers = new HashSet<>();
    }

    public void login(Player player) {
        authenticatedPlayers.add(player.getUniqueId());
        
        // Remove blindness
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        
        // Send success message (can be customized per case in calling method, but here we ensure state is correct)
    }

    public void logout(Player player) {
        authenticatedPlayers.remove(player.getUniqueId());
    }

    public boolean isAuthenticated(Player player) {
        return authenticatedPlayers.contains(player.getUniqueId());
    }

    public void startLimbo(Player player) {
        // Teleport to spawn
        Location spawn = player.getWorld().getSpawnLocation();
        player.teleport(spawn);

        // Apply blindness
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
    }
}
