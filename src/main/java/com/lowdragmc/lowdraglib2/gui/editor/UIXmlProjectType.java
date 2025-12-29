package com.lowdragmc.lowdraglib2.gui.editor;

import com.lowdragmc.lowdraglib2.editor.project.IProject;
import com.lowdragmc.lowdraglib2.editor.project.ProjectType;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;

import java.io.File;
import java.nio.file.Files;

public class UIXmlProjectType extends ProjectType {
    public static final UIXmlProjectType TYPE = new UIXmlProjectType();

    private UIXmlProjectType() {
        super(Icons.XML, "project.xml", ".xml", UIXmlProject::new);
    }

    @Override
    public void saveProjectToFile(IProject project, File file) throws Exception {
        if (project instanceof UIXmlProject xmlProject) {
            Files.writeString(file.toPath(), xmlProject.getXml());
        }
    }

    @Override
    public boolean isProjectDirty(IProject project, File file) throws Exception {
        if (project instanceof UIXmlProject xmlProject) {
            var path = file.toPath();
            if (!Files.exists(path)) {
                return true;
            } else {
                var rawText = new String(Files.readAllBytes(path));
                return !rawText.equals(xmlProject.getXml());
            }
        }
        return super.isProjectDirty(project, file);
    }

    @Override
    public UIXmlProject loadProjectFromFile(File file) throws Exception {
        var path = file.toPath();
        if (!Files.exists(path)) {
            return null;
        } else {
            var rawText = new String(Files.readAllBytes(path));
            return new UIXmlProject().setXml(rawText);
        }
    }
}
