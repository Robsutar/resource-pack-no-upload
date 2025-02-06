package com.robsutar.rnu;

import java.net.URI;
import java.util.UUID;

public record ResourcePackInfo(UUID id, URI uri, String hash) {
}
