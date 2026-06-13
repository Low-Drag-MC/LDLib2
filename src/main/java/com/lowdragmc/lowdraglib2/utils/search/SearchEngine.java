package com.lowdragmc.lowdraglib2.utils.search;

import com.lowdragmc.lowdraglib2.LDLib2;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;

public class SearchEngine<T> {
    private final ISearch<T> search;
    private final IResultHandler<T> result;
    private final AtomicReference<Thread> currentThread = new AtomicReference<>();

    public SearchEngine(@Nonnull ISearch<T> search, @Nonnull IResultHandler<T> result){
        this.search = search;
        this.result = result;
    }

    // TODO shall we add a method to schedule searching on the main thread?
    public void searchWord(String word) {
        Thread virtualThread = Thread.ofVirtual()
                .name("search-" + word.hashCode())
                .unstarted(() -> {
                    try {
                        search.search(word, value -> {
                            Thread current = Thread.currentThread();
                            if (current == currentThread.get() && !current.isInterrupted()) {
                                result.accept(value);
                            }
                        });
                    } catch (Exception e) {
                        if (!Thread.currentThread().isInterrupted()) {
                            LDLib2.LOGGER.error("Search failed for word '{}'", word, e);
                        }
                    } finally {
                        currentThread.compareAndSet(Thread.currentThread(), null);
                    }
                });
        Thread previousThread = currentThread.getAndSet(virtualThread);
        if (previousThread != null && previousThread.isAlive()) {
            previousThread.interrupt();
        }
        virtualThread.start();
    }

    public boolean isSearching() {
        Thread thread = currentThread.get();
        return thread != null && thread.isAlive();
    }

    public void dispose() {
        Thread thread = currentThread.getAndSet(null);
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }
}
