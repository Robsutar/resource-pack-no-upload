package com.robsutar.rnu;

import com.google.inject.Inject;
import com.robsutar.rnu.util.OC;
import com.robsutar.rnu.velocity.RNUCommand;
import com.robsutar.rnu.velocity.RNUPackLoadedEvent;
import com.robsutar.rnu.velocity.VelocityListener;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Plugin(id = "resource-pack-no-upload",
        name = "@name@",
        version = "@version@",
        description = "Send your resource pack, without uploading it",
        authors = {"Robsutar"})
public final class ResourcePackNoUpload implements IResourcePackNoUploadInternal {
    private final ProxyServer server;
    private final Logger logger;

    private final Impl impl = new Impl(this);
    private final VelocityListener listener = new VelocityListener(this);

    @Inject
    public ResourcePackNoUpload(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        impl.onEnable();
    }

    @Subscribe
    public void onDisable(ProxyShutdownEvent event) {
        impl.onDisable();
    }

    @Override
    public void onInitialConfigLoaded() {
        loadAutoReloading();
        listener.register();

        CommandMeta commandMeta = server.getCommandManager().metaBuilder("resourcepacknoupload")
                .aliases("rnu")
                .plugin(this)
                .build();

        server.getCommandManager().register(commandMeta, new RNUCommand(this));
    }

    @Override
    public void onPackLoaded(ResourcePackInfo resourcePackInfo) {
        server.getEventManager().fire(new RNUPackLoadedEvent(resourcePackInfo));
    }

    @Override
    public String getServerIp() {
        InetAddress hostAddress = getServer().getBoundAddress().getAddress();
        return (!hostAddress.isAnyLocalAddress()) ? hostAddress.getHostAddress() : null;
    }

    @Override
    public Impl impl() {
        return impl;
    }

    public ProxyServer getServer() {
        return server;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    public String getName() {
        return "@name@";
    }

    @Override
    public File getDataFolder() {
        return new File("plugins/" + getName() + "/");
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

    private void loadAutoReloading() {
        Map<String, List<Map<String, Object>>> config = loadOrCreateConfig("autoReloading.yml");
        List<Map<String, Object>> invokersRaw = OC.list(config.get("invokers"));

        List<AutoReloadingInvoker<Object>> invokers = invokersRaw.stream()
                .map(AutoReloadingInvoker::deserialize)
                .collect(Collectors.toList());


        for (AutoReloadingInvoker<Object> invoker : invokers) {
            Duration repeatCooldown = Duration.ofMillis(invoker.repeatCooldown() * 50L);
            AtomicReference<Instant> lastUsed = new AtomicReference<>(Instant.now().minus(repeatCooldown));
            getServer().getEventManager().register(
                    this,
                    invoker.eventClass(),
                    (e) -> scheduler().runLaterAsync(() -> {
                        Instant now = Instant.now();
                        if (Duration.between(lastUsed.get(), now).compareTo(repeatCooldown) > 0 &&
                                !(resourcePackState() instanceof ResourcePackState.Loading)
                        ) {
                            lastUsed.set(now);
                            getServer().getCommandManager().executeAsync(
                                    getServer().getConsoleCommandSource(), "rnu reload"
                            );
                        }
                    }, invoker.delay())
            );
        }
    }
}
