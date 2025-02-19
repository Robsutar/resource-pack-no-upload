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

    private @Nullable ResourcePackNoUpload serverMod = null;

    @Override
    public void onInitialize() {
        instance = this;

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            if (serverMod != null) throw new IllegalStateException();
            try {
                serverMod = new ResourcePackNoUpload(server, logger);
                serverMod.onEnable();
            } catch (Exception e) {
                logger.log(Level.SEVERE, e, () -> "Failed to enable ResourcePackNoUpload");
                Throwable cause = e.getCause();
                if (cause != null) {
                    logger.log(Level.SEVERE, cause, () -> "Descend logs due the enabling fail:");
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
            if (serverMod == null) return;
            serverMod.onDisable();
            serverMod = null;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, environment) -> {
            RNUCommand command = new RNUCommand(this, "resourcepacknoupload");
            LiteralCommandNode<CommandSourceStack> node = dispatcher.register(command);
            dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("rnu")
                    .requires(source -> source.hasPermission(2))
                    .redirect(node));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (serverMod == null) return;
            ServerPlayer player = handler.player;
            serverMod.listener().onPlayerJoin(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (serverMod == null) return;
            ServerPlayer player = handler.player;
            serverMod.listener().onPlayerQuit(player);
        });

        RNUPackLoadedCallback.EVENT.register((resourcePackInfo) -> {
            if (serverMod == null) return;
            // In the first server mod loading, the event is called before the listener
            if (serverMod.listener() == null) return;
            serverMod.listener().onRNUPackLoaded(resourcePackInfo);
        });
    }

    public @Nullable ResourcePackNoUpload serverMod() {
        return serverMod;
    }

    public static InitializeHook instance() {
        return instance;
    }
}
