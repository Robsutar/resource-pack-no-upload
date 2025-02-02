package com.robsutar.rnu.paper;

import com.robsutar.rnu.ResourcePackNoUpload;
import com.robsutar.rnu.ResourcePackState;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PaperListener implements Listener {
    private final ResourcePackNoUpload plugin;

    public PaperListener(ResourcePackNoUpload plugin) {
        this.plugin = plugin;
    }

    private void sendPack(Player player, ResourcePackInfo resourcePackInfo) {
        var config = plugin.config();

        player.clearResourcePacks();
        var resourcePack = ResourcePackRequest.resourcePackRequest()
                .callback((uuid, status, audience) -> {
                    if (!status.intermediate()) switch (status) {
                        case DECLINED -> {
                            if (config.kickOnRefuseMessage() != null) {
                                player.kick(config.kickOnRefuseMessage());
                            }
                        }
                        case INVALID_URL, FAILED_DOWNLOAD, FAILED_RELOAD, DISCARDED -> {
                            if (config.kickOnFailMessage() != null) {
                                player.kick(config.kickOnFailMessage().replaceText(
                                        (c) -> c.matchLiteral("<error_code>").replacement(status.name()))
                                );
                            }
                        }
                        case SUCCESSFULLY_LOADED -> {
                        }
                        // Intermediates called here, but was checked previously.
                        default -> throw new IllegalStateException(
                                "Invalid state, " + plugin.getName() + " is outdated?"
                        );
                    }
                })
                .replace(true)
                .prompt(plugin.config().prompt())
                .packs(resourcePackInfo);

        player.sendResourcePacks(resourcePack);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        if (plugin.resourcePackState() instanceof ResourcePackState.Loaded loaded) {
            sendPack(player, loaded.resourcePackInfo());
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
