package com.robsutar.rnu;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Objects;

public record RNUConfig(InetSocketAddress address,
                        URI uri,
                        String prompt,
                        @Nullable String kickOnRefuseMessage,
                        @Nullable String kickOnFailMessage,
                        ResourcePackLoader loader) {
    public static RNUConfig deserialize(ConfigurationSection raw) throws IllegalArgumentException {
        if (raw.get("port") == null)
            throw new IllegalArgumentException("""
                    Port undefined in configuration!
                    Define it in plugins/ResourcePackNoUpload/config.yml
                    Make sure to open this port to the players.
                    """);
        var port = raw.getInt("port");

        String addressStr;
        var addressRaw = raw.getString("serverAddress");
        if (addressRaw != null) {
            addressStr = addressRaw;
        } else {
            var definedIp = Bukkit.getIp();
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
                raw.get("kickOnRefuseMessage") instanceof String s1 ? s1 : null,
                raw.get("kickOnFailMessage") instanceof String s1 ? s1 : null,
                ResourcePackLoader.deserialize(Objects.requireNonNull(raw.getConfigurationSection("loader")))
        );
    }
}
