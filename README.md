# ResourcePackNoUpload Plugin

A Minecraft server plugin that allows server administrators to manage and distribute resource packs efficiently without
requiring external hosting.

## Getting Started

The plugin will generate the configuration templates if they do not
exist, but will not work without additional changes.
Two things are important: defining the port for the resource pack provider, and the loader to load the resource pack.

### Defining port

The server config
`plugins/ResourcePackNoUpload/server.yml`

```yml
# This server port needs to be open to the players
port: 3521 # Chose an open server port for the players.

# Provides the address start for address for the resource pack.
# If this is field is blank, it will use defined ip in server.properties, or the program ipv4.
serverAddress: # For the most common cases, this field does not need to be set.
```

### Creating resource pack loader

The plugin config (reloadable) `plugins/ResourcePackNoUpload/config.yml`

```yml
# The texture pack loader, called every time that the resource pack is loaded by the plugin.
loader:
  type: # Here is your loader type.
  # Additional loader fields, each type has their own fields.
```

