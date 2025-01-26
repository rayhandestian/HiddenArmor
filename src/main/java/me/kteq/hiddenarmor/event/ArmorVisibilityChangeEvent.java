package me.kteq.hiddenarmor.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ArmorVisibilityChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final boolean hidden;

    public ArmorVisibilityChangeEvent(Player player, boolean hidden) {
        this.player = player;
        this.hidden = hidden;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isHidden() {
        return hidden;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
} 