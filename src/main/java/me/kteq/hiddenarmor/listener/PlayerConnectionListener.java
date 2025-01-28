package me.kteq.hiddenarmor.listener;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorPacketHandler;
import me.kteq.hiddenarmor.manager.HiddenArmorManager;
import me.kteq.hiddenarmor.util.EventUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class PlayerConnectionListener implements Listener {
    private final HiddenArmor plugin;
    private final HiddenArmorManager hiddenArmorManager;
    private final Map<UUID, Long> lastTeleportUpdate;
    private final Map<UUID, BukkitTask> pendingTeleportTasks;
    private static final long TELEPORT_COOLDOWN = 100L; // 100ms cooldown
    private static final int[] UPDATE_TICKS = {1, 3, 5, 10}; // Multiple update attempts

    public PlayerConnectionListener(HiddenArmor plugin) {
        this.plugin = plugin;
        this.hiddenArmorManager = plugin.getHiddenArmorManager();
        this.lastTeleportUpdate = new WeakHashMap<>();
        this.pendingTeleportTasks = new WeakHashMap<>();
        EventUtil.register(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!hiddenArmorManager.isArmorHidden(player)) return;

        // Schedule multiple updates to ensure armor stays hidden
        scheduleMultipleUpdates(player, new int[]{5, 10, 15});
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!hiddenArmorManager.isArmorHidden(player)) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Cancel any pending update task for this player
        BukkitTask pendingTask = pendingTeleportTasks.remove(playerId);
        if (pendingTask != null) {
            pendingTask.cancel();
        }

        // Check if we're updating too frequently
        Long lastUpdate = lastTeleportUpdate.get(playerId);
        if (lastUpdate != null && currentTime - lastUpdate < TELEPORT_COOLDOWN) {
            // Schedule multiple updates
            scheduleMultipleUpdates(player, UPDATE_TICKS);
            return;
        }

        // Update immediately and schedule follow-up updates
        lastTeleportUpdate.put(playerId, currentTime);
        ArmorPacketHandler.getInstance().updateSelf(player);
        scheduleMultipleUpdates(player, UPDATE_TICKS);
    }

    private void scheduleMultipleUpdates(Player player, int[] delays) {
        UUID playerId = player.getUniqueId();
        
        // Schedule a series of updates
        new BukkitRunnable() {
            private int updateIndex = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || !hiddenArmorManager.isArmorHidden(player)) {
                    this.cancel();
                    pendingTeleportTasks.remove(playerId);
                    return;
                }

                ArmorPacketHandler.getInstance().updateSelf(player);
                lastTeleportUpdate.put(playerId, System.currentTimeMillis());

                updateIndex++;
                if (updateIndex >= delays.length) {
                    this.cancel();
                    pendingTeleportTasks.remove(playerId);
                }
            }
        }.runTaskTimer(plugin, delays[0], Math.max(1, delays[1] - delays[0]));
    }
} 