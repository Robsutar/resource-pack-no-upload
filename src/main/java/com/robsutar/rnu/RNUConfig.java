package com.robsutar.rnu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public record RNUConfig(int port,
                        Component prompt,
                        @Nullable Component kickOnRefuseMessage,
                        @Nullable Component kickOnFailMessage) {
    public static RNUConfig deserialize(ConfigurationSection raw) throws IllegalArgumentException {
        var mm = MiniMessage.miniMessage();

        if (raw.get("port") == null)
            throw new IllegalArgumentException("""
                    Port undefined in configuration!
                    Define it in plugins/ResourcePackNoUpload/config.yml
                    Make sure to open this port to the players.
                    """);
        
        return new RNUConfig(
                raw.getInt("port"),
                mm.deserialize(Objects.requireNonNull(raw.getString("prompt"))),
                raw.get("kickOnRefuseMessage") instanceof String s1 ? mm.deserialize(s1) : null,
                raw.get("kickOnFailMessage") instanceof String s1 ? mm.deserialize(s1) : null
        );
    }
}
