package com.robsutar.rnu.velocity;

import com.robsutar.rnu.ResourcePackLoadException;
import com.robsutar.rnu.ResourcePackNoUpload;
import com.robsutar.rnu.ResourcePackState;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RNUCommand implements SimpleCommand {
    private static final List<String> SUGGESTIONS;

    static {
        SUGGESTIONS = new ArrayList<>();
        SUGGESTIONS.add("reload");
    }

    private final ResourcePackNoUpload plugin;

    public RNUCommand(ResourcePackNoUpload plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("resourcepacknoupload.admin");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 1) return SUGGESTIONS;
        return Collections.emptyList();
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        CommandSource sender = invocation.source();

        if (args.length != 1 || !args[0].equals("reload")) {
            sender.sendMessage(plugin.text("§cWrong command usage."));
            return;
        }

        try {
            plugin.load();
            sender.sendMessage(plugin.text("§aResource pack reloaded successfully!"));
        } catch (ResourcePackLoadException e) {
            if (plugin.resourcePackState() instanceof ResourcePackState.Loading) {
                sender.sendMessage(plugin.text("§c§lERROR!§r §eResource pack is already being loaded!"));
            } else {
                sender.sendMessage(plugin.text("§c§lERROR!§r §4Failed to reload resource pack. Message: §f" + e.getMessage()));
                sender.sendMessage(plugin.text("§eSee console for more info."));
                sender.sendMessage(plugin.text("§eThe resource pack sending is disabled."));
                sender.sendMessage(plugin.text("§e<italic>Once the problem is fixed, you can use `/rnu reload` again."));
            }
        }
    }
}
