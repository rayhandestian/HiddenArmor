package me.kteq.hiddenarmor.listener;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.manager.HiddenArmorManager;
import me.kteq.hiddenarmor.util.EventUtil;
import me.kteq.hiddenarmor.event.ArmorVisibilityChangeEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DamageListener implements Listener {
    private final HiddenArmor plugin;
    private final HiddenArmorManager hiddenArmorManager;
    private final Map<UUID, BukkitTask> reHideTasks;

    public DamageListener(HiddenArmor plugin) {
        this.plugin = plugin;
        this.hiddenArmorManager = plugin.getHiddenArmorManager();
        this.reHideTasks = new HashMap<>();
        EventUtil.register(this, plugin);
    }

    @EventHandler
    public void onArmorVisibilityChange(ArmorVisibilityChangeEvent event) {
        // Clean up any pending re-hide tasks when armor visibility is manually changed
        cleanupTask(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        cleanupTask(event.getEntity());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupTask(event.getPlayer());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        // Clean up tasks when entering creative mode (armor will be force-shown)
        cleanupTask(event.getPlayer());
    }

    private void cleanupTask(Player player) {
        BukkitTask task = reHideTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Cancels all pending re-hide tasks and clears the task map
     */
    public void cleanup() {
        reHideTasks.values().forEach(BukkitTask::cancel);
        reHideTasks.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        
        // Only check if the feature is enabled
        if (!plugin.getConfig().getBoolean("damage-unhide.enabled", true)) return;

        // Only proceed if the player has manually hidden their armor
        if (!hiddenArmorManager.isEnabled(player)) return;

        // Don't process if damage is 0 or negative
        if (event.getFinalDamage() <= 0) return;

        boolean shouldUnhide = false;

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent) event;
            Entity damager = entityEvent.getDamager();
            
            if (damager instanceof Player) {
                // Player damage
                shouldUnhide = plugin.getConfig().getBoolean("damage-unhide.player-damage", true);
            } else {
                // Other entity damage
                shouldUnhide = plugin.getConfig().getBoolean("damage-unhide.entity-damage", true);
            }
        } else {
            // Environmental damage
            shouldUnhide = plugin.getConfig().getBoolean("damage-unhide.environmental-damage", true);
        }

        if (shouldUnhide) {
            // Get visibility duration from config (in ticks, 20 ticks = 1 second)
            int duration = plugin.getConfig().getInt("damage-unhide.visibility-duration", 200);
            boolean notify = plugin.getConfig().getBoolean("damage-unhide.notify", true);
            
            // Cancel any existing re-hide task first
            BukkitTask existingTask = reHideTasks.remove(player.getUniqueId());
            if (existingTask != null) {
                existingTask.cancel();
            }

            // Unhide the armor
            hiddenArmorManager.disablePlayer(player, notify);

            // Schedule re-hide if duration > 0
            if (duration > 0) {
                BukkitTask task = new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            if (player.isOnline() && !player.isDead()) {
                                hiddenArmorManager.enablePlayer(player, notify);
                            }
                        } finally {
                            reHideTasks.remove(player.getUniqueId());
                        }
                    }
                }.runTaskLater(plugin, duration);

                reHideTasks.put(player.getUniqueId(), task);
            }
        }
    }
} 