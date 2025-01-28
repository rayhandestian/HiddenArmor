package me.kteq.hiddenarmor.listener;

import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.handler.ArmorPacketHandler;
import me.kteq.hiddenarmor.manager.HiddenArmorManager;
import me.kteq.hiddenarmor.util.EventUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class InventoryShiftClickListener implements Listener {
    private final HiddenArmor plugin;
    private final HiddenArmorManager hiddenArmorManager;
    private final Map<UUID, BukkitTask> pendingTasks;
    private static final int[] UPDATE_DELAYS = {1, 2, 3}; // Ticks to wait before updates

    public InventoryShiftClickListener(HiddenArmor plugin) {
        this.plugin = plugin;
        this.hiddenArmorManager = plugin.getHiddenArmorManager();
        this.pendingTasks = new WeakHashMap<>();
        EventUtil.register(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!hiddenArmorManager.isArmorHidden(player)) return;

        boolean shouldUpdate = false;

        // Check if clicking in armor slots
        int slot = event.getRawSlot();
        if (slot >= 5 && slot <= 8) {
            shouldUpdate = true;
        }
        // Any shift-click in player inventory should trigger update
        else if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            if (event.getClickedInventory() instanceof PlayerInventory) {
                shouldUpdate = true;
            }
        }
        // If clicking in player inventory or cursor has armor
        else if (event.getClickedInventory() instanceof PlayerInventory || 
                 (event.getCursor() != null && isArmor(event.getCursor()))) {
            shouldUpdate = true;
        }

        if (shouldUpdate) {
            scheduleUpdates(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!hiddenArmorManager.isArmorHidden(player)) return;

        // Check if any armor slots were affected
        boolean affectedArmorSlot = event.getRawSlots().stream()
                .anyMatch(slot -> slot >= 5 && slot <= 8);

        if (affectedArmorSlot) {
            scheduleUpdates(player);
        }
    }

    private void scheduleUpdates(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel any pending updates for this player
        BukkitTask pendingTask = pendingTasks.remove(playerId);
        if (pendingTask != null) {
            pendingTask.cancel();
        }

        // Schedule multiple updates
        BukkitTask task = new BukkitRunnable() {
            private int updateCount = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !hiddenArmorManager.isArmorHidden(player)) {
                    this.cancel();
                    pendingTasks.remove(playerId);
                    return;
                }

                ArmorPacketHandler.getInstance().updateSelf(player);
                
                updateCount++;
                if (updateCount >= UPDATE_DELAYS.length) {
                    this.cancel();
                    pendingTasks.remove(playerId);
                }
            }
        }.runTaskTimer(plugin, UPDATE_DELAYS[0], 1); // Run every tick
        
        pendingTasks.put(playerId, task);
    }

    private boolean isArmor(ItemStack item) {
        if (item == null) return false;
        String type = item.getType().toString();
        return type.endsWith("_HELMET") || type.endsWith("_CHESTPLATE") || 
               type.endsWith("_LEGGINGS") || type.endsWith("_BOOTS") || 
               type.equals("ELYTRA");
    }
}
