package com.lowdragmc.lowdraglib2.utils;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

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

}
