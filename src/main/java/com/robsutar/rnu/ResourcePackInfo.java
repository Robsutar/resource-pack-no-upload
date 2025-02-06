package com.robsutar.rnu;

import java.util.UUID;

public final class ResourcePackInfo {
    private final UUID id;
    private final String uri;
    private final byte[] hash;
    private final String prompt;

    public ResourcePackInfo(UUID id, String uri, byte[] hash, String prompt) {
        this.id = id;
        this.uri = uri;
        this.hash = hash;
        this.prompt = prompt;
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
}
