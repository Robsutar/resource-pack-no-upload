package com.robsutar.rnu;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class InitializeHook implements ModInitializer {
    private @Nullable ResourcePackNoUpload serverMod = null;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            if (serverMod != null) throw new IllegalStateException();
            serverMod = new ResourcePackNoUpload(server);
            serverMod.onEnable();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
            Objects.requireNonNull(serverMod).onDisable();
            serverMod = null;
        });
    }

    public @Nullable ResourcePackNoUpload serverMod() {
        return serverMod;
    }
}
