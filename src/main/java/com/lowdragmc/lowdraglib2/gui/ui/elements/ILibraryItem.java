package com.lowdragmc.lowdraglib2.gui.ui.elements;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import net.minecraft.network.chat.Component;

/**
 * One row in an {@link ItemLibraryPanel} — everything the panel needs to draw it and to find it.
 *
 * <p>Three getters and nothing else, because that is genuinely all the popup reads. What an entry
 * <i>is</i> — a node class, a component type, a block — is the caller's business, so the panel is
 * generic over this and never sees the payload.
 */
public interface ILibraryItem {

    /** The icon shown at the start of the row. Never null; use {@link IGuiTexture#EMPTY} for none. */
    IGuiTexture getIcon();

    /** The label shown on the row, and one of the three things the search matches against. */
    Component getDisplayName();

    /**
     * A plain, un-translated name to match typed text against, alongside the display name and its
     * localisation. Never null — the search lowercases it directly.
     */
    String getSearchableName();
}
