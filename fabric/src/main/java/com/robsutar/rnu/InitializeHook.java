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

import java.util.Objects;

public class InitializeHook implements ModInitializer {
    private static InitializeHook instance;

    private @Nullable ResourcePackNoUpload serverMod = null;

    @Override
    public void onInitialize() {
        instance = this;

        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            if (serverMod != null) throw new IllegalStateException();
            serverMod = new ResourcePackNoUpload(server);
            serverMod.onEnable();
        });

        ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
            Objects.requireNonNull(serverMod).onDisable();
            serverMod = null;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, environment) -> {
            RNUCommand command = new RNUCommand(this, "resourcepacknoupload");
            LiteralCommandNode<CommandSourceStack> node = dispatcher.register(command);
            dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("rnu").redirect(node));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            Objects.requireNonNull(serverMod).listener().onPlayerJoin(player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            Objects.requireNonNull(serverMod).listener().onPlayerQuit(player);
        });

        RNUPackLoadedCallback.EVENT.register((resourcePackInfo) -> {
            Objects.requireNonNull(serverMod).listener().onRNUPackLoaded(resourcePackInfo);
        });
    }

    public @Nullable ResourcePackNoUpload serverMod() {
        return serverMod;
    }

    public static InitializeHook instance() {
        return instance;
    }
}
