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

        // Delay the update slightly to ensure all inventory contents are properly loaded
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && hiddenArmorManager.isArmorHidden(player)) {
                    ArmorPacketHandler.getInstance().updateSelf(player);
                }
            }
        }.runTaskLater(plugin, 5L); // 5 tick delay (0.25 seconds)
    }

    @EventHandler(priority = EventPriority.MONITOR)
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
            // Schedule a single delayed update
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && hiddenArmorManager.isArmorHidden(player)) {
                        ArmorPacketHandler.getInstance().updateSelf(player);
                        lastTeleportUpdate.put(playerId, System.currentTimeMillis());
                    }
                    pendingTeleportTasks.remove(playerId);
                }
            }.runTaskLater(plugin, 2L);
            
            pendingTeleportTasks.put(playerId, task);
            return;
        }

        // Update immediately if we're outside the cooldown
        lastTeleportUpdate.put(playerId, currentTime);
        ArmorPacketHandler.getInstance().updateSelf(player);
    }
} 