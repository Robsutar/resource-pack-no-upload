package com.robsutar.rnu.bukkit;

import com.robsutar.rnu.ResourcePackLoadException;
import com.robsutar.rnu.ResourcePackNoUpload;
import com.robsutar.rnu.ResourcePackState;
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

        try {
            plugin.load();
            sender.sendMessage("§aResource pack reloaded successfully!");
        } catch (ResourcePackLoadException e) {
            if (plugin.resourcePackState() instanceof ResourcePackState.Loading) {
                sender.sendMessage("§c§lERROR!§r §eResource pack is already being loaded!");
            } else {
                sender.sendMessage("§c§lERROR!§r §4Failed to reload resource pack. Message: §f" + e.getMessage());
                sender.sendMessage("§eSee console for more info.");
                sender.sendMessage("§eThe resource pack sending is disabled.");
                sender.sendMessage("§e<italic>Once the problem is fixed, you can use `/rnu reload` again.");
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
