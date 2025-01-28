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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

public class InventoryShiftClickListener implements Listener {
    private final HiddenArmor plugin;
    private final HiddenArmorManager hiddenArmorManager;

    public InventoryShiftClickListener(HiddenArmor plugin) {
        this.plugin = plugin;
        this.hiddenArmorManager = plugin.getHiddenArmorManager();
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
        // Check if shift-clicking armor
        else if (event.isShiftClick() && event.getCurrentItem() != null) {
            ItemStack item = event.getCurrentItem();
            String type = item.getType().toString();
            if (type.endsWith("_HELMET") || type.endsWith("_CHESTPLATE") || 
                type.endsWith("_LEGGINGS") || type.endsWith("_BOOTS") || 
                type.equals("ELYTRA")) {
                shouldUpdate = true;
            }
        }
        // If clicking in player inventory or cursor has armor
        else if (event.getClickedInventory() instanceof PlayerInventory || 
                 (event.getCursor() != null && isArmor(event.getCursor()))) {
            shouldUpdate = true;
        }

        if (shouldUpdate) {
            // Small delay to ensure inventory is updated first
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && hiddenArmorManager.isArmorHidden(player)) {
                        ArmorPacketHandler.getInstance().updateSelf(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
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
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && hiddenArmorManager.isArmorHidden(player)) {
                        ArmorPacketHandler.getInstance().updateSelf(player);
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    private boolean isArmor(ItemStack item) {
        if (item == null) return false;
        String type = item.getType().toString();
        return type.endsWith("_HELMET") || type.endsWith("_CHESTPLATE") || 
               type.endsWith("_LEGGINGS") || type.endsWith("_BOOTS") || 
               type.equals("ELYTRA");
    }
}
