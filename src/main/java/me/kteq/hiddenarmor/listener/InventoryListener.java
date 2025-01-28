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

public class InventoryListener implements Listener {
    private final HiddenArmor plugin;
    private final HiddenArmorManager hiddenArmorManager;

    public InventoryListener(HiddenArmor plugin) {
        this.plugin = plugin;
        this.hiddenArmorManager = plugin.getHiddenArmorManager();
        EventUtil.register(this, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!hiddenArmorManager.isArmorHidden(player)) return;

        // Schedule an update if:
        // 1. Clicked in armor slots
        // 2. Shift-clicked armor
        // 3. Clicked in player inventory (as this refreshes the entire inventory view)
        boolean shouldUpdate = false;

        int slot = event.getRawSlot();
        if (slot >= 5 && slot <= 8) {  // Armor slots
            shouldUpdate = true;
        } else if (event.isShiftClick() && event.getCurrentItem() != null) {
            String itemType = event.getCurrentItem().getType().toString();
            shouldUpdate = itemType.endsWith("_HELMET") ||
                          itemType.endsWith("_CHESTPLATE") ||
                          itemType.endsWith("_LEGGINGS") ||
                          itemType.endsWith("_BOOTS") ||
                          itemType.equals("ELYTRA");
        } else if (event.getClickedInventory() instanceof PlayerInventory) {
            shouldUpdate = true;
        }

        if (shouldUpdate) {
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
}
