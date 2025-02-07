[![CI](https://github.com/Robsutar/resource-pack-no-upload/actions/workflows/ci.yml/badge.svg)](https://github.com/Robsutar/resource-pack-no-upload/actions/workflows/ci.yml)

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

**Available types:**

<details>
  <summary><strong>Manual</strong> simple loading from folder</summary>
  Uses an existing folder of any provided path.

  ```yml
  type: Manual

  # Relative to the server root folder.
  # Is inside `Cool Folder` (for this example) the resource pack files should be.
  # Cool Folder/pack.mcmeta
  # Cool Folder/assets/minecraft...
  folder: "plugins/ResourcePackNoUpload/Cool Folder/"
  ```

</details>

<details>
  <summary><strong>Download</strong> downloads from web, with http headers</summary>
  Downloads the resourcepack from a link.
  At first, it would be somewhat against the plugin's purpose, but this loader also
  supports http headers for the download request, allowing you to download the
  resource,  pack with private keys, witch is not supported directly by the minecraft client.

```yml
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
  <summary><strong>WithMovedFiles</strong> modify the files from another loader</summary>
  Move loader provided resource pack files from a directory, to another.

```yml
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
    url: https://api.github.com/repos/Robsutar/super-cool-pack/zipball
    headers:
      - key: "Authorization"
        value: "Bearer github_pat_uS78ih32i9DASKxE83hd9203f1930c0ll-g1t-t0k3n2389"
      - key: "Accept"
        value: "application/vnd.github.v3+json"
  ```

</details>

<details>
  <summary><strong>WithDeletedFiles</strong> modify the files from another loader</summary>
  Ignore the files of the loader if they match with a provided path.

```yml
  type: WithDeletedFiles

  # The path to ignored, supports glob file matching. In this example, we delete all
  # files that ends with `.md`.
  toDelete: "**.md"
  # To ignore all the files of a directory:
  # toDelete: "assets/all_inside_me_will_be_deleted/**/*"
  # To ignore a file a file:
  # toDelete: "assets/minecraft/i_will_be_deleted.txt"

  # This can be any loader, For this loader example we are using simple Manual loader.
  loader:
    type: Manual
    folder: "plugins/ResourcePackNoUpload/Cool Folder/"
  ```

</details>

<details>
  <summary><strong>Merged</strong> loads and merges resource packs from a list of loaders</summary>
  Combines multiple loaders, prioritizing the first ones in the list.

```yml
  type: Merged

  # This is a list, for each entry you need to specify the values of the desired loader.
  # See their how to configure each type in the examples above.
  # For overriding cases, loaders on the top of the list have major priority, this is,
  # their files will replace the other files.
  loaders:
    - type: Manual
      folder: "plugins/ResourcePackNoUpload/Cool Folder/"
    - type: Manual
      folder: "plugins/ModelEngine/resource pack/"
  ```

</details>

## Additional Configurations

There are other settings inside config.yml, such as messages and plugin behavior, refer to the automatically generated
config.yml.
