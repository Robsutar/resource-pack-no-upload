package com.robsutar.rnu;

import org.bukkit.configuration.ConfigurationSection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public interface ResourcePackLoader {
    byte[] load() throws Exception;

    static ResourcePackLoader deserialize(ConfigurationSection raw) {
        var type = Objects.requireNonNull(raw.getString("type"), "Missing loader type");
        return switch (type) {
            case "Manual" -> new Manual(raw);
            default -> throw new IllegalArgumentException("Invalid loader type: " + type);
        };
    }

    class Manual implements ResourcePackLoader {
        private final File folder;

        public Manual(ConfigurationSection raw) {
            this.folder = new File(Objects.requireNonNull(raw.getString("folder")));
        }

        @Override
        public byte[] load() throws Exception {
            if (!folder.exists()) throw new IllegalArgumentException("Directory not found: " + folder);

            try (var byteOut = new ByteArrayOutputStream(); var zipOut = new ZipOutputStream(byteOut)) {
                var files = folder.listFiles();
                if (files != null) {
                    for (var file : files) {
                        if (file.isFile()) {
                            var fileIn = new FileInputStream(file);
                            var zipEntry = new ZipEntry(file.getName());
                            zipOut.putNextEntry(zipEntry);

                            var buffer = new byte[1024];
                            int len;
                            while ((len = fileIn.read(buffer)) > 0) {
                                zipOut.write(buffer, 0, len);
                            }

                            fileIn.close();
                        } else if (file.isDirectory()) {
                            addDirectoryToZip(zipOut, file, file.getName() + "/");
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Not an directory: " + folder);
                }

                zipOut.close();
                return byteOut.toByteArray();
            }
        }

        private static void addDirectoryToZip(ZipOutputStream zipOut, File directory, String parentPath) throws IOException {
            var files = directory.listFiles();
            if (files != null) {
                for (var file : files) {
                    if (file.isFile()) {
                        var fileIn = new FileInputStream(file);
                        var zipEntry = new ZipEntry(parentPath + file.getName());
                        zipOut.putNextEntry(zipEntry);

                        var buffer = new byte[1024];
                        int len;
                        while ((len = fileIn.read(buffer)) > 0) {
                            zipOut.write(buffer, 0, len);
                        }

                        fileIn.close();
                    } else if (file.isDirectory()) {
                        addDirectoryToZip(zipOut, file, parentPath + file.getName() + "/");
                    }
                }
            }
        }
    }
}
