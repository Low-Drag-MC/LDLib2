package com.lowdragmc.lowdraglib2.utils.search;

public interface ISearch<T> {
    void search(String word, IResultHandler<T> searchHandler);
}
