package com.robsutar.rnu;

import org.bukkit.configuration.ConfigurationSection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public interface ResourcePackLoader {
    Map<String, File> appendFiles() throws Exception;

    default byte[] load() throws Exception {
        try (var byteOut = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(byteOut)) {
            var appended = appendFiles();

            for (var entry : appended.entrySet()) {
                var file = entry.getValue();
                var name = entry.getKey();
                if (!file.isFile()) throw new IllegalArgumentException();

                var fileIn = new FileInputStream(file);
                var zipEntry = new ZipEntry(name);
                zipOut.putNextEntry(zipEntry);

                var buffer = new byte[1024];
                int len;
                while ((len = fileIn.read(buffer)) > 0) {
                    zipOut.write(buffer, 0, len);
                }

                fileIn.close();
            }

            zipOut.close();
            return byteOut.toByteArray();
        }
    }

    static ResourcePackLoader deserialize(ConfigurationSection raw) {
        var type = Objects.requireNonNull(raw.getString("type"), "Missing loader type");
        return switch (type) {
            case "Manual" -> new Manual(raw);
            case "Merged" -> new Merged(raw);
            default -> throw new IllegalArgumentException("Invalid loader type: " + type);
        };
    }

    class Manual implements ResourcePackLoader {
        private final File folder;

        public Manual(ConfigurationSection raw) {
            this.folder = new File(Objects.requireNonNull(raw.getString("folder")));
        }

        @Override
        public Map<String, File> appendFiles() throws Exception {
            if (!folder.exists()) throw new Exception("Directory not found: " + folder);
            if (!folder.isDirectory()) throw new Exception("File is not directory: " + folder);

            var output = new HashMap<String, File>();
            appendDirectory(output, folder, "");
            return output;
        }

        private static void appendDirectory(Map<String, File> output, File directory, String parentPath) {
            var files = directory.listFiles();
            if (files != null) {
                for (var file : files) {
                    if (file.isFile()) {
                        output.put(parentPath + file.getName(), file);
                    } else if (file.isDirectory()) {
                        appendDirectory(output, file, parentPath + file.getName() + "/");
                    }
                }
            }
        }
    }

    class Merged implements ResourcePackLoader {
        private final List<ResourcePackLoader> loaders = new ArrayList<>();

        public Merged(ConfigurationSection raw) {
            for (var loaderMap : raw.getMapList("loaders")) {
                var loaderRaw = raw.createSection("DISPOSABLE_SECTION", loaderMap);
                loaders.add(deserialize(loaderRaw));
            }
            raw.set("DISPOSABLE_SECTION", null);
        }

        @Override
        public Map<String, File> appendFiles() throws Exception {
            var output = new HashMap<String, File>();
            for (var i = loaders.size() - 1; i >= 0; i--) {
                var loader = loaders.get(i);
                output.putAll(loader.appendFiles());
            }
            return output;
        }
    }
}
