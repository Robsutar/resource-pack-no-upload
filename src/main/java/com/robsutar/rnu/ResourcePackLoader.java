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
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); ZipOutputStream zipOut = new ZipOutputStream(byteOut)) {
            Map<String, File> appended = appendFiles();

            for (Map.Entry<String, File> entry : appended.entrySet()) {
                File file = entry.getValue();
                String name = entry.getKey();
                if (!file.isFile()) throw new IllegalArgumentException();

                FileInputStream fileIn = new FileInputStream(file);
                ZipEntry zipEntry = new ZipEntry(name);
                zipOut.putNextEntry(zipEntry);

                byte[] buffer = new byte[1024];
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
        String type = Objects.requireNonNull(raw.getString("type"), "Missing loader type");
        switch (type) {
            case "Manual":
                return new Manual(raw);

            case "Merged":
                return new Merged(raw);

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
        public Map<String, File> appendFiles() throws Exception {
            if (!folder.exists()) throw new Exception("Directory not found: " + folder);
            if (!folder.isDirectory()) throw new Exception("File is not directory: " + folder);

            HashMap<String, File> output = new HashMap<String, File>();
            appendDirectory(output, folder, "");
            return output;
        }

        private static void appendDirectory(Map<String, File> output, File directory, String parentPath) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
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
            for (Map<?, ?> loaderMap : raw.getMapList("loaders")) {
                ConfigurationSection loaderRaw = raw.createSection("DISPOSABLE_SECTION", loaderMap);
                loaders.add(deserialize(loaderRaw));
            }
            raw.set("DISPOSABLE_SECTION", null);
        }

        @Override
        public Map<String, File> appendFiles() throws Exception {
            HashMap<String, File> output = new HashMap<String, File>();
            for (int i = loaders.size() - 1; i >= 0; i--) {
                ResourcePackLoader loader = loaders.get(i);
                output.putAll(loader.appendFiles());
            }
            return output;
        }
    }
}
