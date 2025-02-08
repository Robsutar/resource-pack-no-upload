package com.robsutar.rnu.velocity;

import com.robsutar.rnu.RNUConfig;
import com.robsutar.rnu.ResourcePackInfo;
import com.robsutar.rnu.ResourcePackNoUpload;
import com.robsutar.rnu.ResourcePackState;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class VelocityListener {
    private final ResourcePackNoUpload plugin;

    private final HashMap<Player, Long> pending = new HashMap<>();

    public VelocityListener(ResourcePackNoUpload plugin) {
        this.plugin = plugin;

        plugin.getServer().getScheduler().buildTask(plugin, this::checkPending)
                .repeat(40 * 50, TimeUnit.MILLISECONDS)
                .schedule();
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
        player.clearResourcePacks();
        player.sendResourcePackOffer(plugin.getServer().createResourcePackBuilder(resourcePackInfo.uri())
                .setId(resourcePackInfo.id())
                .setHash(resourcePackInfo.hash())
                .setPrompt(plugin.text(resourcePackInfo.prompt()))
                .setShouldForce(false)
                .build()
        );
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        if (plugin.resourcePackState() instanceof ResourcePackState.Loaded) {
            ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) plugin.resourcePackState();
            addPending(player, loaded.resourcePackInfo());
        }
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        Player player = event.getPlayer();

        pending.remove(player);
    }

    @Subscribe
    public void onPlayerResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        if (pending.remove(player) != null) {
            RNUConfig config = plugin.config();
            PlayerResourcePackStatusEvent.Status status = event.getStatus();

            switch (status.name()) {
                // Intermediates called here.
                case "ACCEPTED":
                case "DOWNLOADED":
                    break;
                case "DECLINED": {
                    if (config.kickOnRefuseMessage() != null) {
                        player.disconnect(plugin.text(config.kickOnRefuseMessage()));
                    }
                    break;
                }
                case "INVALID_URL":
                case "FAILED_DOWNLOAD":
                case "FAILED_RELOAD":
                case "DISCARDED": {
                    if (config.kickOnFailMessage() != null) {
                        player.disconnect(plugin.text(config.kickOnFailMessage().replace("<error_code>", status.name())));
                    }
                    break;
                }
                case "SUCCESSFUL":
                    // All ok.
                    break;
                default:
                    throw new IllegalStateException(
                            "Invalid state. Is " + plugin.getName() + " outdated?"
                    );
            }
        }
    }

    @Subscribe
    public void onRNUPackLoaded(RNUPackLoadedEvent event) {
        ResourcePackInfo resourcePackInfo = event.getResourcePackInfo();
        for (Player player : plugin.getServer().getAllPlayers()) {
            addPending(player, resourcePackInfo);
        }
    }
}
