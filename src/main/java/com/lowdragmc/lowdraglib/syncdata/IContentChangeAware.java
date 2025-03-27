package com.lowdragmc.lowdraglib.syncdata;

public interface IContentChangeAware {
    /**
     * Set the runnable to be called when the content changes.
     */
    void setOnContentsChanged(Runnable onContentChanged);

    /**
     * Get the runnable to be called when the content changes.
     */
    Runnable getOnContentsChanged();
}
