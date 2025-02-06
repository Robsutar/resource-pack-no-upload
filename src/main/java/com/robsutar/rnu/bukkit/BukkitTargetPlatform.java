package com.robsutar.rnu.bukkit;

import com.robsutar.rnu.ResourcePackInfo;
import com.robsutar.rnu.TargetPlatform;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.BiConsumer;

public class BukkitTargetPlatform implements TargetPlatform {
    private final JavaPlugin plugin;
    private final BiConsumer<Player, ResourcePackInfo> setResourcePackFunction;

    @SuppressWarnings("JavaReflectionMemberAccess")
    public BukkitTargetPlatform(JavaPlugin plugin) {
        this.plugin = plugin;

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

        this.setResourcePackFunction = setResourcePackFunction;
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

    @Override
    public void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void setResourcePack(Player player, ResourcePackInfo resourcePackInfo) {
        setResourcePackFunction.accept(player, resourcePackInfo);
    }
}
