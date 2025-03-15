package com.robsutar.rnu.fabric;

import com.robsutar.rnu.RNUConfig;
import com.robsutar.rnu.ResourcePackInfo;
import com.robsutar.rnu.ResourcePackNoUpload;
import com.robsutar.rnu.ResourcePackState;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

public class FabricListener {
    private final ResourcePackNoUpload rnu;

    private final HashMap<ServerPlayer, Long> pending = new HashMap<>();

    public FabricListener(ResourcePackNoUpload rnu) {
        this.rnu = rnu;

        rnu.scheduler().repeatAsync(
                () -> rnu.getServer().executeBlocking(this::checkPending),
                rnu.config().resendingDelay(), rnu.config().resendingDelay()
        );
    }

    private void checkPending() {
        if (rnu.resourcePackState() instanceof ResourcePackState.Loaded) {
            ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) rnu.resourcePackState();
            long currentTime = System.currentTimeMillis();

            for (Map.Entry<ServerPlayer, Long> entry : pending.entrySet()) {
                if (currentTime - entry.getValue() > 1000) {
                    addPending(entry.getKey(), loaded.resourcePackInfo());
                }
            }
        }
    }

    private void addPending(ServerPlayer player, ResourcePackInfo resourcePackInfo) {
        pending.put(player, System.currentTimeMillis());
        player.connection.send(new ClientboundResourcePackPacket(
                resourcePackInfo.uri(),
                resourcePackInfo.hashStr(),
                false,
                rnu.text(resourcePackInfo.prompt())
        ));
    }

    public void onPlayerJoin(ServerPlayer player) {
        rnu.scheduler().runLaterAsync(() -> rnu.getServer().executeBlocking(() -> {
            if (!pending.containsKey(player) && rnu.resourcePackState() instanceof ResourcePackState.Loaded) {
                ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) rnu.resourcePackState();
                addPending(player, loaded.resourcePackInfo());
            }
        }), rnu.config().sendingDelay());
    }

    public void onPlayerQuit(ServerPlayer player) {
        pending.remove(player);
    }

    public void onPlayerResourcePackStatus(ServerPlayer player, ServerboundResourcePackPacket.Action action) {
        if (pending.remove(player) != null) {
            RNUConfig config = rnu.config();

            switch (action.name()) {
                // Intermediates called here.
                case "ACCEPTED":
                case "DOWNLOADED":
                    break;
                case "DECLINED": {
                    if (config.kickOnRefuseMessage() != null) {
                        player.connection.disconnect(rnu.text(config.kickOnRefuseMessage()));
                    }
                    break;
                }
                case "INVALID_URL":
                case "FAILED_DOWNLOAD":
                case "FAILED_RELOAD":
                case "DISCARDED": {
                    if (config.kickOnFailMessage() != null) {
                        player.connection.disconnect(rnu.text(config.kickOnFailMessage().replace("<error_code>", action.name())));
                    }
                    break;
                }
                case "SUCCESSFULLY_LOADED":
                    // All ok.
                    break;
                default:
                    throw new IllegalStateException(
                            "Invalid state. Is " + rnu.getName() + " outdated?"
                    );
            }
        }
    }

    public void onRNUPackLoaded(ResourcePackInfo resourcePackInfo) {
        for (ServerPlayer player : rnu.getServer().getPlayerList().getPlayers()) {
            addPending(player, resourcePackInfo);
        }
    }
}
