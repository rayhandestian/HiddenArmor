package me.kteq.hiddenarmor.handler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import me.kteq.hiddenarmor.HiddenArmor;
import me.kteq.hiddenarmor.util.ProtocolUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class ArmorPacketHandler {
    private static ArmorPacketHandler instance;
    private HiddenArmor plugin;
    private ProtocolManager protocolManager;
    private final WeakHashMap<Player, Long> lastUpdateTime;
    private static final long UPDATE_COOLDOWN = 50L; // 50ms cooldown between updates

    private ArmorPacketHandler() {
        this.lastUpdateTime = new WeakHashMap<>();
    }

    public static ArmorPacketHandler getInstance() {
        if (instance == null) {
            instance = new ArmorPacketHandler();
        }
        return instance;
    }

    public void setup(HiddenArmor plugin, ProtocolManager protocolManager) {
        this.plugin = plugin;
        this.protocolManager = protocolManager;
    }

    public void updatePlayer(Player player) {
        // Check if we're updating too frequently
        long currentTime = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(player);
        if (lastUpdate != null && currentTime - lastUpdate < UPDATE_COOLDOWN) {
            return;
        }
        lastUpdateTime.put(player, currentTime);

        updateSelf(player);
        updateOthers(player);
    }

    public void updateSelf(Player player) {
        PlayerInventory inv = player.getInventory();
        // Update only armor slots (5-8)
        for (int i = 5; i <= 8; i++) {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SET_SLOT);
            packet.getIntegers().write(0, 0);
            if (!plugin.isOld()) {
                packet.getIntegers().write(2, i);
            } else {
                packet.getIntegers().write(1, i);
            }
            ItemStack armor = ProtocolUtil.getArmor(ProtocolUtil.ArmorType.getType(i), inv);
            packet.getItemModifier().write(0, armor);
            try {
                protocolManager.sendServerPacket(player, packet);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public void updateOthers(Player player) {
        PlayerInventory inv = player.getInventory();
        PacketContainer packetOthers = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packetOthers.getIntegers().write(0, player.getEntityId());
        
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairList = new ArrayList<>();
        
        // Only send armor updates
        ItemStack helmet = inv.getHelmet();
        ItemStack chestplate = inv.getChestplate();
        ItemStack leggings = inv.getLeggings();
        ItemStack boots = inv.getBoots();
        
        if (helmet != null && !helmet.getType().isAir()) {
            pairList.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, helmet.clone()));
        }
        if (chestplate != null && !chestplate.getType().isAir()) {
            pairList.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, chestplate.clone()));
        }
        if (leggings != null && !leggings.getType().isAir()) {
            pairList.add(new Pair<>(EnumWrappers.ItemSlot.LEGS, leggings.clone()));
        }
        if (boots != null && !boots.getType().isAir()) {
            pairList.add(new Pair<>(EnumWrappers.ItemSlot.FEET, boots.clone()));
        }
        
        if (!pairList.isEmpty()) {
            packetOthers.getSlotStackPairLists().write(0, pairList);
            ProtocolUtil.broadcastPlayerPacket(protocolManager, packetOthers, player);
        }
    }
}
