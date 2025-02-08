package com.robsutar.rnu;

import com.robsutar.rnu.util.OC;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

public final class RNUConfig {
    private final String prompt;
    private final @Nullable String kickOnRefuseMessage;
    private final @Nullable String kickOnFailMessage;
    private final int sendingDelay;
    private final int resendingDelay;
    private final ResourcePackLoader loader;

    public RNUConfig(
            String prompt,
            @Nullable String kickOnRefuseMessage,
            @Nullable String kickOnFailMessage, int sendingDelay, int resendingDelay,
            ResourcePackLoader loader) {
        this.prompt = prompt;
        this.kickOnRefuseMessage = kickOnRefuseMessage;
        this.kickOnFailMessage = kickOnFailMessage;
        this.sendingDelay = sendingDelay;
        this.resendingDelay = resendingDelay;
        this.loader = loader;
    }

    public static RNUConfig deserialize(File tempFolder, Map<String, Object> raw) throws IllegalArgumentException {
        return new RNUConfig(
                OC.str(raw.get("prompt")),
                raw.get("kickOnRefuseMessage") instanceof String ? (String) raw.get("kickOnRefuseMessage") : null,
                raw.get("kickOnFailMessage") instanceof String ? (String) raw.get("kickOnFailMessage") : null,
                OC.intValue(raw.get("sendingDelay")),
                OC.intValue(raw.get("resendingDelay")),
                ResourcePackLoader.deserialize(tempFolder, OC.map(raw.get("loader")))
        );
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

    public int sendingDelay() {
        return sendingDelay;
    }

    public int resendingDelay() {
        return resendingDelay;
    }

    public ResourcePackLoader loader() {
        return loader;
    }
}
