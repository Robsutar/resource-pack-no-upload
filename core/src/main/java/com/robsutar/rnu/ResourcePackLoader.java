package com.robsutar.rnu;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.robsutar.rnu.util.OC;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public interface ResourcePackLoader {
    Map<String, Consumer<OutputStream>> appendFiles() throws Exception;

    default byte[] load() throws Exception {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream(); ZipOutputStream zipOut = new ZipOutputStream(byteOut)) {
            Map<String, Consumer<OutputStream>> appended = appendFiles();

            for (Map.Entry<String, Consumer<OutputStream>> entry : appended.entrySet()) {
                Consumer<OutputStream> writer = entry.getValue();
                String name = entry.getKey();

                ZipEntry zipEntry = new ZipEntry(name);
                zipOut.putNextEntry(zipEntry);
                writer.accept(zipOut);
            }

            zipOut.close();
            return byteOut.toByteArray();
        }
    }

    static ResourcePackLoader deserialize(File tempFolder, Map<String, Object> raw) {
        if (raw.get("type") == null) throw new IllegalArgumentException("Missing loader type");
        String type = OC.str(raw.get("type"));
        switch (type) {
            case "Manual":
                return new Manual(raw);
            case "Merged":
                return new Merged(tempFolder, raw);
            case "Download":
                return new Download(tempFolder, raw);
            case "WithMovedFiles":
                return new WithMovedFiles(tempFolder, raw);
            case "WithDeletedFiles":
                return new WithDeletedFiles(tempFolder, raw);

            default:
                throw new IllegalArgumentException("Invalid loader type: " + type);
        }
    }

    class Manual implements ResourcePackLoader {
        private final File folder;

        public Manual(Map<String, Object> raw) {
            this.folder = new File(OC.str(raw.get("folder")));
        }

        @Override
        public Map<String, Consumer<OutputStream>> appendFiles() throws Exception {
            if (!folder.exists() && !folder.mkdirs()) throw new Exception("Directory could not be created: " + folder);
            if (!folder.isDirectory()) throw new Exception("File is not directory: " + folder);

            HashMap<String, Consumer<OutputStream>> output = new HashMap<>();
            appendDirectory(output, folder, "");
            return output;
        }

        private static void appendDirectory(Map<String, Consumer<OutputStream>> output, File directory, String parentPath) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        output.put(parentPath + file.getName(), (out) -> {
                            try (FileInputStream fileIn = new FileInputStream(file)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = fileIn.read(buffer)) > 0) {
                                    out.write(buffer, 0, len);
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

        public Download(File tempFolder, Map<String, Object> raw) {
            this.zipPath = new File(tempFolder, UUID.randomUUID() + ".Download.zip");

            try {
                this.url = new URL(OC.str(raw.get("url")));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            for (Map<String, Object> loaderRaw : OC.<Map<String, Object>>list(raw.get("headers"))) {
                headers.add(new Header(
                        OC.str(loaderRaw.get("key")),
                        OC.str(loaderRaw.get("value"))
                ));
            }
        }

        @Override
        public Map<String, Consumer<OutputStream>> appendFiles() throws Exception {
            HashMap<String, Consumer<OutputStream>> output = new HashMap<>();

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
                        output.put(zipEntry.getName(), out -> {
                            try {
                                out.write(data);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                    zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
            }

            if (!zipPath.delete())
                throw new IllegalArgumentException("Failed to delete temporary zip file: " + zipPath);

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

        public WithMovedFiles(File tempFolder, Map<String, Object> raw) {
            folder = OC.str(raw.get("folder"));
            destination = OC.str(raw.get("destination"));

            loader = deserialize(tempFolder, OC.map(raw.get("loader")));
        }

        @Override
        public Map<String, Consumer<OutputStream>> appendFiles() throws Exception {
            Map<String, Consumer<OutputStream>> output = loader.appendFiles();

            Map<String, String> toMove = new HashMap<>();
            for (Map.Entry<String, Consumer<OutputStream>> entry : output.entrySet()) {
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
                Consumer<OutputStream> applier = Objects.requireNonNull(output.remove(entry.getKey()));
                output.put(entry.getValue(), applier);
            }

            return output;
        }

        public @Nullable String removeShape(String shape, String file, boolean greedy) {
            String[] parts = shape.split("\\?", -1);

            String wildcardRegex = greedy ? "(.*)" : "(.*?)";

            StringBuilder regex = new StringBuilder("^");
            for (int i = 0; i < parts.length; i++) {
                regex.append(Pattern.quote(parts[i]));
                if (i < parts.length - 1) {
                    regex.append(wildcardRegex);
                }
            }

            Pattern pattern = Pattern.compile(regex.toString());
            Matcher matcher = pattern.matcher(file);

            if (matcher.lookingAt()) {
                int end = matcher.end();
                return file.substring(end);
            }
            return null;
        }
    }

    class WithDeletedFiles implements ResourcePackLoader {
        private final PathMatcher toDeletePattern;
        private final ResourcePackLoader loader;

        public WithDeletedFiles(File tempFolder, Map<String, Object> raw) {
            toDeletePattern = FileSystems.getDefault().getPathMatcher(
                    "glob:" + OC.str(raw.get("toDelete"))
            );

            loader = deserialize(tempFolder, OC.map(raw.get("loader")));
        }

        @Override
        public Map<String, Consumer<OutputStream>> appendFiles() throws Exception {
            Map<String, Consumer<OutputStream>> output = loader.appendFiles();

            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, Consumer<OutputStream>> entry : output.entrySet()) {
                String name = entry.getKey();
                if (toDeletePattern.matches(new File(name).toPath())) {
                    toRemove.add(name);
                }
            }
            for (String name : toRemove) {
                Objects.requireNonNull(output.remove(name));
            }

            return output;
        }
    }

    class Merged implements ResourcePackLoader {
        private static final Gson GSON = new Gson();

        private final List<PathMatcher> mergedJsonLists;
        private final List<ResourcePackLoader> loaders;

        public Merged(File tempFolder, Map<String, Object> raw) {
            mergedJsonLists = OC.<String>list(raw.get("mergedJsonLists")).stream()
                    .map((pattern) -> FileSystems.getDefault().getPathMatcher("glob:" + pattern))
                    .collect(Collectors.toList());

            loaders = OC.<Map<String, Object>>list(raw.get("loaders")).stream()
                    .map((loaderRaw) -> deserialize(tempFolder, loaderRaw))
                    .collect(Collectors.toList());
        }

        @Override
        public Map<String, Consumer<OutputStream>> appendFiles() throws Exception {
            HashMap<String, Consumer<OutputStream>> output = new HashMap<>();
            for (int i = loaders.size() - 1; i >= 0; i--) {
                ResourcePackLoader loader = loaders.get(i);
                for (Map.Entry<String, Consumer<OutputStream>> entry : loader.appendFiles().entrySet()) {
                    String name = entry.getKey();
                    Consumer<OutputStream> replacement = entry.getValue();
                    @Nullable Consumer<OutputStream> existing = output.get(name);

                    if (existing == null) {
                        output.put(name, replacement);
                    } else {
                        for (PathMatcher matcher : mergedJsonLists) {
                            if (matcher.matches(new File(name).toPath())) {
                                Map<Object, Object> existingMap;
                                {
                                    ByteArrayOutputStream outRaw = new ByteArrayOutputStream();
                                    existing.accept(outRaw);
                                    String out = outRaw.toString(StandardCharsets.UTF_8.name());
                                    existingMap = GSON.fromJson(out, new TypeToken<Map<Object, Object>>() {
                                    }.getType());
                                }

                                Map<Object, Object> replacementMap;
                                {
                                    ByteArrayOutputStream outRaw = new ByteArrayOutputStream();
                                    replacement.accept(outRaw);
                                    String out = outRaw.toString(StandardCharsets.UTF_8.name());
                                    replacementMap = GSON.fromJson(out, new TypeToken<Map<Object, Object>>() {
                                    }.getType());
                                }

                                Map<Object, Object> mergedMap = mergeMaps(existingMap, replacementMap);

                                output.put(name, (OutputStream out) -> {
                                    try {
                                        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
                                        GSON.toJson(mergedMap, writer);
                                        writer.flush();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                                break;
                            }
                        }
                    }
                }
            }
            return output;
        }


        @SuppressWarnings("unchecked")
        private static Map<Object, Object> mergeMaps(Map<Object, Object> existing, Map<Object, Object> replacement) {
            for (Map.Entry<Object, Object> entry : replacement.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();

                if (existing.containsKey(key)) {
                    Object existingValue = existing.get(key);

                    if (existingValue instanceof Map && value instanceof Map) {
                        existing.put(key, mergeMaps((Map<Object, Object>) existingValue, (Map<Object, Object>) value));
                    } else if (existingValue instanceof List && value instanceof List) {
                        List<Object> mergedList = new ArrayList<>((List<Object>) existingValue);
                        mergedList.addAll((List<Object>) value);
                        existing.put(key, mergedList);
                    } else {
                        existing.put(key, value);
                    }
                } else {
                    existing.put(key, value);
                }
            }
            return existing;
        }
    }
}
