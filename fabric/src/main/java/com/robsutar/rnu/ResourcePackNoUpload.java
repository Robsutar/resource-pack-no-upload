package com.robsutar.rnu;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.robsutar.rnu.fabric.FabricListener;
import com.robsutar.rnu.fabric.FabricUtil;
import com.robsutar.rnu.fabric.RNUCommand;
import com.robsutar.rnu.fabric.RNUPackLoadedCallback;
import com.robsutar.rnu.util.OC;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ResourcePackNoUpload {
    private final MinecraftServer server;
    private final Logger logger = Logger.getLogger(ResourcePackNoUpload.class.getName());

    private TextureProviderBytes textureProviderBytes;
    private RNUConfig config;
    private ResourcePackState resourcePackState = new ResourcePackState.FailedToLoad();

    public ResourcePackNoUpload(MinecraftServer server) {
        this.server = server;
    }

    public void onEnable() {
        textureProviderBytes = loadTextureProviderBytes();

        ResourcePackState.Loaded loaded;
        try {
            loaded = load();
            resourcePackState = new ResourcePackState.LoadedPendingProvider(loaded);
        } catch (ResourcePackLoadException e) {
            throw new RuntimeException("Initial loading failed, and the initial configuration could not be loaded, disabling plugin.", e);
        }

        new FabricListener(this).register();
        CommandRegistrationCallback.EVENT.register((dispatcher, environment) -> {
            RNUCommand command = new RNUCommand(this, "resourcepacknoupload");
            LiteralCommandNode<CommandSourceStack> node = dispatcher.register(command);
            dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("rnu").redirect(node));
        });

        CompletableFuture.runAsync(() -> {
            try {
                textureProviderBytes.run(() -> server.execute(() -> {
                    resourcePackState = loaded;
                    getLogger().info("Resource pack provider bind address: " + textureProviderBytes.address());
                    getLogger().info("Resource pack provider bind uri: " + textureProviderBytes.uri());
                }));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to bind texture provider bytes", e);
            }
        });
    }

    public void onDisable() {
        
    }

    private TextureProviderBytes loadTextureProviderBytes() {
        Map<String, Object> raw = FabricUtil.loadOrCreateConfig(this, "server.yml");

        String addressStr;
        if (raw.get("serverAddress") != null) addressStr = OC.str(raw.get("serverAddress"));
        else {
            @Nullable String localIp = server.getLocalIp();
            if (localIp != null && !localIp.isEmpty())
                addressStr = localIp;
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
                configRaw = FabricUtil.loadOrCreateConfig(this, "config.yml");
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
                    textureProviderBytes.uri().toString(),
                    hash,
                    config.prompt()
            );

            ResourcePackState.Loaded newState = new ResourcePackState.Loaded(resourcePackInfo, bytes);
            resourcePackState = newState;

            RNUPackLoadedCallback.EVENT.invoker().view(resourcePackInfo);

            return newState;
        } catch (ResourcePackLoadException e) {
            resourcePackState = new ResourcePackState.FailedToLoad();
            throw e;
        } catch (Exception e) {
            resourcePackState = new ResourcePackState.FailedToLoad();
            throw new ResourcePackLoadException("Unexpected and unknown error", e);
        }
    }

    public void saveResource(@NotNull String resourcePath, boolean replace) {
        if (resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        File dataFolder = getDataFolder();

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + getId());
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

    public String getId() {
        return "resourcepacknoupload";
    }

    public File getDataFolder() {
        return FabricLoader.getInstance().getConfigDir().resolve(getId()).toFile();
    }

    public Logger getLogger() {
        return logger;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public Component text(String legacy) {
        return Component.nullToEmpty(legacy); // TODO: this does not work
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