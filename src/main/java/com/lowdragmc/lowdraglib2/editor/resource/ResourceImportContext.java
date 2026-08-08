package com.lowdragmc.lowdraglib2.editor.resource;

import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;
import lombok.Getter;

import org.jetbrains.annotations.Nullable;
import java.io.File;
import java.util.function.BiConsumer;

/**
 * Everything a {@link Resource#importFile} implementation needs to turn a file dropped onto the editor
 * into a resource, and the way it reports back.
 * <p>
 * Importing is allowed to take as long as it likes — asking the player where an image should be copied
 * to means waiting for a dialog — so the result is delivered through {@link #complete} rather than
 * returned. Exactly one of {@link #complete}, {@link #cancel} or {@link #fail} must be called, and only
 * once; further calls are ignored.
 */
public class ResourceImportContext<T> {
    @Getter
    private final Resource<T> resource;
    /** The provider the imported resource will be created in. */
    @Getter
    private final IResourceProvider<T> provider;
    /** The file that was dropped. It is not owned by the editor and must not be moved or deleted. */
    @Getter
    private final File file;
    /** The element to anchor dialogs and notifications to. */
    @Getter
    private final UIElement owner;
    @Getter
    @Nullable
    private final Editor editor;

    private final BiConsumer<String, T> onComplete;
    private final Runnable onCancel;
    private boolean finished;

    public ResourceImportContext(Resource<T> resource, IResourceProvider<T> provider, File file,
                                 UIElement owner, @Nullable Editor editor,
                                 BiConsumer<String, T> onComplete, Runnable onCancel) {
        this.resource = resource;
        this.provider = provider;
        this.file = file;
        this.owner = owner;
        this.editor = editor;
        this.onComplete = onComplete;
        this.onCancel = onCancel;
    }

    /**
     * The dropped file's name without its extension, reduced to characters a resource path accepts.
     * A sensible default name for the resource being created.
     */
    public String suggestedName() {
        var name = file.getName();
        var dot = name.indexOf('.');
        return ResourceFileImport.sanitizeName(dot == -1 ? name : name.substring(0, dot));
    }

    /** Creates the resource under {@link #suggestedName()}. */
    public void complete(T value) {
        complete(suggestedName(), value);
    }

    /**
     * Creates the resource. The name is made unique within the provider by the caller, so it does not
     * have to be free.
     *
     * @param name  the resource name, without the resource file extension.
     * @param value the imported resource.
     */
    public void complete(@Nullable String name, T value) {
        if (finished) return;
        finished = true;
        if (value == null) {
            onCancel.run();
            return;
        }
        onComplete.accept(name == null || name.isBlank() ? suggestedName() : name, value);
    }

    /** Abandons the import silently, e.g. because the player closed a dialog. */
    public void cancel() {
        if (finished) return;
        finished = true;
        onCancel.run();
    }

    /**
     * Abandons the import and tells the player why.
     *
     * @param messageKey a translation key describing what went wrong.
     */
    public void fail(String messageKey) {
        if (finished) return;
        Dialog.showNotification("editor.resource.import_failed", messageKey, null).show(owner);
        cancel();
    }
}
