package com.robsutar.rnu.velocity;

import com.robsutar.rnu.RNUConfig;
import com.robsutar.rnu.ResourcePackInfo;
import com.robsutar.rnu.ResourcePackNoUpload;
import com.robsutar.rnu.ResourcePackState;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.HashMap;
import java.util.Map;

public class VelocityListener {
    private final ResourcePackNoUpload rnu;

    private final HashMap<Player, Long> pending = new HashMap<>();

    public VelocityListener(ResourcePackNoUpload rnu) {
        this.rnu = rnu;
    }

    public void register() {
        rnu.scheduler().repeatAsync(this::checkPending, rnu.config().resendingDelay());
        rnu.getServer().getEventManager().register(rnu, this);
    }

    private void checkPending() {
        if (rnu.resourcePackState() instanceof ResourcePackState.Loaded) {
            ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) rnu.resourcePackState();
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
        player.sendResourcePackOffer(rnu.getServer().createResourcePackBuilder(resourcePackInfo.uri())
                .setId(resourcePackInfo.id())
                .setHash(resourcePackInfo.hash())
                .setPrompt(rnu.text(resourcePackInfo.prompt()))
                .setShouldForce(false)
                .build()
        );
    }

    @Subscribe
    public void onPlayerJoin(ServerPostConnectEvent event) {
        if (event.getPreviousServer() == null) {
            rnu.scheduler().runLaterAsync(() -> {
                Player player = event.getPlayer();
                if (!pending.containsKey(player) && rnu.resourcePackState() instanceof ResourcePackState.Loaded) {
                    ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) rnu.resourcePackState();
                    addPending(player, loaded.resourcePackInfo());
                }
            }, rnu.config().sendingDelay());
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
            RNUConfig config = rnu.config();
            PlayerResourcePackStatusEvent.Status status = event.getStatus();

            switch (status.name()) {
                // Intermediates called here.
                case "ACCEPTED":
                case "DOWNLOADED":
                    break;
                case "DECLINED": {
                    if (config.kickOnRefuseMessage() != null) {
                        player.disconnect(rnu.text(config.kickOnRefuseMessage()));
                    }
                    break;
                }
                case "INVALID_URL":
                case "FAILED_DOWNLOAD":
                case "FAILED_RELOAD":
                case "DISCARDED": {
                    if (config.kickOnFailMessage() != null) {
                        player.disconnect(rnu.text(config.kickOnFailMessage().replace("<error_code>", status.name())));
                    }
                    break;
                }
                case "SUCCESSFUL":
                    // All ok.
                    break;
                default:
                    throw new IllegalStateException(
                            "Invalid state. Is " + rnu.getName() + " outdated?"
                    );
            }
        }
    }

    @Subscribe
    public void onRNUPackLoaded(RNUPackLoadedEvent event) {
        ResourcePackInfo resourcePackInfo = event.getResourcePackInfo();
        for (Player player : rnu.getServer().getAllPlayers()) {
            addPending(player, resourcePackInfo);
        }
    }
}
