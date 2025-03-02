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
            ResourcePackNoUpload rnu = hook.rnu();
            if (rnu == null) return 0;

            CommandSourceStack source = context.getSource();
            try {
                rnu.load();
                // Is not an actual `failure`, but this method is the same for 1.19.x-1.20.1
                source.sendFailure(rnu.text("§aResource pack reloaded successfully!"));
            } catch (ResourcePackLoadException e) {
                if (rnu.resourcePackState() instanceof ResourcePackState.Loading) {
                    source.sendFailure(rnu.text("§c§lERROR!§r §eResource pack is already being loaded!"));
                } else {
                    source.sendFailure(rnu.text("§c§lERROR!§r §4Failed to reload resource pack. Message: §f" + e.getMessage()));
                    source.sendFailure(rnu.text("§eSee console for more info."));
                    source.sendFailure(rnu.text("§eThe resource pack sending is disabled."));
                    source.sendFailure(rnu.text("§e<italic>Once the problem is fixed, you can use `/rnu reload` again."));
                }
            }
            return Command.SINGLE_SUCCESS;
        }));
    }
}
