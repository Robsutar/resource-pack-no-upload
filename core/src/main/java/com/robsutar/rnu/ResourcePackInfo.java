package com.robsutar.rnu;

import java.net.URI;
import java.util.UUID;

public final class ResourcePackInfo {
    private final UUID id;
    private final byte[] hash;
    private final String prompt;
    private final String hashStr;
    private final String uri;

    public ResourcePackInfo(UUID id, byte[] hash, String prompt, URI uriRoot) {
        this.id = id;
        this.hash = hash;
        this.prompt = prompt;

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }

        hashStr = hexString.toString();

        this.uri = uriRoot + "/" + hashStr + ".zip";
    }

    public UUID id() {
        return id;
    }

    public byte[] hash() {
        return hash;
    }

    public String prompt() {
        return prompt;
    }

    public String hashStr() {
        return hashStr;
    }

    public String uri() {
        return uri;
    }
}
