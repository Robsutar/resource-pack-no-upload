package com.robsutar.rnu.velocity;

import com.robsutar.rnu.RNUConfig;
import com.robsutar.rnu.ResourcePackInfo;
import com.robsutar.rnu.ResourcePackNoUpload;
import com.robsutar.rnu.ResourcePackState;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.proxy.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class VelocityListener {
    private final ResourcePackNoUpload plugin;
    private final BiConsumer<Player, ResourcePackInfo> setResourcePackFunction;

    private final HashMap<Player, Long> pending = new HashMap<>();

    public VelocityListener(ResourcePackNoUpload plugin) {
        this.plugin = plugin;
        setResourcePackFunction = extractResourcePackFunction();

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
        setResourcePackFunction.accept(player, resourcePackInfo);
    }

    @Subscribe(order = PostOrder.LAST)
    public void onPlayerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();

        if (plugin.resourcePackState() instanceof ResourcePackState.Loaded) {
            ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) plugin.resourcePackState();
            addPending(player, loaded.resourcePackInfo());
        }
    }

    @Subscribe(order = PostOrder.LAST)
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
                case "SUCCESSFULLY_LOADED":
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

    @SuppressWarnings("JavaReflectionMemberAccess")
    private BiConsumer<Player, ResourcePackInfo> extractResourcePackFunction() {
        String setResourcePackError = "Target platform failed method call. Is " + plugin.getName() + " outdated?";
        Class<Player> pClass = Player.class;
        BiConsumer<Player, ResourcePackInfo> setResourcePackFunction;
        try {
            Method method = pClass.getDeclaredMethod(
                    "setResourcePack", UUID.class, String.class, byte[].class, String.class, boolean.class
            );
            setResourcePackFunction = (player, resourcePackInfo) -> {
                try {
                    method.invoke(player, resourcePackInfo.id(), resourcePackInfo.uri(), resourcePackInfo.hash(), resourcePackInfo.prompt(), false);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(setResourcePackError, e);
                }
            };
        } catch (NoSuchMethodException ignored) {
            try {
                Method method = pClass.getDeclaredMethod(
                        "setResourcePack", String.class, byte[].class, String.class, boolean.class
                );
                noUniqueId();
                setResourcePackFunction = (player, resourcePackInfo) -> {
                    try {
                        method.invoke(player, resourcePackInfo.uri(), resourcePackInfo.hash(), resourcePackInfo.prompt(), false);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(setResourcePackError, e);
                    }
                };
            } catch (NoSuchMethodException ignored1) {
                try {
                    Method method = pClass.getDeclaredMethod(
                            "setResourcePack", String.class, byte[].class, boolean.class
                    );
                    noUniqueId();
                    noPrompt();
                    setResourcePackFunction = (player, resourcePackInfo) -> {
                        try {
                            method.invoke(player, resourcePackInfo.uri(), resourcePackInfo.hash(), false);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(setResourcePackError, e);
                        }
                    };
                } catch (NoSuchMethodException ignored2) {
                    try {
                        Method method = pClass.getDeclaredMethod(
                                "setResourcePack", String.class, byte[].class
                        );
                        noUniqueId();
                        noPrompt();
                        setResourcePackFunction = (player, resourcePackInfo) -> {
                            try {
                                method.invoke(player, resourcePackInfo.uri(), resourcePackInfo.hash());
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(setResourcePackError, e);
                            }
                        };
                    } catch (NoSuchMethodException ignored3) {
                        try {
                            Method method = pClass.getDeclaredMethod(
                                    "setResourcePack", String.class
                            );
                            noUniqueId();
                            noPrompt();
                            noHash();
                            setResourcePackFunction = (player, resourcePackInfo) -> {
                                try {
                                    method.invoke(player, resourcePackInfo.uri());
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new RuntimeException(setResourcePackError, e);
                                }
                            };
                        } catch (NoSuchMethodException ignored4) {
                            try {
                                Method method = pClass.getDeclaredMethod(
                                        "setTexturePack", String.class
                                );
                                noUniqueId();
                                noPrompt();
                                noHash();
                                setResourcePackFunction = (player, resourcePackInfo) -> {
                                    try {
                                        method.invoke(player, resourcePackInfo.uri());
                                    } catch (IllegalAccessException | InvocationTargetException e) {
                                        throw new RuntimeException(setResourcePackError, e);
                                    }
                                };
                            } catch (NoSuchMethodException ignored5) {
                                throw new IllegalArgumentException("No way to send resource pack to the player found in this bukkit platform/version.");
                            }
                        }
                    }
                }
            }
        }

        return setResourcePackFunction;
    }

    private void noUniqueId() {
        plugin.getLogger().warning("Resource Pack `UUID` is not supported in this bukkit platform/version.");
    }

    private void noPrompt() {
        plugin.getLogger().warning("Resource Pack `prompt` is not supported in this bukkit platform/version.");
    }

    private void noHash() {
        plugin.getLogger().warning("Resource Pack `hash` is not supported in this bukkit platform/version.");
    }

}
