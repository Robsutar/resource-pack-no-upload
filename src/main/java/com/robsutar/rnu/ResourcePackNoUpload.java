package com.robsutar.rnu;

import net.kyori.adventure.resource.ResourcePackInfo;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.logging.Level;

public final class ResourcePackNoUpload extends JavaPlugin {
    private RNUConfig config;
    private ResourcePackState resourcePackState = new ResourcePackState.Loading();

    @Override
    public void onEnable() {
        try {
            load();
        } catch (ResourcePackLoadException e) {
            if (config == null) {
                throw new RuntimeException("""
                        Initial loading failed, and the initial configuration could not be loaded, disabling plugin.
                        """, e);
            } else {
                getLogger().log(Level.SEVERE, e, () -> "Failed to apply resource pack loader, fix the problems, and try load it again with `/rsu reload`.");
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                new TextureProviderBytes(config.address()) {
                    @Override
                    public ResourcePackState state() {
                        return resourcePackState;
                    }
                }.run();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to bind texture provider bytes", e);
            }
        });
    }

    public void load() throws ResourcePackLoadException {
        try {
            if (resourcePackState instanceof ResourcePackState.Loading)
                throw new ResourcePackLoadException("Already loading");
            resourcePackState = new ResourcePackState.Loading();

            var folder = getDataFolder();
            if (!folder.exists() && !folder.mkdir())
                throw new ResourcePackLoadException("Failed to create plugin folder");

            var configRaw = new YamlConfiguration();
            var configFile = new File(folder, "config.yml");
            if (!configFile.exists()) {
                saveResource("config.yml", false);
            }
            try {
                configRaw.load(configFile);
            } catch (IOException | InvalidConfigurationException e) {
                throw new ResourcePackLoadException("Failed to load configuration file");
            }
            config = RNUConfig.deserialize(configRaw);

            byte[] bytes;
            try {
                bytes = config.loader().load();
            } catch (Exception e) {
                throw new ResourcePackLoadException("Loader failed to load resource pack", e);
            }

            String hashStr;
            try {
                var hash = MessageDigest.getInstance("SHA-1").digest(bytes);
                var sha1Hash = new StringBuilder();
                for (var hashedByte : hash) {
                    sha1Hash.append(Integer.toString((hashedByte & 0xff) + 0x100, 16).substring(1));
                }
                hashStr = sha1Hash.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new ResourcePackLoadException("Failed to load SHA-1 algorithm to create texture hash.");
            }

            var resourcePackInfo = ResourcePackInfo.resourcePackInfo(UUID.randomUUID(), config().uri(), hashStr);

            resourcePackState = new ResourcePackState.Loaded(resourcePackInfo, bytes);
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
