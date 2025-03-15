package com.robsutar.rnu.bukkit;

import com.robsutar.rnu.ResourcePackNoUpload;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
}
