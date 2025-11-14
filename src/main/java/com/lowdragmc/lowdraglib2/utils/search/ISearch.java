package com.lowdragmc.lowdraglib2.utils.search;

public interface ISearch<T> {
    /**
     * Performs a search operation for the given word and handles the results using the provided result handler.
     *
     * @param word the search term to look for
     * @param searchHandler the handler to process results found during the search
     */
    void search(String word, IResultHandler<T> searchHandler);
}
