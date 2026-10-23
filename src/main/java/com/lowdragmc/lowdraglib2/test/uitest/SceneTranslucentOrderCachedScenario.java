package com.lowdragmc.lowdraglib2.test.uitest;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Scene;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import net.minecraft.world.level.Level;

/** {@link SceneTranslucentOrderScenario} with the terrain drawn from the compiled cache. */
@LDLRegisterClient(name = "scene_translucent_order_cached", group = "ldlib2", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SceneTranslucentOrderCachedScenario extends SceneTranslucentOrderScenario {

    @Override
    protected Scene createScene(Scene scene, Level world) {
        return scene.useCacheBuffer().createScene(world);
    }
}
