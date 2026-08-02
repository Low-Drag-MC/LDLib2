package com.lowdragmc.lowdraglib2.test.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceInstance;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtIo;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

/**
 * A resource file inside the game directory must resolve even when no provider is registered for its
 * folder — that is what lets the asset browser create resources anywhere. These tests cover the tier
 * itself, its caching, and above all that it never shadows a registered provider or leaks into the
 * resource enumeration.
 */
@GameTestHolder(LDLib2.MOD_ID)
public class DirectFileResolutionTest {

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void resolvesFileOutsideAnyProvider(GameTestHelper helper) {
        var directory = makeDirectory("direct_basic");
        try {
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, "color", 0x123456);
            var path = new FilePath(file);
            if (!path.getPath().startsWith("./")) {
                helper.fail("Expected a game relative path, got " + path.getPath());
                return;
            }
            var instance = instance();
            var value = instance.getResource(path);
            if (!Objects.equals(value, 0x123456)) {
                helper.fail("Expected 0x123456, got " + value);
                return;
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("IO failure: " + e);
        } finally {
            deleteRecursively(directory);
        }
    }

    /** A file whose envelope declares another resource type must not be loaded. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void rejectsWrongTypeEnvelope(GameTestHelper helper) {
        var directory = makeDirectory("direct_wrong_type");
        try {
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, "texture", 0x123456);
            if (instance().getResource(new FilePath(file)) != null) {
                helper.fail("A foreign envelope was loaded as a color");
                return;
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("IO failure: " + e);
        } finally {
            deleteRecursively(directory);
        }
    }

    /**
     * Resource paths are decoded from project data that can come from anywhere, so the tier must stay
     * inside the game directory.
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void rejectsOutsideGameDir(GameTestHelper helper) {
        var directory = new File(System.getProperty("java.io.tmpdir"), "ldlib-direct-test");
        try {
            directory.mkdirs();
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, "color", 0x123456);
            if (instance().getResource(new FilePath(file)) != null) {
                helper.fail("A file outside the game directory was loaded");
                return;
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("IO failure: " + e);
        } finally {
            deleteRecursively(directory);
        }
    }

    /** Editing a file outside the editor is picked up once the shared cache is dropped. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void picksUpExternalEdits(GameTestHelper helper) {
        var directory = makeDirectory("direct_external_edit");
        try {
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, "color", 1);
            var instance = instance();
            var path = new FilePath(file);
            if (!Objects.equals(instance.getResource(path), 1)) {
                helper.fail("Initial value was not read");
                return;
            }
            writeResource(file, "color", 2);
            instance.clearCache();
            if (!Objects.equals(instance.getResource(path), 2)) {
                helper.fail("The external edit was not picked up");
                return;
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("IO failure: " + e);
        } finally {
            deleteRecursively(directory);
        }
    }

    /** A missing path is cached as such, but creating the file must still make it resolvable. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void negativeCacheDoesNotBlockCreation(GameTestHelper helper) {
        var directory = makeDirectory("direct_negative_cache");
        try {
            var file = new File(directory, "sample.color.nbt");
            var instance = instance();
            var path = new FilePath(file);
            if (instance.getResource(path) != null) {
                helper.fail("A missing file resolved to a value");
                return;
            }
            writeResource(file, "color", 7);
            instance.clearCache();
            if (!Objects.equals(instance.getResource(path), 7)) {
                helper.fail("The newly created file did not resolve");
                return;
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("IO failure: " + e);
        } finally {
            deleteRecursively(directory);
        }
    }

    /**
     * The fallback runs after the registered providers, so a provider's in-memory value keeps winning
     * over whatever happens to be on disk.
     */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void registeredProviderStillWins(GameTestHelper helper) {
        var directory = makeDirectory("direct_provider_wins");
        var instance = instance();
        FileResourceProvider<Integer> provider = null;
        try {
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, "color", 111);
            // the provider loads 111 into memory when it is constructed
            provider = new FileResourceProvider<>(instance, directory);
            instance.addBuiltinProvider(provider);
            instance.clearCache();
            // change the file behind the provider's back, without letting it rescan
            writeResource(file, "color", 222);
            var value = instance.getResource(new FilePath(file));
            if (!Objects.equals(value, 111)) {
                helper.fail("Expected the provider's value 111, got " + value);
                return;
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("IO failure: " + e);
        } finally {
            if (provider != null) {
                instance.removeBuiltinProvider(provider);
            }
            instance.clearCache();
            deleteRecursively(directory);
        }
    }

    /** The tier resolves paths but owns none, so it must never appear in the resource listing. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void listAllResourcesUnaffected(GameTestHelper helper) {
        var directory = makeDirectory("direct_listing");
        try {
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, "color", 5);
            var instance = instance();
            var before = instance.listAllResourceEntries().size();
            if (instance.getResource(new FilePath(file)) == null) {
                helper.fail("The file did not resolve");
                return;
            }
            var after = instance.listAllResourceEntries().size();
            if (before != after) {
                helper.fail("The resource listing changed from " + before + " to " + after);
                return;
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("IO failure: " + e);
        } finally {
            deleteRecursively(directory);
        }
    }

    // ----------------------------------------------------------------------------------- helpers

    private static ResourceInstance<Integer> instance() {
        var instance = ColorsResource.INSTANCE.getResourceInstance();
        instance.clearCache();
        return instance;
    }

    private static File makeDirectory(String name) {
        var directory = new File(Platform.getGamePath().toFile(), LDLib2.MOD_ID + "/gametest/" + name);
        deleteRecursively(directory);
        directory.mkdirs();
        return directory;
    }

    private static void writeResource(File file, String type, int value) throws IOException {
        var tag = new CompoundTag();
        tag.putString("type", type);
        tag.put("data", IntTag.valueOf(value));
        NbtIo.write(tag, file.toPath());
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
            var children = file.listFiles();
            if (children != null) {
                for (var child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
