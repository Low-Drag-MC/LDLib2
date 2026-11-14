package com.lowdragmc.lowdraglib2.test.resource;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.editor.resource.ColorsResource;
import com.lowdragmc.lowdraglib2.editor.resource.FilePath;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.IResourcePath;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.ReportedNbtException;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
@GameTestHolder(LDLib2.MOD_ID)
public class ConcurrentResourceReadTest {
    private static final int THREADS = 8;
    // over Integer's small-value cache, so every read is a new instance
    private static final int VALUE = 0x123456;

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
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

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
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

    @GameTest(template = "empty")
    @PrefixGameTestTemplate(false)
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
