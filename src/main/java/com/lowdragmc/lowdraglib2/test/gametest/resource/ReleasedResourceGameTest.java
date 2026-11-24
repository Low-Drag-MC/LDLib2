package com.lowdragmc.lowdraglib2.test.gametest.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtIo;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * A released resource is held by nothing in the resource system: the same value while something else holds it, read
 * again once nothing does, and held again once kept.
 */
public final class ReleasedResourceGameTest {
    private static final String A_PROVIDER_HANDS_OUT_WHAT_IS_STILL_HELD_AND_READS_AGAIN_WHAT_IS_NOT = "released_resource_a_provider_hands_out_what_is_still_held_and_reads_again_what_is_not";
    private static final String AN_INSTANCE_HOLDS_NOTHING_IT_WAS_TOLD_TO_RELEASE = "released_resource_an_instance_holds_nothing_it_was_told_to_release";
    private static final String A_SAVE_OVER_A_RELEASED_RESOURCE_IS_WHAT_IS_READ = "released_resource_a_save_over_a_released_resource_is_what_is_read";
    // over Integer's small-value cache, so every read is a new instance
    private static final int VALUE = 0x654321;

    private ReleasedResourceGameTest() {
    }

    static void registerFunctions() {
        ResourceGameTests.registerFunction(A_PROVIDER_HANDS_OUT_WHAT_IS_STILL_HELD_AND_READS_AGAIN_WHAT_IS_NOT, ReleasedResourceGameTest::aProviderHandsOutWhatIsStillHeldAndReadsAgainWhatIsNot);
        ResourceGameTests.registerFunction(AN_INSTANCE_HOLDS_NOTHING_IT_WAS_TOLD_TO_RELEASE, ReleasedResourceGameTest::anInstanceHoldsNothingItWasToldToRelease);
        ResourceGameTests.registerFunction(A_SAVE_OVER_A_RELEASED_RESOURCE_IS_WHAT_IS_READ, ReleasedResourceGameTest::aSaveOverAReleasedResourceIsWhatIsRead);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = ResourceGameTests.defaultTestData(environment, "empty");
        ResourceGameTests.registerFunctionTest(event, A_PROVIDER_HANDS_OUT_WHAT_IS_STILL_HELD_AND_READS_AGAIN_WHAT_IS_NOT, ResourceGameTests.functionKey(A_PROVIDER_HANDS_OUT_WHAT_IS_STILL_HELD_AND_READS_AGAIN_WHAT_IS_NOT), testData);
        ResourceGameTests.registerFunctionTest(event, AN_INSTANCE_HOLDS_NOTHING_IT_WAS_TOLD_TO_RELEASE, ResourceGameTests.functionKey(AN_INSTANCE_HOLDS_NOTHING_IT_WAS_TOLD_TO_RELEASE), testData);
        ResourceGameTests.registerFunctionTest(event, A_SAVE_OVER_A_RELEASED_RESOURCE_IS_WHAT_IS_READ, ResourceGameTests.functionKey(A_SAVE_OVER_A_RELEASED_RESOURCE_IS_WHAT_IS_READ), testData);
    }

    public static void aProviderHandsOutWhatIsStillHeldAndReadsAgainWhatIsNot(GameTestHelper helper) {
        var directory = makeDirectory("released_provider");
        try {
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, VALUE);
            var provider = new FileResourceProvider<>(ColorsResource.INSTANCE.getResourceInstance(), directory);
            IResourcePath path = new FilePath(file);
            check(helper, path, provider::getResource, () -> provider.release(path), () -> provider.keep(path),
                    () -> provider.isReleased(path));
        } catch (Exception e) {
            helper.fail("Failed: " + e);
        } finally {
            deleteRecursively(directory);
        }
    }

    public static void anInstanceHoldsNothingItWasToldToRelease(GameTestHelper helper) {
        var directory = makeDirectory("released_instance");
        try {
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, VALUE);
            var instance = ColorsResource.INSTANCE.getResourceInstance();
            instance.clearCache();
            IResourcePath path = new FilePath(file);
            check(helper, path, instance::getResource, () -> instance.release(path), () -> instance.keep(path),
                    () -> instance.isReleased(path));
        } catch (Exception e) {
            helper.fail("Failed: " + e);
        } finally {
            deleteRecursively(directory);
        }
    }

    /** Saved over while released and still held: the save is what is read, not what was let go of. */
    public static void aSaveOverAReleasedResourceIsWhatIsRead(GameTestHelper helper) {
        var directory = makeDirectory("released_saved");
        var instance = ColorsResource.INSTANCE.getResourceInstance();
        var provider = new FileResourceProvider<>(instance, directory);
        instance.addUnlistedProvider(provider);
        try {
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, VALUE);
            provider.checkAndUpdateResourceProvider();
            IResourcePath path = new FilePath(file);
            var held = instance.getResource(path);
            instance.release(path);
            if (instance.getResource(path) != held) {
                helper.fail("A released value something still holds was read again");
                return;
            }
            Integer saved = VALUE + 1;
            provider.addResource(path, saved);
            if (instance.getResource(path) != saved) {
                helper.fail("Saved over while released, " + instance.getResource(path) + " was read, not the save");
                return;
            }
            provider.removeResource(path);
            if (instance.getResource(path) != null) {
                helper.fail("Deleted, " + instance.getResource(path) + " was still read");
                return;
            }
            helper.succeed();
        } catch (Exception e) {
            helper.fail("Failed: " + e);
        } finally {
            instance.removeUnlistedProvider(provider);
            deleteRecursively(directory);
        }
    }

    private static void check(GameTestHelper helper, IResourcePath path, Function<IResourcePath, Object> read,
                              Runnable release, Runnable keep, BooleanSupplier released) throws InterruptedException {
        var held = read.apply(path);
        release.run();
        if (read.apply(path) != held) {
            helper.fail("A released value something still holds was read again");
            return;
        }
        if (!released.getAsBoolean()) {
            helper.fail("A released value still around was not said to be released");
            return;
        }
        var probe = new WeakReference<>(held);
        held = null;
        if (!collected(probe)) {
            helper.fail("A released value nothing holds was kept");
            return;
        }
        if (released.getAsBoolean()) {
            helper.fail("A collected value was still said to be released");
            return;
        }
        var again = read.apply(path);
        if (!(again instanceof Integer value) || value != VALUE) {
            helper.fail("Expected " + VALUE + " read again, got " + again);
            return;
        }
        var probeAgain = new WeakReference<>(again);
        again = null;
        if (collected(probeAgain)) {
            helper.fail("A value read again was not held");
            return;
        }
        release.run();
        keep.run();
        if (released.getAsBoolean() || collected(probeAgain)) {
            helper.fail("A value released and kept again was not held");
            return;
        }
        helper.succeed();
    }

    private static boolean collected(WeakReference<?> probe) throws InterruptedException {
        for (int i = 0; i < 50 && probe.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }
        return probe.get() == null;
    }

    private static File makeDirectory(String name) {
        var directory = new File(Platform.getGamePath().toFile(), LDLib2.MOD_ID + "/gametest/" + name);
        deleteRecursively(directory);
        directory.mkdirs();
        return directory;
    }

    private static void writeResource(File file, int value) throws IOException {
        var tag = new CompoundTag();
        tag.putString("type", "color");
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
