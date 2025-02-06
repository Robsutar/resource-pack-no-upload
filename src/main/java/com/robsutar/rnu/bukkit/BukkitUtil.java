package com.robsutar.rnu.bukkit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class BukkitUtil {
    public static ConfigurationSection loadOrCreateConfig(JavaPlugin plugin, String fileName) throws IllegalStateException {
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdir())
            throw new IllegalStateException("Failed to create plugin folder");

        YamlConfiguration configRaw = new YamlConfiguration();
        File configFile = new File(folder, fileName);
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }
        try {
            configRaw.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            throw new IllegalStateException("Failed to load configuration file");
        }

        return configRaw;
    }
}
