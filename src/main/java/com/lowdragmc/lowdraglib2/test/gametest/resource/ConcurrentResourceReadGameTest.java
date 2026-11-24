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
import net.minecraft.nbt.ReportedNbtException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Resources may be read on worker threads (e.g. preloading) while the game thread scans: readers racing for the same
 * file are handed the same value, and a scan running alongside them breaks nothing.
 */
public final class ConcurrentResourceReadGameTest {
    private static final String RACING_READS_OF_A_PROVIDER_SHARE_ONE_VALUE = "concurrent_resource_read_racing_reads_of_a_provider_share_one_value";
    private static final String RACING_READS_OF_A_DIRECT_FILE_SHARE_ONE_VALUE = "concurrent_resource_read_racing_reads_of_a_direct_file_share_one_value";
    private static final String READS_ALONGSIDE_SCANS_AND_REWRITES = "concurrent_resource_read_reads_alongside_scans_and_rewrites";
    private static final String A_READ_RACING_A_SAVE_DOES_NOT_KEEP_THE_OLD_VALUE = "concurrent_resource_read_a_read_racing_a_save_does_not_keep_the_old_value";
    private static final int THREADS = 8;
    // over Integer's small-value cache, so every read is a new instance
    private static final int VALUE = 0x123456;

    private ConcurrentResourceReadGameTest() {
    }

    static void registerFunctions() {
        ResourceGameTests.registerFunction(RACING_READS_OF_A_PROVIDER_SHARE_ONE_VALUE, ConcurrentResourceReadGameTest::racingReadsOfAProviderShareOneValue);
        ResourceGameTests.registerFunction(RACING_READS_OF_A_DIRECT_FILE_SHARE_ONE_VALUE, ConcurrentResourceReadGameTest::racingReadsOfADirectFileShareOneValue);
        ResourceGameTests.registerFunction(READS_ALONGSIDE_SCANS_AND_REWRITES, ConcurrentResourceReadGameTest::readsAlongsideScansAndRewrites);
        ResourceGameTests.registerFunction(A_READ_RACING_A_SAVE_DOES_NOT_KEEP_THE_OLD_VALUE, ConcurrentResourceReadGameTest::aReadRacingASaveDoesNotKeepTheOldValue);
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = ResourceGameTests.defaultTestData(environment, "empty");
        ResourceGameTests.registerFunctionTest(event, RACING_READS_OF_A_PROVIDER_SHARE_ONE_VALUE, ResourceGameTests.functionKey(RACING_READS_OF_A_PROVIDER_SHARE_ONE_VALUE), testData);
        ResourceGameTests.registerFunctionTest(event, RACING_READS_OF_A_DIRECT_FILE_SHARE_ONE_VALUE, ResourceGameTests.functionKey(RACING_READS_OF_A_DIRECT_FILE_SHARE_ONE_VALUE), testData);
        ResourceGameTests.registerFunctionTest(event, READS_ALONGSIDE_SCANS_AND_REWRITES, ResourceGameTests.functionKey(READS_ALONGSIDE_SCANS_AND_REWRITES), testData);
        ResourceGameTests.registerFunctionTest(event, A_READ_RACING_A_SAVE_DOES_NOT_KEEP_THE_OLD_VALUE, ResourceGameTests.functionKey(A_READ_RACING_A_SAVE_DOES_NOT_KEEP_THE_OLD_VALUE), testData);
    }

