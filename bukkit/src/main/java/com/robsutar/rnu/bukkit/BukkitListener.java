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

import java.util.HashMap;
import java.util.Map;

public class BukkitListener implements Listener {
    private final ResourcePackNoUpload plugin;
    private final HashMap<Player, Long> pending = new HashMap<>();

    public BukkitListener(ResourcePackNoUpload plugin) {
        this.plugin = plugin;
        plugin.targetPlatform().runTaskTimer(this::checkPending, 40, 40);
    }

    private void checkPending() {
        if (plugin.resourcePackState() instanceof ResourcePackState.Loaded) {
            ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) plugin.resourcePackState();
            long currentTime = System.currentTimeMillis();

            for (Map.Entry<Player, Long> entry : pending.entrySet()) {
                if (currentTime - entry.getValue() > 1000) {
                    addPending(entry.getKey(), loaded.resourcePackInfo());
                }
            }
        }
    }

    private void addPending(Player player, ResourcePackInfo resourcePackInfo) {
        pending.put(player, System.currentTimeMillis());
        plugin.targetPlatform().setResourcePack(player, resourcePackInfo);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.resourcePackState() instanceof ResourcePackState.Loaded) {
            ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) plugin.resourcePackState();
            addPending(player, loaded.resourcePackInfo());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        if (pending.remove(player) != null) plugin.targetPlatform().runTask(() -> {
            RNUConfig config = plugin.config();
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            addPending(player, resourcePackInfo);
        }
    }
}
