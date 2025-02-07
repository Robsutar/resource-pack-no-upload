package com.robsutar.rnu;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Objects;

public final class RNUConfig {
    private final String prompt;
    private final @Nullable String kickOnRefuseMessage;
    private final @Nullable String kickOnFailMessage;
    private final ResourcePackLoader loader;

    public RNUConfig(
            String prompt,
            @Nullable String kickOnRefuseMessage,
            @Nullable String kickOnFailMessage,
            ResourcePackLoader loader) {
        this.prompt = prompt;
        this.kickOnRefuseMessage = kickOnRefuseMessage;
        this.kickOnFailMessage = kickOnFailMessage;
        this.loader = loader;
    }

    public static RNUConfig deserialize(File tempFolder, ConfigurationSection raw) throws IllegalArgumentException {
        return new RNUConfig(
                Objects.requireNonNull(raw.getString("prompt")),
                raw.get("kickOnRefuseMessage") instanceof String ? (String) raw.get("kickOnRefuseMessage") : null,
                raw.get("kickOnFailMessage") instanceof String ? (String) raw.get("kickOnFailMessage") : null,
                ResourcePackLoader.deserialize(tempFolder, Objects.requireNonNull(raw.getConfigurationSection("loader")))
        );
    }

    public String prompt() {
        return prompt;
    }

    public @Nullable String kickOnRefuseMessage() {
        return kickOnRefuseMessage;
    }

    public @Nullable String kickOnFailMessage() {
        return kickOnFailMessage;
    }

    public ResourcePackLoader loader() {
        return loader;
    }
}
