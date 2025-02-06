package com.robsutar.rnu.paper;

import com.robsutar.rnu.ResourcePackInfo;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RNUPackLoadedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final @NotNull ResourcePackInfo resourcePackInfo;

    public RNUPackLoadedEvent(@NotNull ResourcePackInfo resourcePackInfo) {
        this.resourcePackInfo = resourcePackInfo;
    }

    public @NotNull ResourcePackInfo getResourcePackInfo() {
        return resourcePackInfo;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
