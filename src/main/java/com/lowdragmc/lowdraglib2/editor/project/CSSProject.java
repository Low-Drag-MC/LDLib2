package com.lowdragmc.lowdraglib2.editor.project;

import com.lowdragmc.lowdraglib2.editor.resource.*;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.appliedenergistics.yoga.YogaEdge;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;

public class CSSProject implements IProject {
    public static final ProjectType TYPE = new CSSProjectType();

    @Getter
    private final Resources resources;
    @Getter
    private final CodeEditor cssEditor;
    private final View editorView = new View();
    @Getter
    private String css;

    public CSSProject() {
        this("");
    }

    public CSSProject(String css) {
        this.css = css;
        this.resources = Resources.of(
                UIResource.INSTANCE,
                ColorsResource.INSTANCE,
                TexturesResource.INSTANCE
        );
        cssEditor = new CodeEditor();
        cssEditor.setLanguage(Languages.CSS);
        cssEditor.contentView.layout(layout -> layout.setPadding(YogaEdge.ALL, 2));
        cssEditor.contentView.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        cssEditor.textAreaStyle(style -> style.focusOverlay(IGuiTexture.EMPTY));
        cssEditor.layout(layout -> {
            layout.setPadding(YogaEdge.ALL, 2);
            layout.setHeightPercent(100);
            layout.setWidthPercent(100);
        });
        cssEditor.style(style -> style.backgroundTexture(Sprites.RECT_SOLID));
        cssEditor.setLinesResponder(this::onCSSChanged);

        editorView.addChild(cssEditor);
    }

    private void onCSSChanged(String[] lines) {
        this.css = String.join("\n", lines);
    }

    @Override
    public ProjectType getProjectType() {
        return TYPE;
    }

    @Override
    public void onLoad(Editor editor) {
        IProject.super.onLoad(editor);
        editor.centerWindow.getLeftTop().addView(editorView);
    }

    @Override
    public void onClosed(Editor editor) {
        IProject.super.onClosed(editor);
        editorView.removeSelf();
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        var tag = new CompoundTag();
        tag.putString("css", css);
        return tag;
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag tag) {
        this.css = tag.getString("css");
    }

    private static class CSSProjectType extends ProjectType {
        public CSSProjectType() {
            super(Icons.CSS, "project.css", ".css", CSSProject::new);
        }

        @Override
        public IProject loadProjectFromFile(File file) throws Exception {
            var css = Files.readString(file.toPath());
            return new CSSProject(css);
        }

        @Override
        public void saveProjectToFile(IProject project, File file) throws Exception {
            if (project instanceof CSSProject cssProject) {
                Files.writeString(file.toPath(), cssProject.getCss());
            }
        }

        @Override
        public boolean isProjectDirty(IProject project, File file) throws Exception {
            if (project instanceof CSSProject cssProject) {
                return !cssProject.getCss().equals(Files.readString(file.toPath()));
            }
            return true;
        }
    }
}
