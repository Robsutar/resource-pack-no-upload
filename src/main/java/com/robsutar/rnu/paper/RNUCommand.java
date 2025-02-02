package com.robsutar.rnu.paper;

import com.robsutar.rnu.ResourcePackLoadException;
import com.robsutar.rnu.ResourcePackNoUpload;
import com.robsutar.rnu.ResourcePackState;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RNUCommand implements CommandExecutor, TabCompleter {
    private final ResourcePackNoUpload plugin;

    public RNUCommand(ResourcePackNoUpload plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1 || !args[0].equals("reload")) return false;

        var mm = MiniMessage.miniMessage();
        try {
            plugin.load();
            sender.sendMessage(mm.deserialize("<green>Resource pack reloaded successfully!"));
        } catch (ResourcePackLoadException e) {
            if (plugin.resourcePackState() instanceof ResourcePackState.Loading) {
                sender.sendMessage(mm.deserialize("<red><bold>ERROR!</bold> <yellow>Resource pack is already being loaded!"));
            } else {
                sender.sendMessage(mm.deserialize("<red><bold>ERROR!</bold> Failed to reload resource pack. Message: <white>" + e.getMessage()));
                sender.sendMessage(mm.deserialize("<yellow>See console for more info."));
                sender.sendMessage(mm.deserialize("<yellow>The resource pack sending is disabled."));
                sender.sendMessage(mm.deserialize("<yellow><italic>Once the problem is fixed, you can use `/rnu reload` again."));
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("reload");
        return List.of();
    }
}
