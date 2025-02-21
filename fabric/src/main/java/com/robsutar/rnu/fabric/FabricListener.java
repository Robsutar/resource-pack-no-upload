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
    private final ResourcePackNoUpload serverMod;

    private final HashMap<ServerPlayer, Long> pending = new HashMap<>();

    public FabricListener(ResourcePackNoUpload serverMod) {
        this.serverMod = serverMod;

        serverMod.getScheduler().repeat(this::checkPending, serverMod.config().resendingDelay(), serverMod.config().resendingDelay());
    }

    private void checkPending() {
        if (serverMod.resourcePackState() instanceof ResourcePackState.Loaded) {
            ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) serverMod.resourcePackState();
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
                serverMod.text(resourcePackInfo.prompt())
        ));
    }

    public void onPlayerJoin(ServerPlayer player) {
        serverMod.getScheduler().runLater(() -> {
            if (!pending.containsKey(player) && serverMod.resourcePackState() instanceof ResourcePackState.Loaded) {
                ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) serverMod.resourcePackState();
                addPending(player, loaded.resourcePackInfo());
            }
        }, serverMod.config().sendingDelay());
    }

    public void onPlayerQuit(ServerPlayer player) {
        pending.remove(player);
    }

    public void onPlayerResourcePackStatus(ServerPlayer player, ServerboundResourcePackPacket.Action action) {
        if (pending.remove(player) != null) {
            RNUConfig config = serverMod.config();

            switch (action.name()) {
                // Intermediates called here.
                case "ACCEPTED":
                case "DOWNLOADED":
                    break;
                case "DECLINED": {
                    if (config.kickOnRefuseMessage() != null) {
                        player.connection.disconnect(serverMod.text(config.kickOnRefuseMessage()));
                    }
                    break;
                }
                case "INVALID_URL":
                case "FAILED_DOWNLOAD":
                case "FAILED_RELOAD":
                case "DISCARDED": {
                    if (config.kickOnFailMessage() != null) {
                        player.connection.disconnect(serverMod.text(config.kickOnFailMessage().replace("<error_code>", action.name())));
                    }
                    break;
                }
                case "SUCCESSFULLY_LOADED":
                    // All ok.
                    break;
                default:
                    throw new IllegalStateException(
                            "Invalid state. Is " + serverMod.getName() + " outdated?"
                    );
            }
        }
    }

    public void onRNUPackLoaded(ResourcePackInfo resourcePackInfo) {
        for (ServerPlayer player : serverMod.getServer().getPlayerList().getPlayers()) {
            addPending(player, resourcePackInfo);
        }
    }
}
