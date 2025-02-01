package com.robsutar.rnu;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public record RNUConfig(int port, Component prompt, @Nullable Component kickOnRefuseMessage,
                        @Nullable Component kickOnFailMessage) {

}
