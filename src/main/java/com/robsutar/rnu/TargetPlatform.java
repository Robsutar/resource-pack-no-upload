package com.robsutar.rnu;

import org.bukkit.entity.Player;

public interface TargetPlatform {
    void runTask(Runnable runnable, int delay);

    default void runTask(Runnable runnable) {
        runTask(runnable, 1);
    }
    
    void runTaskTimer(Runnable runnable, int delay, int period);

    void setResourcePack(Player player, ResourcePackInfo resourcePackInfo);
}
