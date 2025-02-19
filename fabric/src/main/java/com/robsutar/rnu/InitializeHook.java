package com.robsutar.rnu;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.robsutar.rnu.fabric.FabricListener;
import com.robsutar.rnu.fabric.RNUCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
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

        new FabricListener(this).register();
        CommandRegistrationCallback.EVENT.register((dispatcher, environment) -> {
            RNUCommand command = new RNUCommand(this, "resourcepacknoupload");
            LiteralCommandNode<CommandSourceStack> node = dispatcher.register(command);
            dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("rnu").redirect(node));
        });
    }

    public @Nullable ResourcePackNoUpload serverMod() {
        return serverMod;
    }
}
