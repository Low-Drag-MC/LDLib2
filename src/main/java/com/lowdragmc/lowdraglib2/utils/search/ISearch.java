package com.lowdragmc.lowdraglib2.utils.search;

import java.util.function.Consumer;

public interface ISearch<T> {
    void search(String word, Consumer<T> find);
}
