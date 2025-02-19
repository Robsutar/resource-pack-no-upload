package com.robsutar.rnu.fabric;

import com.robsutar.rnu.ResourcePackInfo;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.jetbrains.annotations.NotNull;

public interface RNUPackLoadedCallback {
    Event<RNUPackLoadedCallback> EVENT = EventFactory.createArrayBacked(RNUPackLoadedCallback.class,
            (listeners) -> (resourcePackInfo) -> {
                for (RNUPackLoadedCallback listener : listeners) {
                    listener.view(resourcePackInfo);
                }
            });

    void view(@NotNull ResourcePackInfo resourcePackInfo);
}
