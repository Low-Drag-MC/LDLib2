package com.lowdragmc.lowdraglib.editor.data;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.editor.ui.Editor;
import com.lowdragmc.lowdraglib.gui.util.TreeBuilder;
import com.lowdragmc.lowdraglib.registry.ILDLRegisterClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * @author KilaBash
 * @date 2022/12/9
 * @implNote IProject
 */
public interface IProject extends ILDLRegisterClient<IProject, Supplier<IProject>>, INBTSerializable<CompoundTag> {

    Resources getResources();

    /**
     * Save project
     */
    default void saveProject(Path file) {
        try {
            NbtIo.write(serializeNBT(Platform.getFrozenRegistry()), file);
        } catch (IOException exception) {
            LDLib.LOGGER.error("Failed to save project: {}", file, exception);
        }
    }

    /**
     * Load project from file. return null if loading failed
     */
    @Nullable
    default IProject loadProject(Path file) {
        try {
            var tag = NbtIo.read(file);
            if (tag != null) {
                deserializeNBT(Platform.getFrozenRegistry(), tag);
            }
        } catch (IOException ignored) {}
        return this;
    }

    IProject newEmptyProject();

    /**
     * Get project work space
     */
    default File getProjectWorkSpace(Editor editor) {
        return new File(editor.getWorkSpace(), "projects/" + name());
    }

    /**
     * Suffix name of this project
     */
    default String getSuffix() {
        return name();
    }

    /**
     * Fired when project is closed
     */
    default void onClosed(Editor editor) {
    }

    /**
     * Fired when project is opened
     */
    default void onLoad(Editor editor) {
        editor.getResourcePanel().loadResource(getResources(), false);
    }

    /**
     * Attach menu
     * @param name menu name
     * @param menu current menu
     */
    default void attachMenu(Editor editor, String name, TreeBuilder.Menu menu) {

    }

    /**
     * Load resource from nbt data
     */
    default Resources loadResources(CompoundTag tag) {
        return Resources.fromNBT(tag);
    }

}
