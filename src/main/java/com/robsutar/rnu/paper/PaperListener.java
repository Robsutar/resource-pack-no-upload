package com.robsutar.rnu.paper;

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

public class PaperListener implements Listener {
    private final ResourcePackNoUpload plugin;

    public PaperListener(ResourcePackNoUpload plugin) {
        this.plugin = plugin;
    }

    private void sendPack(Player player, ResourcePackInfo resourcePackInfo) {
        player.setResourcePack(resourcePackInfo.uri().toString(), resourcePackInfo.hash());
//        player.setResourcePack(
//                resourcePackInfo.id(),
//                resourcePackInfo.uri().toString(),
//                resourcePackInfo.hash(),
//                plugin.config().prompt(),
//                false
//        );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        if (plugin.resourcePackState() instanceof ResourcePackState.Loaded loaded) {
            sendPack(player, loaded.resourcePackInfo());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerResourcePackStatus(PlayerResourcePackStatusEvent event) {
        var config = plugin.config();
        var player = event.getPlayer();
        var status = event.getStatus();

        switch (status.name()) {
            // Intermediates called here.
            case "ACCEPTED", "DOWNLOADED" -> {
            }
            case "DECLINED" -> {
                if (config.kickOnRefuseMessage() != null) {
                    player.kickPlayer(config.kickOnRefuseMessage());
                }
            }
            case "INVALID_URL", "FAILED_DOWNLOAD", "FAILED_RELOAD", "DISCARDED" -> {
                if (config.kickOnFailMessage() != null) {
                    player.kickPlayer(config.kickOnFailMessage().replace("<error_code>", status.name()));
                }
            }
            case "SUCCESSFULLY_LOADED" -> {
                // All ok.
            }
            default -> throw new IllegalStateException(
                    "Invalid state, " + plugin.getName() + " is outdated?"
            );
        }
    }

    @EventHandler
    public void onRNUPackLoaded(RNUPackLoadedEvent event) {
        var resourcePackInfo = event.getResourcePackInfo();
        for (var pending : Bukkit.getOnlinePlayers()) {
            sendPack(pending, resourcePackInfo);
        }
    }
}
