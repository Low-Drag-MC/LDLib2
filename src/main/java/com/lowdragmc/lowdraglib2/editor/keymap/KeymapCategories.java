package com.lowdragmc.lowdraglib2.editor.keymap;

/**
 * The groups the keymap settings page lists actions under. Plain strings so a mod can add its own
 * without registering anything — they are translation keys, and an unknown one shows as itself.
 */
public final class KeymapCategories {
    public static final String GENERAL = "keymap.category.ldlib2.general";
    public static final String FILE = "keymap.category.ldlib2.file";
    public static final String EDIT = "keymap.category.ldlib2.edit";
    public static final String VIEW = "keymap.category.ldlib2.view";
    public static final String WINDOW = "keymap.category.ldlib2.window";

    private KeymapCategories() {}
}