    public static void racingReadsOfAProviderShareOneValue(GameTestHelper helper) {
        var directory = makeDirectory("concurrent_provider");
        try {
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, VALUE);
            var provider = new FileResourceProvider<>(ColorsResource.INSTANCE.getResourceInstance(), directory);
            var path = new FilePath(file);
            var read = race(() -> provider.getResource(path));
            check(helper, read);
        } catch (Exception e) {
            helper.fail("Failed: " + e);
        } finally {
            deleteRecursively(directory);
        }
    }

    public static void racingReadsOfADirectFileShareOneValue(GameTestHelper helper) {
        var directory = makeDirectory("concurrent_direct");
        try {
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, VALUE);
            var instance = ColorsResource.INSTANCE.getResourceInstance();
            instance.clearCache();
            IResourcePath path = new FilePath(file);
            var read = race(() -> instance.getResource(path));
            check(helper, read);
        } catch (Exception e) {
            helper.fail("Failed: " + e);
        } finally {
            deleteRecursively(directory);
        }
    }

    public static void readsAlongsideScansAndRewrites(GameTestHelper helper) {
        var directory = makeDirectory("concurrent_scan");
        var pool = Executors.newFixedThreadPool(THREADS);
        try {
            var files = new ArrayList<File>();
            for (int i = 0; i < 16; i++) {
                var file = new File(directory, "sample" + i + ".color.nbt");
                writeResource(file, VALUE + i);
                files.add(file);
            }
            var provider = new FileResourceProvider<>(ColorsResource.INSTANCE.getResourceInstance(), directory);
            var stop = new java.util.concurrent.atomic.AtomicBoolean();
            var readers = new ArrayList<Future<?>>();
            for (int t = 0; t < THREADS; t++) {
                readers.add(pool.submit(() -> {
                    while (!stop.get()) {
                        for (var file : files) {
                            try {
                                provider.getResource(new FilePath(file));
                            } catch (ReportedNbtException torn) {
                                // read while it was being written: the next scan sees the new stamp
                            }
                        }
                    }
                }));
            }
            for (int round = 0; round < 50; round++) {
                writeResource(files.get(round % files.size()), VALUE + 100 + round);
                files.get(round % files.size()).setLastModified(System.currentTimeMillis() + round * 1000L);
                provider.checkAndUpdateResourceProvider();
            }
            stop.set(true);
            for (var reader : readers) {
                reader.get();
            }
            provider.checkAndUpdateResourceProvider();
            var last = files.get(49 % files.size());
            var value = provider.getResource(new FilePath(last));
            if (value == null || value != VALUE + 100 + 49) {
                helper.fail("Expected the last write, " + (VALUE + 100 + 49) + ", got " + value);
                return;
            }
            helper.succeed();
        } catch (Exception e) {
            helper.fail("Failed: " + e);
        } finally {
            pool.shutdownNow();
            deleteRecursively(directory);
        }
    }

    /** A read that raced a save must not leave the old value cached once the save has cleared the cache. */
    public static void aReadRacingASaveDoesNotKeepTheOldValue(GameTestHelper helper) {
        var directory = makeDirectory("concurrent_save");
        var pool = Executors.newFixedThreadPool(THREADS);
        try {
            var file = new File(directory, "sample.color.nbt");
            writeResource(file, VALUE);
            var instance = ColorsResource.INSTANCE.getResourceInstance();
            instance.clearCache();
            IResourcePath path = new FilePath(file);
            var stop = new java.util.concurrent.atomic.AtomicBoolean();
            var readers = new ArrayList<Future<?>>();
            for (int t = 0; t < THREADS; t++) {
                readers.add(pool.submit(() -> {
                    while (!stop.get()) {
                        try {
                            instance.getResource(path);
                        } catch (ReportedNbtException torn) {
                            // read while it was being written
                        }
                    }
                }));
            }
            int last = VALUE;
            for (int round = 1; round <= 50; round++) {
                last = VALUE + round;
                writeResource(file, last);
                file.setLastModified(System.currentTimeMillis() + round * 1000L);
                instance.clearCache();
            }
            Thread.sleep(50);
            stop.set(true);
            for (var reader : readers) {
                reader.get();
            }
            var value = instance.getResource(path);
            if (value == null || (Integer) value != last) {
                helper.fail("Expected the last save, " + last + ", got " + value);
                return;
            }
            helper.succeed();
        } catch (Exception e) {
            helper.fail("Failed: " + e);
        } finally {
            pool.shutdownNow();
            deleteRecursively(directory);
        }
    }

    private static List<Object> race(Supplier<Object> read) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            var start = new CountDownLatch(1);
            var futures = new ArrayList<Future<Object>>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return read.get();
                }));
            }
            start.countDown();
            var results = new ArrayList<>();
            for (var future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static void check(GameTestHelper helper, List<Object> read) {
        var first = read.getFirst();
        if (!(first instanceof Integer value) || value != VALUE) {
            helper.fail("Expected " + VALUE + ", got " + first);
            return;
        }
        for (var other : read) {
            if (other != first) {
                helper.fail("Two threads were handed different values: " + read);
                return;
            }
        }
        helper.succeed();
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
