package com.robsutar.rnu.bukkit;

import com.robsutar.rnu.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public class BukkitListener implements Listener {
    private final ResourcePackNoUpload rnu;

    private final BiConsumer<Player, ResourcePackInfo> setResourcePackFunction;
    private final HashMap<Player, Long> pending = new HashMap<>();

    public BukkitListener(ResourcePackNoUpload rnu) {
        this.rnu = rnu;
        setResourcePackFunction = extractResourcePackFunction();
    }

    public void register() {
        if (rnu.serverConfig().sender() instanceof ResourcePackSender.Delayed) {
            ResourcePackSender.Delayed delayed = (ResourcePackSender.Delayed) rnu.serverConfig().sender();
            rnu.scheduler().repeatAsync(() -> rnu.runSync(() -> {
                if (rnu.resourcePackState() instanceof ResourcePackState.Loaded) {
                    ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) rnu.resourcePackState();
                    long currentTime = System.currentTimeMillis();

                    for (Map.Entry<Player, Long> entry : pending.entrySet()) {
                        if (currentTime - entry.getValue() > 1000) {
                            addPending(entry.getKey(), loaded.resourcePackInfo());
                        }
                    }
                }
            }), delayed.resendingDelay());
        } else if (rnu.serverConfig().sender() instanceof ResourcePackSender.PaperPropertyInjector) {
            ResourcePackState.LoadedPendingProvider loaded = (ResourcePackState.LoadedPendingProvider) rnu.resourcePackState();
            rnu.platformHandler().injectPackInServer(loaded.loaded().resourcePackInfo(), rnu.config().kickOnFailMessage() != null);
        } else {
            throw new RuntimeException("Loader not supported in this platform: " + rnu.serverConfig().sender().type());
        }
        Bukkit.getPluginManager().registerEvents(this, rnu);
    }

    private void addPending(Player player, ResourcePackInfo resourcePackInfo) {
        pending.put(player, System.currentTimeMillis());
        setResourcePackFunction.accept(player, resourcePackInfo);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (rnu.serverConfig().sender() instanceof ResourcePackSender.Delayed) {
            ResourcePackSender.Delayed delayed = (ResourcePackSender.Delayed) rnu.serverConfig().sender();
            rnu.scheduler().runLaterAsync(() -> rnu.runSync(() -> {
                Player player = event.getPlayer();
                if (!pending.containsKey(player) && rnu.resourcePackState() instanceof ResourcePackState.Loaded) {
                    ResourcePackState.Loaded loaded = (ResourcePackState.Loaded) rnu.resourcePackState();
                    addPending(player, loaded.resourcePackInfo());
                }
            }), delayed.resendingDelay());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        pending.remove(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        if (pending.remove(player) != null) rnu.runSync(() -> {
            RNUConfig config = rnu.config();
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
                            "Invalid state. Is " + rnu.getName() + " outdated?"
                    );
            }
        });
    }

    @EventHandler
    public void onRNUPackLoaded(RNUPackLoadedEvent event) {
        if (rnu.serverConfig().sender() instanceof ResourcePackSender.PaperPropertyInjector) {
            rnu.platformHandler().injectPackInServer(event.getResourcePackInfo(), rnu.config().kickOnFailMessage() != null);
        }

        ResourcePackInfo resourcePackInfo = event.getResourcePackInfo();
        for (Player player : Bukkit.getOnlinePlayers()) {
            addPending(player, resourcePackInfo);
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private BiConsumer<Player, ResourcePackInfo> extractResourcePackFunction() {
        String setResourcePackError = "Target platform failed method call. Is " + rnu.getName() + " outdated?";
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
        rnu.getLogger().warning("Resource Pack `UUID` is not supported in this bukkit platform/version.");
    }

    private void noPrompt() {
        rnu.getLogger().warning("Resource Pack `prompt` is not supported in this bukkit platform/version.");
    }

    private void noHash() {
        rnu.getLogger().warning("Resource Pack `hash` is not supported in this bukkit platform/version.");
    }
}
