package com.robsutar.rnu.velocity;

import com.robsutar.rnu.ResourcePackInfo;
import org.jetbrains.annotations.NotNull;

public class RNUPackLoadedEvent {
    private final @NotNull ResourcePackInfo resourcePackInfo;

    public RNUPackLoadedEvent(@NotNull ResourcePackInfo resourcePackInfo) {
        this.resourcePackInfo = resourcePackInfo;
    }

    public @NotNull ResourcePackInfo getResourcePackInfo() {
        return resourcePackInfo;
    }
}
