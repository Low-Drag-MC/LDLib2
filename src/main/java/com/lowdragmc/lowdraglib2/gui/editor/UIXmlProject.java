package com.lowdragmc.lowdraglib2.gui.editor;

import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.editor.resource.*;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.gui.editor.view.UIXmlView;
import com.lowdragmc.lowdraglib2.utils.TagBuilder;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class UIXmlProject implements IProject {
    @Getter
    private String xml = "";
    @Getter
    private final Resources resources = Resources.of();

    // runtime
    @Nullable
    @Getter
    private Editor editor;
    @Nullable
    private UIXmlView xmlView;

    public UIXmlProject() {}

    @Override
    public ProjectType getProjectType() {
        return UIXmlProjectType.TYPE;
    }

    @Override
    public void initNewProject() {
        xml = """
                <?xml version="1.0" encoding="UTF-8" ?>
                <ldlib2-ui xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                           xsi:noNamespaceSchemaLocation="https://raw.githubusercontent.com/Low-Drag-MC/LDLib2/refs/heads/1.21/ldlib2-ui.xsd">
                    <stylesheet location="ldlib2:lss/mc.lss"/>
                    <style>
                        .flex-1 {
                            flex: 1;
                        }
                    </style>
                    <root class="panel_bg" style="width: 150; height: 300">
                        <button text="click me!"/>
                    </root>
                </ldlib2-ui>
                """;
    }

    public UIXmlProject setXml(String xml) {
        return setXml(xml, true);
    }

    public UIXmlProject setXml(String xml, boolean notifyChanged) {
        if (this.xml.equals(xml)) return this;
        this.xml = xml;
        if (notifyChanged && xmlView != null) xmlView.loadXmlFromProject();
        return this;
    }

    @Override
    public CompoundTag serializeProject(@NotNull HolderLookup.Provider provider) {
        return TagBuilder.compound().add("xml", xml).build();
    }

    @Override
    public void deserializeProject(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag nbt) {
        setXml(nbt.getString("xml"));
    }

    @Override
    public void onLoad(@Nonnull Editor editor) {
        IProject.super.onLoad(editor);
        this.editor = editor;
        this.xmlView = new UIXmlView(this);
        this.editor.centerWindow.getLeftTop().addView(this.xmlView);
    }

    @Override
    public void onClosed(@Nonnull Editor editor) {
        IProject.super.onClosed(editor);
        if (this.xmlView != null) {
            this.xmlView.removeSelf();
        }
        this.editor = null;
        this.xmlView = null;
    }

}
