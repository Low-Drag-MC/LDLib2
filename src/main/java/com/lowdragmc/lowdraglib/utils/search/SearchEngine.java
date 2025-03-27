package com.lowdragmc.lowdraglib.utils.search;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public  class SearchEngine<T> {
    private final ISearch<T> search;
    private final Consumer<T> result;
    private Thread thread;


    public SearchEngine(@Nonnull ISearch<T> search, @Nonnull Consumer<T> result){
        this.search = search;
        this.result = result;
    }

    public void searchWord(String word) {
        dispose();
        thread = new Thread(()-> search.search(word, value -> {
            var currentThread = Thread.currentThread();
            if (!currentThread.isInterrupted() && thread == currentThread) {
                result.accept(value);
            } else {
                throw new RuntimeException("searching thread has been disposed");
            }
        }));
        thread.start();
    }

    public boolean isSearching() {
        return thread != null && thread.isAlive();
    }

    public void dispose() {
        if (isSearching()) {
            thread.interrupt();
        }
        thread = null;
    }

}
