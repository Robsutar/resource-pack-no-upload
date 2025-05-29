[![CI](https://github.com/Robsutar/resource-pack-no-upload/actions/workflows/ci.yml/badge.svg)](https://github.com/Robsutar/resource-pack-no-upload/actions/workflows/ci.yml)

# ResourcePackNoUpload Plugin

A Minecraft server tool that allows server administrators to manage and distribute resource packs efficiently without
requiring external hosting.

## Getting Started

The RNU will generate the configuration templates if they do not
exist, but will not work without additional changes.
Two things are important: defining the port for the resource pack provider, and the loader to load the resource pack.

### Defining port

The server config:

- For RNU plugin: `plugins/ResourcePackNoUpload/server.yml`
- For RNU mod: `config/resourcepacknoupload/server.yml`

```yaml
# This server port needs to be open to the players
port: 25008 # Chose an open server port for the players.

# Provide the root address that will be sent to players, usually
# `http://` + server's public IP + `:` + port defined above.
# Example:
# publicLinkRoot: "http://192.0.2.1:25008"
# 
# For the most common cases, this field does not need to be set, but for Pterodactyl-based
# servers this field is mandatory.
publicLinkRoot:

# The approach of sending the resource pack for the players.
sender:
  type: # Here is your sender type.
  # Additional sender fields, each type has their own fields.
  # See the end of this document for more information.
```

### Creating resource pack loader

The plugin config (reloadable with `/rnu reload`):

- For RNU plugin: `plugins/ResourcePackNoUpload/config.yml`
- For RNU mod: `config/resourcepacknoupload/config.yml`

```yaml
# The texture pack loader, called every time that the resource pack is loaded by the RNU.
loader:
  type: # Here is your loader type.
  # Additional loader fields, each type has their own fields.
```

**Available types:**

<details>
  <summary><strong>ReadFolder</strong> <i>simple loading from folder</i></summary>
  Uses an existing folder of any provided path.

```yaml
  type: ReadFolder

  # Relative to the server root folder.
  # Is inside `Cool Folder` (for this example) the resource pack files should be.
  # Cool Folder/pack.mcmeta
  # Cool Folder/assets/minecraft...
  folder: "rnu resource pack/Cool Folder/"
```

</details>

<details>
  <summary><strong>ReadZip</strong> <i>simple loading from zipped resource pack</i></summary>
  Uses an existing .zip of any provided path.

```yaml
  type: ReadZip

  # Relative to the server root folder.
  # Is inside `Cool Resource Pack.zip` (for this example) the resource pack files should be.
  # Cool Resource Pack.zip/pack.mcmeta
  # Cool Resource Pack.zip/assets/minecraft...
  zip: "rnu resource pack/Cool Resource Pack.zip"
```

</details>

<details>
  <summary><strong>Download</strong> <i>downloads from web, with http headers</i></summary>
  Downloads the resource pack from a link.
  At first, it would be somewhat against the RNU purpose, but this loader also
  supports http headers for the download request, allowing you to download the
  resource,  pack with private keys, which is not supported directly by the minecraft client.

```yaml
  type: Download

  # The link for the download.
  url: https://www.googleapis.com/drive/v3/files/FILE_ID?alt=media

  # Optional field, this is a list of headers, with their keys and values, Here we
  # are calling the Google API, and passing a required token to download the file.
  headers:
    - key: "Authorization"
      value: "Bearer drive_api3213xih32i9DASKxE83hd9203f1930c0ll-d1v3-t0k3n2389"
```

</details>

<details>
  <summary><strong>WithMovedFiles</strong> <i>modify the files from another loader</i></summary>
  Move loader provided resource pack files from a directory, to another.

```yaml
  type: WithMovedFiles

  # The folder contents to be moved. Supports unknown paths, for they use `?`.
  # For this example, the download (see more loader information below) result for
  # the loader link would be something like:
  # `Robsutar-super-cool-pack78HN3278gj32d/assets/minecraft...`
  # We will use this first folder as origin, ignoring their name.
  folder: "?/"

  # The folder destination, here we are using the resource pack root.
  destination: ""

  # This can be any loader, For this loader example we are using a release in GitHub,
  # with Fine-grained personal  access token, with reading permissions. Depending on
  # the release, the content of the resource pack can be inside another download file,
  # we use WithMovedFiles to adjust this.
  loader:
    type: Download
    url: https://api.github.com/repos/Robsutar/super-cool-pack/zipball
    headers:
      - key: "Authorization"
        value: "Bearer github_pat_uS78ih32i9DASKxE83hd9203f1930c0ll-g1t-t0k3n2389"
      - key: "Accept"
        value: "application/vnd.github.v3+json"
```

</details>

<details>
  <summary><strong>WithDeletedFiles</strong> <i>modify the files from another loader</i></summary>
  Ignore the files of the loader if they match with a provided path.

```yaml
  type: WithDeletedFiles

  # The path to ignored, supports glob file matching. In this example, we delete all
  # files that ends with `.md`.
  toDelete: "**.md"
  # To ignore all the files of a directory:
  # toDelete: "assets/all_inside_me_will_be_deleted/**/*"
  # To ignore a file a file:
  # toDelete: "assets/minecraft/i_will_be_deleted.txt"

  # This can be any loader, For this loader example we are using simple ReadFolder loader.
  loader:
    type: ReadFolder
    folder: "rnu resource pack/Cool Folder/"
```

</details>

<details>
  <summary><strong>Merged</strong> <i>loads and merges resource packs from a list of loaders</i></summary>
  Combines multiple loaders, prioritizing the first ones in the list.

```yaml
  type: Merged

  # Optional field, this is a list of paths to merge json files list entries, particularly
  # useful to merge custom model data automatically. 
  mergedJsonLists:
    - files: "assets/minecraft/models/item/**.json"
      # The numeric field used to order the json entries, in the case of custom model data,
      # entries must necessarily be ordered.
      orderBy: "predicate.custom_model_data"

  # This is a list, for each entry you need to specify the values of the desired loader.
  # See their how to configure each type in the examples above.
  # For overriding cases, loaders on the top of the list have major priority, this is,
  # their files will replace the other files.
  loaders:
    - type: ReadFolder
      folder: "rnu resource pack/Cool Folder/"
    - type: ReadZip
      zip: "plugins/ModelEngine/resource pack.zip"
```

</details>

## Auto reloading

It is possible to make the RNU (**only the plugin**) reload automatically on event call.

<details>
  <summary><strong>How to configure it</strong></summary>
  The auto reloading config: `plugins/ResourcePackNoUpload/autoReloading.yml`

```yaml
invokers:
  # The event class, this is very specific to each case.
  # Here we will use a ModelEngine event.
  - eventClass: "com.ticxo.modelengine.api.events.ModelRegistrationEvent"

    # The delay in ticks after the event being called.
    delay: 10 # Default is 1.

    # The cooldown before this event can call the reload again.
    # Here we add a cooldown because in the case of ModelEngine, the
    # `ModelRegistrationEvent` event is called a few times in `/meg reload`, this
    # cooldown will make the reload be executed only once.
    repeatCooldown: 10 # Default is 0.
```

</details>

## Different sender

By default, the `Delayed` sender is used, and it works for most cases.

<details>
  <summary><strong>Using the injector on the PaperMC server</strong></summary>
  The server config: `plugins/ResourcePackNoUpload/server.yml`

```yaml
sender:
  # This sender will override in runtime the server resource pack properties, 
  # leaving the pack sending handling for the PaperMC.
  type: PaperPropertyInjector
```

</details>

<details>
  <summary><strong>No automatic url sending, and fixed link server</strong></summary>
  The server config: `plugins/ResourcePackNoUpload/server.yml`

```yaml
sender:
  # This sender will not send the resource pack url to the players automatically,
  # and will have a fixed link.
  # Useful if you want to send the resource pack through the vanilla behavior,
  # setting the resource-pack server.properties to this link for example.
  type: NoSenderFixedLink
  # With the "pack" route, the link would be something like: "http://192.0.2.1:25008/pack.zip"
  # But with the ip and port defined in the config `publicLinkRoot`
  route: "pack"
```

</details>

## Additional Configurations

There are other settings inside config.yml, such as messages and RNU behavior, refer to the automatically generated
config.yml.
