package com.lowdragmc.lowdraglib2.test.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.File;
import java.io.IOException;

/**
 * {@link FilePath} stores a game-relative identity ({@code ./ldlib2/...}) so a reference saved in a
 * project and the resource panel's own enumeration collapse to the SAME key. These tests pin that
 * behavior down, and above all that references saved before the migration — absolute paths, from this
 * machine or another one, in any of the three historical codec layouts — still resolve.
 */
@GameTestHolder(LDLib2.MOD_ID)
public class ResourcePathMigrationTest {

    /** A file inside the game directory becomes a "./" anchored relative path. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void absoluteUnderGameDirCollapses(GameTestHelper helper) {
        var file = new File(LDLib2.getAssetsDir(), "ldlib2/resources/global/x.color.nbt");
        var path = new FilePath(file).getPath();
        if (!path.equals("./ldlib2/assets/ldlib2/resources/global/x.color.nbt")) {
            helper.fail("Expected the canonical relative path, got " + path);
            return;
        }
        helper.succeed();
    }

    /** An absolute path saved on ANOTHER machine collapses to the same identity as the local file. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void foreignAbsoluteCollapses(GameTestHelper helper) {
        var local = new FilePath(new File(LDLib2.getAssetsDir(), "ldlib2/resources/global/x.color.nbt"));
        var foreign = new FilePath("/some/other/machine/ldlib2/assets/ldlib2/resources/global/x.color.nbt");
        if (!local.getPath().equals(foreign.getPath())) {
            helper.fail("Foreign save did not collapse: " + foreign.getPath() + " != " + local.getPath());
            return;
        }
        helper.succeed();
    }

    /** The File based and the String based constructors produce equal, interchangeable keys. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void identityIsStable(GameTestHelper helper) {
        var file = new File(LDLib2.getAssetsDir(), "ldlib2/resources/global/x.color.nbt");
        var fromFile = new FilePath(file);
        var fromCanonical = new FilePath(fromFile.getPath());
        var fromAbsolute = new FilePath(file.getPath());
        if (!fromFile.equals(fromCanonical) || fromFile.hashCode() != fromCanonical.hashCode()) {
            helper.fail("Canonical string key differs from the file key");
            return;
        }
        if (!fromFile.equals(fromAbsolute)) {
            helper.fail("Absolute string key differs from the file key");
            return;
        }
        // whichever constructor was used, the file used for I/O must point at the same place.
        // Not necessarily the same string: the game directory itself is relative in a dev environment.
        try {
            if (!fromCanonical.getFile().getCanonicalFile().equals(file.getCanonicalFile())) {
                helper.fail("Canonical path resolved to " + fromCanonical.getFile().getCanonicalFile() +
                        " instead of " + file.getCanonicalFile());
                return;
            }
            if (!fromAbsolute.getFile().getCanonicalFile().equals(file.getCanonicalFile())) {
                helper.fail("Absolute path resolved to " + fromAbsolute.getFile().getCanonicalFile() +
                        " instead of " + file.getCanonicalFile());
                return;
            }
        } catch (IOException e) {
            helper.fail("Failed to canonicalize: " + e);
            return;
        }
        helper.succeed();
    }

    /** A custom provider outside the game directory keeps its absolute path. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void outsideGameDirUnchanged(GameTestHelper helper) {
        var outside = new File(System.getProperty("java.io.tmpdir"), "ldlib-path-test/x.color.nbt").getAbsolutePath();
        var path = new FilePath(outside).getPath();
        if (path.startsWith("./")) {
            helper.fail("A path outside the game dir must not be anchored, got " + path);
            return;
        }
        if (!path.equals(FilePath.normalizePath(outside))) {
            helper.fail("Expected " + FilePath.normalizePath(outside) + ", got " + path);
            return;
        }
        helper.succeed();
    }

    /** Normalizing an already canonical path must not change it again. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void idempotent(GameTestHelper helper) {
        var candidates = new String[]{
                new File(LDLib2.getAssetsDir(), "ldlib2/resources/global/x.color.nbt").getPath(),
                "/some/other/machine/ldlib2/assets/ldlib2/resources/global/x.color.nbt",
                new File(System.getProperty("java.io.tmpdir"), "ldlib-path-test/x.color.nbt").getAbsolutePath(),
        };
        for (var candidate : candidates) {
            var once = FilePath.toGameRelative(candidate);
            var twice = FilePath.toGameRelative(once);
            if (!once.equals(twice)) {
                helper.fail("Not idempotent for " + candidate + ": " + once + " -> " + twice);
                return;
            }
        }
        helper.succeed();
    }

    /** A canonical path resolves back to a file whose parent is the provider directory it came from. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void resolveFileRoundTrip(GameTestHelper helper) {
        var directory = new File(LDLib2.getAssetsDir(), "ldlib2/resources/global");
        var provider = new FileResourceProvider<>(ColorsResource.INSTANCE.getResourceInstance(), directory);
        var path = provider.createSubPath("round_trip");
        if (!(path instanceof FilePath filePath)) {
            helper.fail("createSubPath did not produce a FilePath");
            return;
        }
        var reparsed = new FilePath(filePath.getPath());
        if (!directory.equals(reparsed.getFile().getParentFile())) {
            helper.fail("Resolved parent " + reparsed.getFile().getParentFile() + " != " + directory);
            return;
        }
        // the provider only accepts paths whose direct parent is its own directory
        if (!provider.supportResourcePath(reparsed)) {
            helper.fail("Provider rejected its own path after a string round trip");
            return;
        }
        helper.succeed();
    }

    /** Provider locations survive serialization, in the new form and in both legacy forms. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void providerNbtRoundTrip(GameTestHelper helper) {
        var instance = ColorsResource.INSTANCE.getResourceInstance();
        var inside = new File(LDLib2.getAssetsDir(), "ldlib2/resources/global");
        var restored = FileResourceProvider.fromNBT(instance, new FileResourceProvider<>(instance, inside).serializeNBT());
        if (!restored.resourceLocation.equals(inside)) {
            helper.fail("Game dir provider became " + restored.resourceLocation);
            return;
        }

        var outside = new File(System.getProperty("java.io.tmpdir"), "ldlib-path-test").getAbsoluteFile();
        var restoredOutside = FileResourceProvider.fromNBT(instance, new FileResourceProvider<>(instance, outside).serializeNBT());
        if (!restoredOutside.resourceLocation.equals(outside)) {
            helper.fail("External provider became " + restoredOutside.resourceLocation);
            return;
        }

        // legacy _version=1: game relative, but without the "./" anchor
        var legacyRelative = new CompoundTag();
        legacyRelative.putString("name", "global");
        legacyRelative.putString("location", "ldlib2/assets/ldlib2/resources/global");
        legacyRelative.putInt("_version", 1);
        if (!FileResourceProvider.fromNBT(instance, legacyRelative).resourceLocation.equals(inside)) {
            helper.fail("Legacy relative location did not resolve to " + inside);
            return;
        }

        // legacy without _version: an absolute path from this machine
        var legacyAbsolute = new CompoundTag();
        legacyAbsolute.putString("name", "global");
        legacyAbsolute.putString("location", inside.getPath().replace('\\', '/'));
        if (!FileResourceProvider.fromNBT(instance, legacyAbsolute).resourceLocation.equals(inside)) {
            helper.fail("Legacy absolute location did not resolve to " + inside);
            return;
        }
        helper.succeed();
    }

    /** The resource pack view of a path still derives from the relative form. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void toResourceLocationStillDerives(GameTestHelper helper) {
        var path = new FilePath(new File(LDLib2.getAssetsDir(), "ldlib2/resources/global/x.color.nbt"));
        var expected = ResourceLocation.fromNamespaceAndPath("ldlib2", "resources/global/x.color.nbt");
        if (!expected.equals(path.getLocation())) {
            helper.fail("Expected " + expected + ", got " + path.getLocation());
            return;
        }
        helper.succeed();
    }

    /** All three historical codec layouts decode, and all three land on the canonical identity. */
    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
    public static void legacyCodecsStillDecode(GameTestHelper helper) {
        var file = new File(LDLib2.getAssetsDir(), "ldlib2/resources/global/x.color.nbt");
        var absolute = file.getPath().replace('\\', '/');
        var expected = new FilePath(file);

        var v2 = StringTag.valueOf("file(" + absolute + ")");

        var v1 = new CompoundTag();
        v1.putString("type", "file");
        v1.putString("path", absolute);

        var v0 = new CompoundTag();
        v0.putBoolean("built-in", false);
        v0.putString("path", absolute);

        var names = new String[]{"V2", "V1", "V0"};
        var tags = new Tag[]{v2, v1, v0};
        for (var i = 0; i < tags.length; i++) {
            var decoded = IResourcePath.CODEC.parse(NbtOps.INSTANCE, tags[i]).result().orElse(null);
            if (decoded == null) {
                helper.fail(names[i] + " failed to decode");
                return;
            }
            if (!expected.equals(decoded)) {
                helper.fail(names[i] + " decoded to " + decoded.getPath() + ", expected " + expected.getPath());
                return;
            }
        }

        // and encoding always emits the canonical "type(path)" string
        var encoded = IResourcePath.CODEC.encodeStart(NbtOps.INSTANCE, expected).result().orElse(null);
        if (encoded == null || !encoded.getAsString().equals("file(" + expected.getPath() + ")")) {
            helper.fail("Unexpected encoding: " + encoded);
            return;
        }
        helper.succeed();
    }
}
