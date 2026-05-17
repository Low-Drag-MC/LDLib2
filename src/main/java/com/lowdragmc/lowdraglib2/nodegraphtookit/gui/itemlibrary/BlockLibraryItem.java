package com.lowdragmc.lowdraglib2.nodegraphtookit.gui.itemlibrary;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.nodegraphtookit.api.node.BlockNode;
import lombok.Getter;
import net.minecraft.network.chat.Component;

/**
 * Library entry for a {@link BlockNode} class. Used when the {@link ItemLibrary} is opened in
 * block-only mode (via {@code showBlocksForContext}). The selection handler reads
 * {@link #getBlockClass()} and dispatches an
 * {@code InsertBlockCommand} against the originating context.
 *
 * <p>Blocks are not created through the standard {@code NodeModelLibraryItem.createNode} lambda
 * because they cannot exist as top-level graph nodes — insertion is mediated by the parent
 * context.</p>
 */
public class BlockLibraryItem extends ItemLibraryItem {
    @Getter
    private final Class<? extends BlockNode> blockClass;

    public BlockLibraryItem(Class<? extends BlockNode> blockClass) {
        this.blockClass = blockClass;
        this.icon = Icons.NODE;
        this.displayName = Component.literal(blockClass.getSimpleName());
        this.searchableName = blockClass.getSimpleName();
    }
}
