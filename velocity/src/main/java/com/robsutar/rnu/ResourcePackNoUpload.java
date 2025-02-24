package com.robsutar.rnu;

import com.google.inject.Inject;
import com.robsutar.rnu.velocity.RNUCommand;
import com.robsutar.rnu.velocity.RNUPackLoadedEvent;
import com.robsutar.rnu.velocity.VelocityListener;
import com.robsutar.rnu.velocity.VelocityUtil;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Plugin(id = "resource-pack-no-upload",
        name = "@name@",
        version = "@version@",
        description = "Send your resource pack, without uploading it",
        authors = {"Robsutar"})
public final class ResourcePackNoUpload implements TextureProviderBytes.StateProvider {
    private final ProxyServer server;
    private final Logger logger;

    private TextureProviderBytes textureProviderBytes;
    private RNUConfig config;
    private ResourcePackState resourcePackState = new ResourcePackState.FailedToLoad();

    @Inject
    public ResourcePackNoUpload(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        InetAddress hostAddress = getServer().getBoundAddress().getAddress();
        @Nullable String serverIp = (!hostAddress.isAnyLocalAddress()) ? hostAddress.getHostAddress() : null;
        textureProviderBytes = TextureProviderBytes.deserialize(
                this,
                serverIp,
                VelocityUtil.loadOrCreateConfig(this, "server.yml")
        );

        ResourcePackState.Loaded loaded;
        try {
            loaded = load();
            resourcePackState = new ResourcePackState.LoadedPendingProvider(loaded);
        } catch (ResourcePackLoadException e) {
            throw new RuntimeException("Initial loading failed, and the initial configuration could not be loaded, disabling plugin.", e);
        }

        server.getEventManager().register(this, new VelocityListener(this));

        CommandMeta commandMeta = server.getCommandManager().metaBuilder("resourcepacknoupload")
                .aliases("rnu")
                .plugin(this)
                .build();

        server.getCommandManager().register(commandMeta, new RNUCommand(this));

        server.getScheduler().buildTask(this, () -> {
            try {
                textureProviderBytes.run(() -> {
                    resourcePackState = loaded;
                    getLogger().info("Resource pack provider bind address: " + textureProviderBytes.address());
                    getLogger().info("Resource pack provider bind uri: " + textureProviderBytes.uri());
                });
            } catch (Exception e) {
                throw new IllegalStateException("Failed to bind texture provider bytes", e);
            }
        }).schedule();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        textureProviderBytes.close();
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
                configRaw = VelocityUtil.loadOrCreateConfig(this, "config.yml");
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

            getLogger().info("Resource Pack loaded, link: " + resourcePackInfo.uri());
            server.getEventManager().fire(new RNUPackLoadedEvent(resourcePackInfo));

            return newState;
        } catch (ResourcePackLoadException e) {
            resourcePackState = new ResourcePackState.FailedToLoad();
            throw e;
        } catch (Exception e) {
            resourcePackState = new ResourcePackState.FailedToLoad();
            throw new ResourcePackLoadException("Unexpected and unknown error", e);
        }
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getName() {
        return "@name@";
    }

    public File getDataFolder() {
        return new File("plugins/" + getName() + "/");
    }

    public void saveResource(@NotNull String resourcePath, boolean replace) {
        if (resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        File dataFolder = getDataFolder();

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + getName());
        }

        File outFile = new File(dataFolder, resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(dataFolder, resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists() && !outDir.mkdirs()) throw new IllegalStateException();

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = Files.newOutputStream(outFile.toPath());
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                logger.log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    @Nullable
    public InputStream getResource(@NotNull String filename) {
        try {
            URL url = getClass().getClassLoader().getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    public Component text(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }

    public TextureProviderBytes textureProviderBytes() {
        return textureProviderBytes;
    }

    public RNUConfig config() {
        return config;
    }

    @Override
    public ResourcePackState resourcePackState() {
        return resourcePackState;
    }
}
