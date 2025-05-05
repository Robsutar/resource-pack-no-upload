package com.robsutar.rnu;

import com.robsutar.rnu.util.OC;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public final class RNUServerConfig {
    private final int port;
    private final @Nullable String publicLinkRoot;
    private final ResourcePackSender sender;

    public RNUServerConfig(int port, @Nullable String publicLinkRoot, ResourcePackSender sender) {
        this.port = port;
        this.publicLinkRoot = publicLinkRoot;
        this.sender = sender;
    }

    public int port() {
        return port;
    }

    public @Nullable String publicLinkRoot() {
        return publicLinkRoot;
    }

    public ResourcePackSender sender() {
        return sender;
    }

    public static RNUServerConfig deserialize(@Nullable String serverIp, Map<String, Object> raw) throws IllegalArgumentException {
        if (raw.get("port") == null)
            throw new IllegalArgumentException(
                    "Port undefined in configuration!\n" +
                            "Define it in ResourcePackNoUpload server.yml config\n" +
                            "Make sure to open this port to the players.\n"
            );
        int port = OC.intValue(raw.get("port"));

        String publicLinkRoot;
        if (raw.get("publicLinkRoot") != null) publicLinkRoot = OC.str(raw.get("publicLinkRoot"));
        else {
            if (serverIp != null && !serverIp.isEmpty()) publicLinkRoot = serverIp;
            else try {
                publicLinkRoot = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Failed to get server address from program ipv4.");
            }
            publicLinkRoot = "http://" + publicLinkRoot + ":" + port;
        }

        ResourcePackSender sender = ResourcePackSender.deserialize(OC.map(raw.get("sender")));

        return new RNUServerConfig(port, publicLinkRoot, sender);
    }
}
