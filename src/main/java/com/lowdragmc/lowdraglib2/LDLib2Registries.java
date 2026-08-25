package com.lowdragmc.lowdraglib2;

import com.lowdragmc.lowdraglib2.editor.resource.BuiltinResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.FileResourceProvider;
import com.lowdragmc.lowdraglib2.editor.resource.ResourceProviderType;
import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.registry.AutoRegistry;
import com.lowdragmc.lowdraglib2.registry.LDLRegistry;
import com.lowdragmc.lowdraglib2.test.ui.IMenuTest;
import com.lowdragmc.lowdraglib2.uitest.mp.MPScenario;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class LDLib2Registries {
    public final static AutoRegistry.LDLibRegister<UIElement, Supplier<UIElement>> UI_ELEMENTS = AutoRegistry.LDLibRegister
            .create(LDLib2.id("ui_element"), UIElement.class, AutoRegistry::noArgsCreator);

    public final static LDLRegistry.String<ResourceProviderType> RESOURCE_PROVIDER_TYPES = new LDLRegistry.String<>(LDLib2.id("resource_provider_types"));

    @Nullable
    public static AutoRegistry.LDLibRegister<IMenuTest, Supplier<IMenuTest>> MENU_TESTS;

    /**
     * Multi-process test scenarios. Deliberately a dist-neutral registry: the dedicated-server
     * process of a {@code runMpTest} run discovers scenarios through it too.
     *
     * @see com.lowdragmc.lowdraglib2.uitest.mp.MPScenario
     */
    @Nullable
    public static AutoRegistry.LDLibRegister<MPScenario, Supplier<MPScenario>> MP_SCENARIOS;

    static {
        if (Platform.isDevEnv()) {
            MP_SCENARIOS = AutoRegistry.LDLibRegister.create(LDLib2.id("mp_scenario"), MPScenario.class, AutoRegistry::noArgsCreator);
            MENU_TESTS = AutoRegistry.LDLibRegister.create(LDLib2.id("menu_test"), IMenuTest.class, AutoRegistry::noArgsCreator);
            for (var menuTest : MENU_TESTS) {
                PlayerUIMenuType.register(LDLib2.id(menuTest.annotation().name()), player -> {
                    var test = menuTest.value().get();
                    test.init(player);
                    return test;
                });
            }
        }
    }

    public static void init() {
        RESOURCE_PROVIDER_TYPES.register(BuiltinResourceProvider.TYPE.getTypeName(), BuiltinResourceProvider.TYPE);
        RESOURCE_PROVIDER_TYPES.register(FileResourceProvider.TYPE.getTypeName(), FileResourceProvider.TYPE);
    }
}
