package com.robsutar.rnu;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

public interface IResourcePackNoUploadInternal {
    File getDataFolder();

    Logger getLogger();

    @Nullable String getServerIp();

    Impl impl();

    void saveResource(@NotNull String resourcePath, boolean replace);

    void onInitialConfigLoaded();

    void runInMain(Runnable runnable);

    void runAsync(Runnable runnable);

    void onPackLoaded(ResourcePackInfo resourcePackInfo);

    default TextureProviderBytes textureProviderBytes() {
        return impl().textureProviderBytes;
    }

    default RNUConfig config() {
        return impl().config;
    }

    default ResourcePackState resourcePackState() {
        return impl().resourcePackState;
    }

    default ResourcePackState.Loaded load() throws ResourcePackLoadException {
        return impl().load();
    }

    default <K, V> Map<K, V> loadOrCreateConfig(String fileName) throws IllegalStateException {
        IResourcePackNoUploadInternal rnu = impl().rnu;
        File folder = rnu.getDataFolder();
        if (!folder.exists() && !folder.mkdir())
            throw new IllegalStateException("Failed to create RNU folder");

        File configFile = new File(folder, fileName);
        if (!configFile.exists()) {
            rnu.saveResource(fileName, false);
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(configFile.toPath()), StandardCharsets.UTF_8)) {
            Map<K, V> mapToAdd = Impl.YAML.load(reader);
            if (mapToAdd == null) {
                throw new IllegalArgumentException("null/empty yml: " + configFile);
            }
            return mapToAdd;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    class Impl {
        private static final Yaml YAML = new Yaml();

        private final IResourcePackNoUploadInternal rnu;
        private TextureProviderBytes textureProviderBytes;
        private RNUConfig config;
        private ResourcePackState resourcePackState;

        public Impl(IResourcePackNoUploadInternal rnu) {
            this.rnu = rnu;
        }

        public void onEnable() {
            textureProviderBytes = TextureProviderBytes.deserialize(
                    rnu,
                    rnu.getServerIp(),
                    rnu.loadOrCreateConfig("server.yml")
            );

            ResourcePackState.Loaded loaded;
            try {
                loaded = load();
                resourcePackState = new ResourcePackState.LoadedPendingProvider(loaded);
            } catch (ResourcePackLoadException e) {
                throw new RuntimeException("Initial loading failed, and the initial configuration could not be loaded, disabling RNU.", e);
            }

            rnu.onInitialConfigLoaded();

            rnu.runAsync(() -> {
                try {
                    textureProviderBytes.run(() -> rnu.runInMain(() -> {
                        resourcePackState = loaded;
                        rnu.getLogger().info("Resource pack provider initialized, its link is now available.");
                    }));
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to bind texture provider bytes", e);
                }
            });
        }

        public void onDisable() {
            textureProviderBytes.close();
        }

        public ResourcePackState.Loaded load() throws ResourcePackLoadException {
            try {
                if (resourcePackState instanceof ResourcePackState.Loading)
                    throw new ResourcePackLoadException("Already loading");
                resourcePackState = new ResourcePackState.Loading();

                File tempFolder = new File(rnu.getDataFolder(), "temp");

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
                    configRaw = rnu.loadOrCreateConfig("config.yml");
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

                rnu.getLogger().info("Resource Pack loaded, link: " + resourcePackInfo.uri());
                rnu.onPackLoaded(resourcePackInfo);

                return newState;
            } catch (ResourcePackLoadException e) {
                resourcePackState = new ResourcePackState.FailedToLoad();
                throw e;
            } catch (Exception e) {
                resourcePackState = new ResourcePackState.FailedToLoad();
                throw new ResourcePackLoadException("Unexpected and unknown error", e);
            }
        }
    }
}
