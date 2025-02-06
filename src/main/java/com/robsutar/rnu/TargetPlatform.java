package com.robsutar.rnu;

import org.bukkit.entity.Player;

public interface TargetPlatform {
    void runSync(Runnable runnable);

    void setResourcePack(Player player, ResourcePackInfo resourcePackInfo);
}
