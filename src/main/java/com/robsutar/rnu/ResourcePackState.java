package com.robsutar.rnu;

import net.kyori.adventure.resource.ResourcePackInfo;

public interface ResourcePackState {
    record Loaded(ResourcePackInfo resourcePackInfo, byte[] bytes) implements ResourcePackState {
    }

    class FailedToLoad implements ResourcePackState {

    }

    class Loading implements ResourcePackState {
    }

    record LoadedPendingProvider(Loaded loaded) implements ResourcePackState {

    }
}
