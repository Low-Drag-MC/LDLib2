package com.lowdragmc.lowdraglib2.gui.ui.data;

/**
 * The diffuse lighting a scene shades its entities and block entities with — the game's own lighting setups,
 * named here so a {@link com.lowdragmc.lowdraglib2.gui.ui.elements.Scene} can hold one on a dedicated server too.
 */
public enum SceneLighting {
    /** As in the world, for the scene world's dimension. */
    LEVEL,
    /** As a flat item in a GUI. */
    ITEMS_FLAT,
    /** As a 3D item in a GUI. */
    ITEMS_3D,
    /** As an entity in an inventory screen. */
    ENTITY_IN_UI,
    /** As a player skin preview. */
    PLAYER_SKIN
}
