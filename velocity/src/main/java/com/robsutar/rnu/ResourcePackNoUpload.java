package com.robsutar.rnu;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.logging.Logger;

@Plugin(id = "@name@",
        name = "@name@",
        version = "@version@",
        description = "Send your resource pack, without uploading it",
        authors = {"Robsutar"})
public final class ResourcePackNoUpload {
    private final ProxyServer server;
    private final Logger logger;

    private TextureProviderBytes textureProviderBytes;
    private RNUConfig config;
    private ResourcePackState resourcePackState = new ResourcePackState.FailedToLoad();

    @Inject
    public ResourcePackNoUpload(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

}
