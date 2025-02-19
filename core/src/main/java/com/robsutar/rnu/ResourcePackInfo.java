package com.robsutar.rnu;

import java.util.UUID;

public final class ResourcePackInfo {
    private final UUID id;
    private final String uri;
    private final byte[] hash;
    private final String prompt;
    private final String hashStr;

    public ResourcePackInfo(UUID id, String uri, byte[] hash, String prompt) {
        this.id = id;
        this.uri = uri;
        this.hash = hash;
        this.prompt = prompt;

        StringBuilder sha1Hash = new StringBuilder();
        for (byte hashedByte : hash) {
            sha1Hash.append(Integer.toString((hashedByte & 0xff) + 0x100, 16).substring(1));
        }
        hashStr = sha1Hash.toString();
    }

    public UUID id() {
        return id;
    }

    public String uri() {
        return uri;
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
}
