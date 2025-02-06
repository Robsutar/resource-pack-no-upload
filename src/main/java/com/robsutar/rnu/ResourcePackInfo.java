package com.robsutar.rnu;

import java.net.URI;
import java.util.UUID;

public final class ResourcePackInfo {
    private final UUID id;
    private final URI uri;
    private final String hash;
    private final String prompt;

    public ResourcePackInfo(UUID id, URI uri, String hash, String prompt) {
        this.id = id;
        this.uri = uri;
        this.hash = hash;
        this.prompt = prompt;
    }

    public UUID id() {
        return id;
    }

    public URI uri() {
        return uri;
    }

    public String hash() {
        return hash;
    }

    public String prompt() {
        return prompt;
    }
}
