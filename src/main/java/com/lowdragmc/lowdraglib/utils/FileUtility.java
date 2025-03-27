package com.lowdragmc.lowdraglib.utils;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@UtilityClass
public final class FileUtility {
    public static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    public static String readInputStream(InputStream inputStream) throws IOException {
        byte[] streamData = IOUtils.toByteArray(inputStream);
        return new String(streamData, StandardCharsets.UTF_8);
    }

    public static InputStream writeInputStream(String contents) {
        return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Tries to extract <code>JsonObject</code> from file on given path
     *
     * @param filePath path to file
     * @return <code>JsonObject</code> if extraction succeeds; otherwise <code>null</code>
     */
    public static JsonObject tryExtractFromFile(Path filePath) {
        try (InputStream fileStream = Files.newInputStream(filePath)) {
            InputStreamReader streamReader = new InputStreamReader(fileStream);
            return JsonParser.parseReader(streamReader).getAsJsonObject();
        } catch (Exception ignored) {
        }

        return null;
    }

    public static JsonElement loadJson(File file) {
        try {
            if (!file.isFile()) return null;
            Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            JsonElement json = JsonParser.parseReader(new JsonReader(reader));
            reader.close();
            return json;
        } catch (Exception ignored) {
        }
        return null;
    }

    public static boolean saveJson(File file, JsonElement element) {
        try {
            if (!file.getParentFile().isDirectory()) {
                file.getParentFile().mkdirs();
            }
            Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            writer.write(GSON_PRETTY.toJson(element));
            writer.close();
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static void extractJarFiles(String resource, File targetPath, boolean replace) { //terminal/guide
        FileSystem zipFileSystem = null;
        try {
            URI sampleUri = FileUtility.class.getResource("/assets/gregtech/.gtassetsroot").toURI();
            Path resourcePath;
            if (sampleUri.getScheme().equals("jar") || sampleUri.getScheme().equals("zip")) {
                zipFileSystem = FileSystems.newFileSystem(sampleUri, Collections.emptyMap());
                resourcePath = zipFileSystem.getPath(resource);
            } else if (sampleUri.getScheme().equals("file")) {
                resourcePath = Paths.get(FileUtility.class.getResource(resource).toURI());
            } else {
                throw new IllegalStateException("Unable to locate absolute path to directory: " + sampleUri);
            }

            List<Path> jarFiles = Files.walk(resourcePath)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            for (Path jarFile : jarFiles) {
                Path genPath = targetPath.toPath().resolve(resourcePath.relativize(jarFile).toString());
                Files.createDirectories(genPath.getParent());
                if (replace || !genPath.toFile().isFile()) {
                    Files.copy(jarFile, genPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (URISyntaxException | IOException ignored) {
        } finally {
            if (zipFileSystem != null) {
                //close zip file system to avoid issues
                IOUtils.closeQuietly(zipFileSystem);
            }
        }

    }
}
