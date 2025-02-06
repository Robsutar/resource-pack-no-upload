package com.robsutar.rnu.bukkit;

import com.robsutar.rnu.RNUConfig;
import com.robsutar.rnu.ResourcePackInfo;
import com.robsutar.rnu.ResourcePackNoUpload;
import com.robsutar.rnu.ResourcePackState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public class BukkitListener implements Listener {
    private final ResourcePackNoUpload plugin;

    public BukkitListener(ResourcePackNoUpload plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.resourcePackState() instanceof ResourcePackState.Loaded) {
            ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) plugin.resourcePackState();
            plugin.targetPlatform().setResourcePack(player, loaded.resourcePackInfo());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerResourcePackStatus(PlayerResourcePackStatusEvent event) {
        plugin.targetPlatform().runSync(() -> {
            RNUConfig config = plugin.config();
            Player player = event.getPlayer();
            PlayerResourcePackStatusEvent.Status status = event.getStatus();

            switch (status.name()) {
                // Intermediates called here.
                case "ACCEPTED":
                case "DOWNLOADED":
                    break;
                case "DECLINED": {
                    if (config.kickOnRefuseMessage() != null) {
                        player.kickPlayer(config.kickOnRefuseMessage());
                    }
                    break;
                }
                case "INVALID_URL":
                case "FAILED_DOWNLOAD":
                case "FAILED_RELOAD":
                case "DISCARDED": {
                    if (config.kickOnFailMessage() != null) {
                        player.kickPlayer(config.kickOnFailMessage().replace("<error_code>", status.name()));
                    }
                    break;
                }
                case "SUCCESSFULLY_LOADED":
                    // All ok.
                    break;
                default:
                    throw new IllegalStateException(
                            "Invalid state. Is " + plugin.getName() + " outdated?"
                    );
            }
        });
    }

    @EventHandler
    public void onRNUPackLoaded(RNUPackLoadedEvent event) {
        ResourcePackInfo resourcePackInfo = event.getResourcePackInfo();
        for (Player pending : Bukkit.getOnlinePlayers()) {
            plugin.targetPlatform().setResourcePack(pending, resourcePackInfo);
        }
    }
}
