package com.robsutar.rnu;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.robsutar.rnu.fabric.RNUCommand;
import com.robsutar.rnu.fabric.RNUPackLoadedCallback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

public class InitializeHook implements ModInitializer {
    private static InitializeHook instance;

    private final Logger logger = Logger.getLogger(ResourcePackNoUpload.class.getName());

    private @Nullable ResourcePackNoUpload rnu = null;

    @Override
    public void onInitialize() {
        instance = this;

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            if (rnu != null) throw new IllegalStateException();
            try {
                rnu = new ResourcePackNoUpload(server, logger);
                rnu.onEnable();
            } catch (Exception e) {
                logger.log(Level.SEVERE, e, () -> "Failed to enable ResourcePackNoUpload");
                Throwable cause = e.getCause();
                if (cause != null) {
                    logger.log(Level.SEVERE, cause, () -> "Descend logs due the enabling fail:");
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
            if (rnu == null) return;
            rnu.onDisable();
            rnu = null;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, environment) -> {
            RNUCommand command = new RNUCommand(this, "resourcepacknoupload");
            LiteralCommandNode<CommandSourceStack> node = dispatcher.register(command);
            dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("rnu")
                    .requires(source -> source.hasPermission(2))
                    .redirect(node));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (rnu == null) return;
            ServerPlayer player = handler.player;
            rnu.listener().onPlayerJoin(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (rnu == null) return;
            ServerPlayer player = handler.player;
            rnu.listener().onPlayerQuit(player);
        });

        RNUPackLoadedCallback.EVENT.register((resourcePackInfo) -> {
            if (rnu == null) return;
            // In the first server mod loading, the event is called before the listener
            if (rnu.listener() == null) return;
            rnu.listener().onRNUPackLoaded(resourcePackInfo);
        });
    }

    public @Nullable ResourcePackNoUpload rnu() {
        return rnu;
    }

    public static InitializeHook instance() {
        return instance;
    }
}
