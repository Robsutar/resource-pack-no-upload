package com.robsutar.rnu.velocity;

import com.robsutar.rnu.ResourcePackNoUpload;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

public class VelocityUtil {
    private static final Yaml YAML = new Yaml();

    public static <K, V> Map<K, V> loadOrCreateConfig(ResourcePackNoUpload rnu, String fileName) throws IllegalStateException {
        File folder = rnu.getDataFolder();
        if (!folder.exists() && !folder.mkdir())
            throw new IllegalStateException("Failed to create RNU folder");

        File configFile = new File(folder, fileName);
        if (!configFile.exists()) {
            rnu.saveResource(fileName, false);
        }
        try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(configFile.toPath()), StandardCharsets.UTF_8)) {
            Map<K, V> mapToAdd = YAML.load(reader);
            if (mapToAdd == null) {
                throw new IllegalArgumentException("null/empty yml: " + configFile);
            }
            return mapToAdd;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
