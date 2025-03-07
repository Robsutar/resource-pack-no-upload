package com.robsutar.rnu;

import com.robsutar.rnu.bukkit.BukkitListener;
import com.robsutar.rnu.bukkit.RNUCommand;
import com.robsutar.rnu.bukkit.RNUPackLoadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class ResourcePackNoUpload extends JavaPlugin implements IResourcePackNoUploadInternal {
    private final Impl impl = new Impl(this);

    @Override
    public void onEnable() {
        impl.onEnable();
    }

    @Override
    public void onDisable() {
        impl.onDisable();
    }

    @Override
    public void onInitialConfigLoaded() {
        Bukkit.getPluginManager().registerEvents(new BukkitListener(this), this);
        Objects.requireNonNull(getCommand("resourcepacknoupload")).setExecutor(new RNUCommand(this));
    }

    @Override
    public void runInMain(Runnable runnable) {
        Bukkit.getScheduler().runTask(this, runnable);
    }

    @Override
    public void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(this, runnable);
    }

    @Override
    public void onPackLoaded(ResourcePackInfo resourcePackInfo) {
        Bukkit.getPluginManager().callEvent(new RNUPackLoadedEvent(resourcePackInfo));
    }

    @Override
    public String getServerIp() {
        return Bukkit.getIp();
    }

    @Override
    public Impl impl() {
        return impl;
    }
}
