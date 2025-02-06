package com.robsutar.rnu;

import com.robsutar.rnu.bukkit.BukkitListener;
import com.robsutar.rnu.bukkit.RNUCommand;
import com.robsutar.rnu.bukkit.RNUPackLoadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

public final class ResourcePackNoUpload extends JavaPlugin {
    private RNUConfig config;
    private ResourcePackState resourcePackState = new ResourcePackState.FailedToLoad();

    @Override
    public void onEnable() {
        ResourcePackState.Loaded loaded;
        try {
            loaded = load();
            resourcePackState = new ResourcePackState.LoadedPendingProvider(loaded);
        } catch (ResourcePackLoadException e) {
            throw new RuntimeException("Initial loading failed, and the initial configuration could not be loaded, disabling plugin.", e);
        }

        Bukkit.getPluginManager().registerEvents(new BukkitListener(this), this);
        Objects.requireNonNull(getCommand("resourcepacknoupload")).setExecutor(new RNUCommand(this));

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                new TextureProviderBytes(config.address()) {
                    @Override
                    public ResourcePackState state() {
                        return resourcePackState;
                    }
                }.run(() -> Bukkit.getScheduler().runTask(this, () -> {
                    resourcePackState = loaded;
                    getLogger().info("Resource pack provider bind address: " + config.address());
                    getLogger().info("Resource pack provider bind uri: " + config.uri());
                }));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to bind texture provider bytes", e);
            }
        });
    }

    public ResourcePackState.Loaded load() throws ResourcePackLoadException {
        try {
            if (resourcePackState instanceof ResourcePackState.Loading)
                throw new ResourcePackLoadException("Already loading");
            resourcePackState = new ResourcePackState.Loading();

            File folder = getDataFolder();
            if (!folder.exists() && !folder.mkdir())
                throw new ResourcePackLoadException("Failed to create plugin folder");

            YamlConfiguration configRaw = new YamlConfiguration();
            File configFile = new File(folder, "config.yml");
            if (!configFile.exists()) {
                saveResource("config.yml", false);
            }
            try {
                configRaw.load(configFile);
            } catch (IOException | InvalidConfigurationException e) {
                throw new ResourcePackLoadException("Failed to load configuration file");
            }
            try {
                config = RNUConfig.deserialize(configRaw);
            } catch (Exception e) {
                throw new ResourcePackLoadException("Failed to deserialize configuration from file");
            }

            byte[] bytes;
            try {
                bytes = config.loader().load();
            } catch (Exception e) {
                throw new ResourcePackLoadException("Loader failed to load resource pack", e);
            }

            String hashStr;
            try {
                byte[] hash = MessageDigest.getInstance("SHA-1").digest(bytes);
                StringBuilder sha1Hash = new StringBuilder();
                for (byte hashedByte : hash) {
                    sha1Hash.append(Integer.toString((hashedByte & 0xff) + 0x100, 16).substring(1));
                }
                hashStr = sha1Hash.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new ResourcePackLoadException("Failed to load SHA-1 algorithm to create texture hash.");
            }

            ResourcePackInfo resourcePackInfo = new ResourcePackInfo(UUID.randomUUID(), config().uri(), hashStr);

            ResourcePackState.Loaded newState = new ResourcePackState.Loaded(resourcePackInfo, bytes);
            resourcePackState = newState;

            Bukkit.getPluginManager().callEvent(new RNUPackLoadedEvent(resourcePackInfo));

            return newState;
        } catch (ResourcePackLoadException e) {
            resourcePackState = new ResourcePackState.FailedToLoad();
            throw e;
        } catch (Exception e) {
            resourcePackState = new ResourcePackState.FailedToLoad();
            throw new ResourcePackLoadException("Unexpected and unknown error", e);
        }
    }

    public RNUConfig config() {
        return config;
    }

    public ResourcePackState resourcePackState() {
        return resourcePackState;
    }
}
