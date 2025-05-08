package com.robsutar.rnu.bukkit;

import com.robsutar.rnu.ResourcePackInfo;
import com.robsutar.rnu.ResourcePackNoUpload;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;

public class BukkitPlatformHandler {
    private Consumer<Runnable> runSyncFunction;

    public BukkitPlatformHandler(ResourcePackNoUpload rnu) {
        try {
            @SuppressWarnings("JavaReflectionMemberAccess")
            Method getSchedulerMethod = Bukkit.class.getMethod("getGlobalRegionScheduler");
            Object scheduler = getSchedulerMethod.invoke(null);

            Method runMethod = scheduler.getClass().getMethod("execute", Plugin.class, Runnable.class);

            runSyncFunction = (runnable) -> {
                try {
                    runMethod.invoke(scheduler, rnu, runnable);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            runSyncFunction = (runnable) -> Bukkit.getScheduler().runTask(rnu, runnable);
        }
    }

    public void runSync(Runnable runnable) {
        runSyncFunction.accept(runnable);
    }

    public void injectPackInServer(ResourcePackInfo resourcePackInfo, boolean required) {
        try {
            Object parent = Bukkit.getServer();
            Class<?> parentClass = parent.getClass();
            Field dedicatedServerField = parentClass.getDeclaredField("console");
            dedicatedServerField.setAccessible(true);
            Object dedicatedServer = dedicatedServerField.get(parent);

            Class<?> serverClass = dedicatedServer.getClass();
            Field settingsField = serverClass.getDeclaredField("settings");
            settingsField.setAccessible(true);
            Object settings = settingsField.get(dedicatedServer);

            Class<?> settingsClass = settings.getClass();
            Field propertiesField = settingsClass.getDeclaredField("properties");
            propertiesField.setAccessible(true);
            Object properties = propertiesField.get(settings);

            Class<?> propertiesClass = properties.getClass();
            Method getPackInfo = propertiesClass.getDeclaredMethod(
                    "getServerPackInfo",
                    String.class, String.class, String.class, String.class, boolean.class, String.class
            );
            getPackInfo.setAccessible(true);
            Optional<?> newInfo = (Optional<?>) getPackInfo.invoke(
                    null,
                    resourcePackInfo.id().toString(),
                    resourcePackInfo.uri(),
                    resourcePackInfo.hashStr(),
                    null,
                    optional,
                    "{\"text\": \"Accept our server resource pack!\"}" // TODO:
                    required,
            );

            Field infoField = propertiesClass.getDeclaredField("serverResourcePackInfo");
            infoField.setAccessible(true);
            infoField.set(properties, newInfo);
        } catch (Exception e) {
            throw new RuntimeException("Could not inject resource pack info in server properties.", e);
        }
    }
}
