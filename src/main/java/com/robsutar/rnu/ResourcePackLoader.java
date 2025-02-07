package com.robsutar.rnu;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public interface ResourcePackLoader {
    Map<String, Consumer<ZipOutputStream>> appendFiles() throws Exception;

    default byte[] load() throws Exception {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); ZipOutputStream zipOut = new ZipOutputStream(byteOut)) {
            Map<String, Consumer<ZipOutputStream>> appended = appendFiles();

            for (Map.Entry<String, Consumer<ZipOutputStream>> entry : appended.entrySet()) {
                Consumer<ZipOutputStream> writer = entry.getValue();
                String name = entry.getKey();

                ZipEntry zipEntry = new ZipEntry(name);
                zipOut.putNextEntry(zipEntry);
                writer.accept(zipOut);
            }

            zipOut.close();
            return byteOut.toByteArray();
        }
    }

    static ResourcePackLoader deserialize(File tempFolder, ConfigurationSection raw) {
        String type = Objects.requireNonNull(raw.getString("type"), "Missing loader type");
        switch (type) {
            case "Manual":
                return new Manual(raw);
            case "Merged":
                return new Merged(tempFolder, raw);
            case "Download":
                return new Download(tempFolder, raw);
            case "WithMovedFiles":
                return new WithMovedFiles(tempFolder, raw);

            default:
                throw new IllegalArgumentException("Invalid loader type: " + type);
        }
    }

    class Manual implements ResourcePackLoader {
        private final File folder;

        public Manual(ConfigurationSection raw) {
            this.folder = new File(Objects.requireNonNull(raw.getString("folder")));
        }

        @Override
        public Map<String, Consumer<ZipOutputStream>> appendFiles() throws Exception {
            if (!folder.exists()) throw new Exception("Directory not found: " + folder);
            if (!folder.isDirectory()) throw new Exception("File is not directory: " + folder);

            HashMap<String, Consumer<ZipOutputStream>> output = new HashMap<>();
            appendDirectory(output, folder, "");
            return output;
        }

        private static void appendDirectory(Map<String, Consumer<ZipOutputStream>> output, File directory, String parentPath) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        output.put(parentPath + file.getName(), (zipOut) -> {
                            try (FileInputStream fileIn = new FileInputStream(file)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = fileIn.read(buffer)) > 0) {
                                    zipOut.write(buffer, 0, len);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    } else if (file.isDirectory()) {
                        appendDirectory(output, file, parentPath + file.getName() + "/");
                    }
                }
            }
        }
    }

    class Download implements ResourcePackLoader {
        private final File zipPath;
        private final URL url;
        private final List<Header> headers = new ArrayList<>();

        public Download(File tempFolder, ConfigurationSection raw) {
            this.zipPath = new File(tempFolder, UUID.randomUUID() + ".Download.zip");

            try {
                this.url = new URL(Objects.requireNonNull(raw.getString("url")));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            for (Map<?, ?> loaderMap : raw.getMapList("headers")) {
                ConfigurationSection loaderRaw = raw.createSection("DISPOSABLE_SECTION", loaderMap);
                headers.add(new Header(
                        Objects.requireNonNull(loaderRaw.getString("key")),
                        Objects.requireNonNull(loaderRaw.getString("value"))
                ));
            }
            raw.set("DISPOSABLE_SECTION", null);
        }

        @Override
        public Map<String, Consumer<ZipOutputStream>> appendFiles() throws Exception {
            HashMap<String, Consumer<ZipOutputStream>> output = new HashMap<>();

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            for (Header header : headers) {
                connection.setRequestProperty(header.key, header.value);
            }

            try (InputStream inputStream = connection.getInputStream();
                 ReadableByteChannel rbc = Channels.newChannel(inputStream);
                 FileOutputStream fos = new FileOutputStream(zipPath)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            }

            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath.toPath()))) {
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    if (!zipEntry.isDirectory()) {
                        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            byteStream.write(buffer, 0, len);
                        }
                        byte[] data = byteStream.toByteArray();
                        output.put(zipEntry.getName(), zipOut -> {
                            try {
                                zipOut.write(data);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                    zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
            }

            zipPath.delete();

            return output;
        }

        private static File newFile(File destinationDir, ZipEntry zipEntry) {
            String entryName = zipEntry.getName();
            String[] parts = entryName.split("/", 2);

            String newEntryName = parts.length > 1 ? parts[1] : parts[0]; // Ignore root directory

            return new File(destinationDir, newEntryName);
        }

        private static class Header {
            private final String key;
            private final String value;

            private Header(String key, String value) {
                this.key = key;
                this.value = value;
            }
        }
    }

    class WithMovedFiles implements ResourcePackLoader {
        private final String folder;
        private final String destination;
        private final ResourcePackLoader loader;

        public WithMovedFiles(File tempFolder, ConfigurationSection raw) {
            folder = Objects.requireNonNull(raw.getString("folder"));
            destination = Objects.requireNonNull(raw.getString("destination"));

            loader = deserialize(tempFolder, Objects.requireNonNull(raw.getConfigurationSection("loader")));
        }

        @Override
        public Map<String, Consumer<ZipOutputStream>> appendFiles() throws Exception {
            Map<String, Consumer<ZipOutputStream>> output = loader.appendFiles();

            Map<String, String> toMove = new HashMap<>();
            for (Map.Entry<String, Consumer<ZipOutputStream>> entry : output.entrySet()) {
                String name = entry.getKey();
                @Nullable String newName = removeShape(folder, name, false);
                if (newName != null) {
                    toMove.put(name, destination + newName);
                } else {
                    newName = removeShape(folder, name, true);
                    if (newName != null) {
                        toMove.put(name, destination + newName);
                    }
                }
            }
            for (Map.Entry<String, String> entry : toMove.entrySet()) {
                Consumer<ZipOutputStream> applier = Objects.requireNonNull(output.remove(entry.getKey()));
                output.put(entry.getValue(), applier);
            }

            return output;
        }

        public @Nullable String removeShape(String shape, String file, boolean greedy) {
            // Separa as partes literais do shape
            String[] parts = shape.split("\\?", -1);

            // Monta a regex a partir do shape.
            // Use (.*) se greedy for true, ou (.*?) se false.
            String wildcardRegex = greedy ? "(.*)" : "(.*?)";

            StringBuilder regex = new StringBuilder("^");
            for (int i = 0; i < parts.length; i++) {
                // Escapa os caracteres especiais na parte literal
                regex.append(Pattern.quote(parts[i]));
                // Se ainda houver um '?' depois, insere o grupo correspondente
                if (i < parts.length - 1) {
                    regex.append(wildcardRegex);
                }
            }

            // Compila a regex
            Pattern pattern = Pattern.compile(regex.toString());
            Matcher matcher = pattern.matcher(file);

            // Verifica se o file começa com o padrão shape
            if (matcher.lookingAt()) {
                int end = matcher.end();
                return file.substring(end);
            }
            return null;
        }


    }

    class Merged implements ResourcePackLoader {
        private final List<ResourcePackLoader> loaders = new ArrayList<>();

        public Merged(File tempFolder, ConfigurationSection raw) {
            for (Map<?, ?> loaderMap : raw.getMapList("loaders")) {
                ConfigurationSection loaderRaw = raw.createSection("DISPOSABLE_SECTION", loaderMap);
                loaders.add(deserialize(tempFolder, loaderRaw));
            }
            raw.set("DISPOSABLE_SECTION", null);
        }

        @Override
        public Map<String, Consumer<ZipOutputStream>> appendFiles() throws Exception {
            HashMap<String, Consumer<ZipOutputStream>> output = new HashMap<>();
            for (int i = loaders.size() - 1; i >= 0; i--) {
                ResourcePackLoader loader = loaders.get(i);
                output.putAll(loader.appendFiles());
            }
            return output;
        }
    }
}
