package com.robsutar.rnu.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.robsutar.rnu.InitializeHook;
import com.robsutar.rnu.ResourcePackLoadException;
import com.robsutar.rnu.ResourcePackNoUpload;
import com.robsutar.rnu.ResourcePackState;
import net.minecraft.commands.CommandSourceStack;

public class RNUCommand extends LiteralArgumentBuilder<CommandSourceStack> {
    public RNUCommand(InitializeHook hook, String literal) {
        super(literal);
        requires(source -> source.hasPermission(2));

        then(LiteralArgumentBuilder.<CommandSourceStack>literal("reload").executes((context) -> {
            ResourcePackNoUpload serverMod = hook.serverMod();
            if (serverMod == null) return 0;

            CommandSourceStack source = context.getSource();
            try {
                serverMod.load();
                source.sendSuccess(serverMod.text("§aResource pack reloaded successfully!"), false);
            } catch (ResourcePackLoadException e) {
                if (serverMod.resourcePackState() instanceof ResourcePackState.Loading) {
                    source.sendFailure(serverMod.text("§c§lERROR!§r §eResource pack is already being loaded!"));
                } else {
                    source.sendFailure(serverMod.text("§c§lERROR!§r §4Failed to reload resource pack. Message: §f" + e.getMessage()));
                    source.sendFailure(serverMod.text("§eSee console for more info."));
                    source.sendFailure(serverMod.text("§eThe resource pack sending is disabled."));
                    source.sendFailure(serverMod.text("§e<italic>Once the problem is fixed, you can use `/rnu reload` again."));
                }
            }
            return Command.SINGLE_SUCCESS;
        }));
    }
}
