package com.robsutar.rnu.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.robsutar.rnu.ResourcePackLoadException;
import com.robsutar.rnu.ResourcePackNoUpload;
import com.robsutar.rnu.ResourcePackState;
import net.minecraft.commands.CommandSourceStack;

public class RNUCommand extends LiteralArgumentBuilder<CommandSourceStack> {
    public RNUCommand(ResourcePackNoUpload plugin, String literal) {
        super(literal);
        then(LiteralArgumentBuilder.<CommandSourceStack>literal("reload").executes((context) -> {
            CommandSourceStack source = context.getSource();
            try {
                plugin.load();
                source.sendSuccess(plugin.text("§aResource pack reloaded successfully!"), false);
            } catch (ResourcePackLoadException e) {
                if (plugin.resourcePackState() instanceof ResourcePackState.Loading) {
                    source.sendFailure(plugin.text("§c§lERROR!§r §eResource pack is already being loaded!"));
                } else {
                    source.sendFailure(plugin.text("§c§lERROR!§r §4Failed to reload resource pack. Message: §f" + e.getMessage()));
                    source.sendFailure(plugin.text("§eSee console for more info."));
                    source.sendFailure(plugin.text("§eThe resource pack sending is disabled."));
                    source.sendFailure(plugin.text("§e<italic>Once the problem is fixed, you can use `/rnu reload` again."));
                }
            }
            return Command.SINGLE_SUCCESS;
        }));
    }
}
