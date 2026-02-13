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

@LDLRegisterClient(name="scene_editor", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestSceneEditor implements IScreenTest {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        var root = new UIElement();
        var sceneEditor = new SceneEditor();
        sceneEditor.layout(layout -> {
            layout.widthPercent(100);
            layout.flex(1);
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
            layout.width(300);
            layout.height(300);
            layout.paddingAll(10);
        }).setId("root").getStyle().backgroundTexture(Sprites.BORDER);
        root.addChildren(sceneEditor);
        var blockModel = new BlockModelObject();
        var childModel = new BlockModelObject();
        blockModel.transform().position(entityPlayer.position().toVector3f().add(0, 1, 0));
        blockModel.transform().rotateLocal(entityPlayer.position().toVector3f().add(0, 1, 0));
        childModel.transform().parent(blockModel.transform(), false);
        sceneEditor.addSceneObject(blockModel);
        sceneEditor.addSceneObject(childModel);
        sceneEditor.setTransformGizmoTarget(childModel.transform());
        return new ModularUI(UI.of(root));
    }

}