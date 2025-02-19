package com.robsutar.rnu;

import com.robsutar.rnu.bukkit.BukkitListener;
import com.robsutar.rnu.bukkit.BukkitUtil;
import com.robsutar.rnu.bukkit.RNUCommand;
import com.robsutar.rnu.bukkit.RNUPackLoadedEvent;
import com.robsutar.rnu.util.OC;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public final class ResourcePackNoUpload extends JavaPlugin {
    private TextureProviderBytes textureProviderBytes;
    private RNUConfig config;
    private ResourcePackState resourcePackState = new ResourcePackState.FailedToLoad();

    @Override
    public void onEnable() {
        textureProviderBytes = loadTextureProviderBytes();

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
                textureProviderBytes.run(() -> Bukkit.getScheduler().runTask(this, () -> {
                    resourcePackState = loaded;
                    getLogger().info("Resource pack provider bind address: " + textureProviderBytes.address());
                    getLogger().info("Resource pack provider bind uri: " + textureProviderBytes.uri());
                }));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to bind texture provider bytes", e);
            }
        });
    }

    private TextureProviderBytes loadTextureProviderBytes() {
        Map<String, Object> raw = BukkitUtil.loadOrCreateConfig(this, "server.yml");

        String addressStr;
        if (raw.get("serverAddress") != null) addressStr = OC.str(raw.get("serverAddress"));
        else {
            String definedIp = Bukkit.getIp();
            if (!definedIp.isEmpty()) addressStr = definedIp;
            else try {
                addressStr = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Failed to get server address from program ipv4.");
            }
        }

        if (raw.get("port") == null)
            throw new IllegalArgumentException(
                    "Port undefined in configuration!\n" +
                            "Define it in plugins/ResourcePackNoUpload/server.yml\n" +
                            "Make sure to open this port to the players.\n"
            );
        int port = OC.intValue(raw.get("port"));

        return new TextureProviderBytes(addressStr, port) {
            @Override
            public ResourcePackState state() {
                return resourcePackState;
            }
        };
    }

    public ResourcePackState.Loaded load() throws ResourcePackLoadException {
        try {
            if (resourcePackState instanceof ResourcePackState.Loading)
                throw new ResourcePackLoadException("Already loading");
            resourcePackState = new ResourcePackState.Loading();

            File tempFolder = new File(getDataFolder(), "temp");

            if (tempFolder.exists()) try (Stream<Path> walk = Files.walk(tempFolder.toPath())) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            } catch (Exception e) {
                throw new ResourcePackLoadException("Failed to delete temp folder.", e);
            }

            if (!tempFolder.mkdir())
                throw new ResourcePackLoadException("Failed to create temp folder.");

            Map<String, Object> configRaw;

            try {
                configRaw = BukkitUtil.loadOrCreateConfig(this, "config.yml");
            } catch (IllegalStateException e) {
                throw new ResourcePackLoadException("Failed to load configuration file.", e);
            }
            try {
                config = RNUConfig.deserialize(tempFolder, configRaw);
            } catch (Exception e) {
                throw new ResourcePackLoadException("Failed to deserialize configuration from file", e);
            }

            byte[] bytes;
            try {
                bytes = config.loader().load();
            } catch (Exception e) {
                throw new ResourcePackLoadException("Loader failed to load resource pack", e);
            }

            byte[] hash;
            try {
                hash = MessageDigest.getInstance("SHA-1").digest(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new ResourcePackLoadException("Failed to load SHA-1 algorithm to create texture hash.");
            }

            ResourcePackInfo resourcePackInfo = new ResourcePackInfo(
                    UUID.randomUUID(),
                    hash,
                    config.prompt(),
                    textureProviderBytes.uri()
            );

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

    public TextureProviderBytes textureProviderBytes() {
        return textureProviderBytes;
    }

    public RNUConfig config() {
        return config;
    }

    public ResourcePackState resourcePackState() {
        return resourcePackState;
    }
}
