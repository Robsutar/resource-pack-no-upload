package com.robsutar.rnu;

import net.kyori.adventure.resource.ResourcePackInfo;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public final class ResourcePackNoUpload extends JavaPlugin {
    private RNUConfig config;

    @Override
    public void onEnable() {
        load();
    }

    public void load() {
        var folder = getDataFolder();
        if (!folder.exists()&&!folder.mkdir())
            throw new IllegalStateException("Failed to create plugin folder");

        var configRaw = new YamlConfiguration();
        var configFile = new File(folder, "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        try {
            configRaw.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalStateException("Failed to load configuration file");
        }
        config = RNUConfig.deserialize(configRaw);
    }

    public RNUConfig config() {
        return config;
    }

    public @Nullable ResourcePackInfo actualResourcePackInfo() {

    }
}
