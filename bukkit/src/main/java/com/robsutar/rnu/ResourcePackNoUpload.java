package com.robsutar.rnu;

import com.robsutar.rnu.bukkit.BukkitListener;
import com.robsutar.rnu.bukkit.BukkitPlatformHandler;
import com.robsutar.rnu.bukkit.RNUCommand;
import com.robsutar.rnu.bukkit.RNUPackLoadedEvent;
import com.robsutar.rnu.util.OC;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public final class ResourcePackNoUpload extends JavaPlugin implements IResourcePackNoUploadInternal {
    private final Impl impl = new Impl(this);
    private final BukkitPlatformHandler platformHandler = new BukkitPlatformHandler(this);
    private final BukkitListener listener = new BukkitListener(this);

    @Override
    public void onEnable() {
        impl.onEnable();
    }

    @Override
    public void onDisable() {
        impl.onDisable();
    }

    @Override
    public void onInitialConfigLoaded() {
        loadAutoReloading();
        listener.register();
        Objects.requireNonNull(getCommand("resourcepacknoupload")).setExecutor(new RNUCommand(this));
    }

    @Override
    public void onPackLoaded(ResourcePackInfo resourcePackInfo) {
        Bukkit.getPluginManager().callEvent(new RNUPackLoadedEvent(resourcePackInfo));
    }

    @Override
    public String getServerIp() {
        return Bukkit.getIp();
    }

    @Override
    public Impl impl() {
        return impl;
    }

    public void runSync(Runnable runnable) {
        platformHandler.runSync(runnable);
    }

    private void loadAutoReloading() {
        Map<String, List<Map<String, Object>>> config = loadOrCreateConfig("autoReloading.yml");
        List<Map<String, Object>> invokersRaw = OC.list(config.get("invokers"));

        List<AutoReloadingInvoker<Event>> invokers = invokersRaw.stream()
                .map(AutoReloadingInvoker::<Event>deserialize)
                .collect(Collectors.toList());

        for (AutoReloadingInvoker<Event> invoker : invokers) {
            Duration repeatCooldown = Duration.ofMillis(invoker.repeatCooldown() * 50L);
            AtomicReference<Instant> lastUsed = new AtomicReference<>(Instant.now().minus(repeatCooldown));
            Bukkit.getPluginManager().registerEvent(
                    invoker.eventClass(),
                    listener,
                    EventPriority.HIGH,
                    (l, b) -> scheduler().runLaterAsync(
                            () -> {
                                Instant now = Instant.now();
                                if (Duration.between(lastUsed.get(), now).compareTo(repeatCooldown) > 0 &&
                                        !(resourcePackState() instanceof ResourcePackState.Loading)
                                ) {
                                    lastUsed.set(now);
                                    runSync(() -> Bukkit.dispatchCommand(
                                            Bukkit.getConsoleSender(), "rnu reload"
                                    ));
                                }
                            },
                            invoker.delay()),
                    this
            );
        }
    }
}
