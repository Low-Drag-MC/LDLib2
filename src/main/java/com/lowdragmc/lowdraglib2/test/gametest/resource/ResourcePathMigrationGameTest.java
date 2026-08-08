package com.lowdragmc.lowdraglib2.test.gametest.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.io.File;
import java.io.IOException;

/**
 * {@link FilePath} stores a game-relative identity ({@code ./ldlib2/...}) so a reference saved in a
 * project and the resource panel's own enumeration collapse to the SAME key. These tests pin that
 * behavior down, and above all that references saved before the migration — absolute paths, from this
 * machine or another one, in any of the three historical codec layouts — still resolve.
 */
public final class ResourcePathMigrationGameTest {
    private static final String ABSOLUTE_UNDER_GAME_DIR_COLLAPSES = "resource_path_migration_absolute_under_game_dir_collapses";
    private static final String FOREIGN_ABSOLUTE_COLLAPSES = "resource_path_migration_foreign_absolute_collapses";
    private static final String IDENTITY_IS_STABLE = "resource_path_migration_identity_is_stable";
    private static final String OUTSIDE_GAME_DIR_UNCHANGED = "resource_path_migration_outside_game_dir_unchanged";
    private static final String IDEMPOTENT = "resource_path_migration_idempotent";
    private static final String RESOLVE_FILE_ROUND_TRIP = "resource_path_migration_resolve_file_round_trip";
    private static final String PROVIDER_NBT_ROUND_TRIP = "resource_path_migration_provider_nbt_round_trip";
    private static final String TO_RESOURCE_LOCATION_STILL_DERIVES = "resource_path_migration_to_resource_location_still_derives";
    private static final String LEGACY_CODECS_STILL_DECODE = "resource_path_migration_legacy_codecs_still_decode";

    private ResourcePathMigrationGameTest() {
    }

    static void registerFunctions() {
        ResourceGameTests.registerFunction(ABSOLUTE_UNDER_GAME_DIR_COLLAPSES, ResourcePathMigrationGameTest::absoluteUnderGameDirCollapses);
        ResourceGameTests.registerFunction(FOREIGN_ABSOLUTE_COLLAPSES, ResourcePathMigrationGameTest::foreignAbsoluteCollapses);
        ResourceGameTests.registerFunction(IDENTITY_IS_STABLE, ResourcePathMigrationGameTest::identityIsStable);
        ResourceGameTests.registerFunction(OUTSIDE_GAME_DIR_UNCHANGED, ResourcePathMigrationGameTest::outsideGameDirUnchanged);
        ResourceGameTests.registerFunction(IDEMPOTENT, ResourcePathMigrationGameTest::idempotent);
        ResourceGameTests.registerFunction(RESOLVE_FILE_ROUND_TRIP, ResourcePathMigrationGameTest::resolveFileRoundTrip);
        ResourceGameTests.registerFunction(PROVIDER_NBT_ROUND_TRIP, ResourcePathMigrationGameTest::providerNbtRoundTrip);
        ResourceGameTests.registerFunction(TO_RESOURCE_LOCATION_STILL_DERIVES, ResourcePathMigrationGameTest::toResourceLocationStillDerives);
        ResourceGameTests.registerFunction(LEGACY_CODECS_STILL_DECODE, ResourcePathMigrationGameTest::legacyCodecsStillDecode);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = ResourceGameTests.defaultTestData(environment, "empty");
        ResourceGameTests.registerFunctionTest(event, ABSOLUTE_UNDER_GAME_DIR_COLLAPSES, ResourceGameTests.functionKey(ABSOLUTE_UNDER_GAME_DIR_COLLAPSES), testData);
        ResourceGameTests.registerFunctionTest(event, FOREIGN_ABSOLUTE_COLLAPSES, ResourceGameTests.functionKey(FOREIGN_ABSOLUTE_COLLAPSES), testData);
        ResourceGameTests.registerFunctionTest(event, IDENTITY_IS_STABLE, ResourceGameTests.functionKey(IDENTITY_IS_STABLE), testData);
        ResourceGameTests.registerFunctionTest(event, OUTSIDE_GAME_DIR_UNCHANGED, ResourceGameTests.functionKey(OUTSIDE_GAME_DIR_UNCHANGED), testData);
        ResourceGameTests.registerFunctionTest(event, IDEMPOTENT, ResourceGameTests.functionKey(IDEMPOTENT), testData);
        ResourceGameTests.registerFunctionTest(event, RESOLVE_FILE_ROUND_TRIP, ResourceGameTests.functionKey(RESOLVE_FILE_ROUND_TRIP), testData);
        ResourceGameTests.registerFunctionTest(event, PROVIDER_NBT_ROUND_TRIP, ResourceGameTests.functionKey(PROVIDER_NBT_ROUND_TRIP), testData);
        ResourceGameTests.registerFunctionTest(event, TO_RESOURCE_LOCATION_STILL_DERIVES, ResourceGameTests.functionKey(TO_RESOURCE_LOCATION_STILL_DERIVES), testData);
        ResourceGameTests.registerFunctionTest(event, LEGACY_CODECS_STILL_DECODE, ResourceGameTests.functionKey(LEGACY_CODECS_STILL_DECODE), testData);
    }

    /** A file inside the game directory becomes a "./" anchored relative path. */
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
    public static void toResourceLocationStillDerives(GameTestHelper helper) {
        var path = new FilePath(new File(LDLib2.getAssetsDir(), "ldlib2/resources/global/x.color.nbt"));
        var expected = Identifier.fromNamespaceAndPath("ldlib2", "resources/global/x.color.nbt");
        if (!expected.equals(path.getLocation())) {
            helper.fail("Expected " + expected + ", got " + path.getLocation());
            return;
        }
        helper.succeed();
    }

    /** All three historical codec layouts decode, and all three land on the canonical identity. */
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
        if (encoded == null || !encoded.asString().orElse("").equals("file(" + expected.getPath() + ")")) {
            helper.fail("Unexpected encoding: " + encoded);
            return;
        }
        helper.succeed();
    }
}
