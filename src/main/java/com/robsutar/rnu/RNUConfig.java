package com.robsutar.rnu;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Objects;

public final class RNUConfig {
    private final InetSocketAddress address;
    private final URI uri;
    private final String prompt;
    private final @Nullable String kickOnRefuseMessage;
    private final @Nullable String kickOnFailMessage;
    private final ResourcePackLoader loader;

    public RNUConfig(InetSocketAddress address,
                     URI uri,
                     String prompt,
                     @Nullable String kickOnRefuseMessage,
                     @Nullable String kickOnFailMessage,
                     ResourcePackLoader loader) {
        this.address = address;
        this.uri = uri;
        this.prompt = prompt;
        this.kickOnRefuseMessage = kickOnRefuseMessage;
        this.kickOnFailMessage = kickOnFailMessage;
        this.loader = loader;
    }

    public static RNUConfig deserialize(ConfigurationSection raw) throws IllegalArgumentException {
        if (raw.get("port") == null)
            throw new IllegalArgumentException(
                    "Port undefined in configuration!\n" +
                            "Define it in plugins/ResourcePackNoUpload/config.yml\n" +
                            "Make sure to open this port to the players.\n"
            );
        int port = raw.getInt("port");

        String addressStr;
        String addressRaw = raw.getString("serverAddress");
        if (addressRaw != null) {
            addressStr = addressRaw;
        } else {
            String definedIp = Bukkit.getIp();
            if (!definedIp.isEmpty()) {
                addressStr = definedIp;
            } else {
                try {
                    addressStr = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    throw new IllegalArgumentException("Failed to get server address from program ipv4.");
                }
            }
        }

        return new RNUConfig(
                new InetSocketAddress(addressStr, port),
                URI.create("http://" + addressStr + ":" + port),
                Objects.requireNonNull(raw.getString("prompt")),
                raw.get("kickOnRefuseMessage") instanceof String ? (String) raw.get("kickOnRefuseMessage") : null,
                raw.get("kickOnFailMessage") instanceof String ? (String) raw.get("kickOnFailMessage") : null,
                ResourcePackLoader.deserialize(Objects.requireNonNull(raw.getConfigurationSection("loader")))
        );
    }

    public InetSocketAddress address() {
        return address;
    }

    public URI uri() {
        return uri;
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
