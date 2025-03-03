package com.lowdragmc.lowdraglib.test.ui;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.SceneEditorWidget;
import com.lowdragmc.lowdraglib.gui.editor.ui.sceneeditor.sceneobject.utils.BlockModelObject;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

@LDLRegisterClient(name="scene_object", group = "ui_test")
@NoArgsConstructor
public class TestSceneObject implements IUITest {
    @Override
    public ModularUI createUI(IUIHolder holder, Player entityPlayer) {
        var mc = Minecraft.getInstance();
        var sceneWidget = new SceneEditorWidget(0, 0,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(),
                mc.level);
        var playerPos = entityPlayer.getOnPos();
        sceneWidget.useCacheBuffer();
        sceneWidget.setRenderedCore(List.of(
                playerPos,
                playerPos.above(),
                playerPos.below(),
                playerPos.east(),
                playerPos.west(),
                playerPos.north(),
                playerPos.south()
        ));

        var testModel = new BlockModelObject();
        testModel.transform().position(new Vector3f(playerPos.above().getX(), playerPos.above().getY() + 1, playerPos.above().getZ()));
        testModel.transform().rotation(new Quaternionf().identity().rotationXYZ(30, 23, 75));
        sceneWidget.addSceneObject(testModel);
        sceneWidget.setTransformGizmoTarget(testModel.transform());

//        var sizeBox = new SizeBoxObject();
//        sizeBox.transform().position(new Vector3f(playerPos.below().getX(), playerPos.below().getY() + 1, playerPos.below().getZ()));
//        sceneWidget.addSceneObject(sizeBox);

        return IUITest.super.createUI(holder, entityPlayer)
                .widget(sceneWidget);
    }
}
