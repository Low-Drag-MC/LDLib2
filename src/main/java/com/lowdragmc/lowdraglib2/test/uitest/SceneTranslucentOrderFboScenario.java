package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import net.minecraft.world.level.Level;

/** {@link SceneTranslucentOrderScenario} drawn into the textures of an {@code FBOWorldSceneRenderer} and blitted from there. */
@LDLRegisterClient(name = "scene_translucent_order_fbo", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SceneTranslucentOrderFboScenario extends SceneTranslucentOrderScenario {

    @Override
    protected Scene createScene(Scene scene, Level world) {
        return scene.createScene(world, true, null);
    }
}
