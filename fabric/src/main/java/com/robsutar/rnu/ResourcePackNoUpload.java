package com.robsutar.rnu;

import com.robsutar.rnu.fabric.FabricListener;
import com.robsutar.rnu.fabric.RNUPackLoadedCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResourcePackNoUpload implements IResourcePackNoUploadInternal {
    private final MinecraftServer server;
    private final Logger logger;
    private final SimpleScheduler scheduler;

    private final Impl impl = new Impl(this);
    private FabricListener listener;

    public ResourcePackNoUpload(MinecraftServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        scheduler = new SimpleScheduler();
    }

    public void onEnable() {
        impl.onEnable();
    }

    public void onDisable() {
        impl.onDisable();
        scheduler.closeAndCancelPending();
    }

    @Override
    public void onInitialConfigLoaded() {
        listener = new FabricListener(this);
    }

    @Override
    public void runAsync(Runnable runnable) {
        scheduler.runAsync(runnable);
    }

    @Override
    public void onPackLoaded(ResourcePackInfo resourcePackInfo) {
        RNUPackLoadedCallback.EVENT.invoker().view(resourcePackInfo);
    }

    @Override
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

    public String getName() {
        return "ResourcePackNoUpload";
    }

    @Override
    public File getDataFolder() {
        return FabricLoader.getInstance().getConfigDir().resolve(getId()).toFile();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public SimpleScheduler getScheduler() {
        return scheduler;
    }

    public Component text(String legacy) {
        return Component.nullToEmpty(legacy);
    }

    public FabricListener listener() {
        return listener;
    }

    @Override
    public @Nullable String getServerIp() {
        return server.getLocalIp();
    }

    @Override
    public Impl impl() {
        return impl;
    }
}