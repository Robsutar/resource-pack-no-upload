package com.robsutar.rnu;

import org.jetbrains.annotations.NotNull;

public class ResourcePackLoadException extends Exception {
    public ResourcePackLoadException(String message) {
        super(message);
    }

    public ResourcePackLoadException(String message, Exception cause) {
        super(message, cause);
    }

    @Override
    public @NotNull String getMessage() {
        return super.getMessage();
    }
}
