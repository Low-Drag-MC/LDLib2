package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils.BlockModelObject;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;
import org.appliedenergistics.yoga.YogaEdge;

import java.util.List;

@LDLRegisterClient(name="scene_editor", registry = "ui_test")
@NoArgsConstructor
public class TestSceneEditor implements IUITest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        var sceneEditor = new SceneEditor();
        sceneEditor.layout(layout -> {
            layout.setWidthPercent(100);
            layout.setFlex(1);
        });
        sceneEditor.scene
                .createScene(entityPlayer.level())
                .setTickWorld(true)
                .setRenderedCore(List.of(
                        entityPlayer.getOnPos(),
                        entityPlayer.getOnPos().offset(0, 0, 1),
                        entityPlayer.getOnPos().offset(1, 0, 0),
                        entityPlayer.getOnPos().offset(1, 0, 1),
                        entityPlayer.getOnPos().offset(-1, 0, 0),
                        entityPlayer.getOnPos().offset(-1, 0, -1),
                        entityPlayer.getOnPos().offset(0, 0, -1),
                        entityPlayer.getOnPos().offset(1, 0, -1),
                        entityPlayer.getOnPos().offset(-1, 0, 1)
                ))
                .useCacheBuffer();
        root.layout(layout -> {
            layout.setWidth(300);
            layout.setHeight(300);
            layout.setPadding(YogaEdge.ALL, 10);
        }).setId("root").getStyle().backgroundTexture(Sprites.BORDER);
        root.addChildren(sceneEditor);
        var blockModel = new BlockModelObject();
        blockModel.transform().position(entityPlayer.position().toVector3f().add(0, 1, 0));
        sceneEditor.addSceneObject(blockModel);
        sceneEditor.setTransformGizmoTarget(blockModel.transform());
        return new ModularUI(UI.of(root));
    }

}