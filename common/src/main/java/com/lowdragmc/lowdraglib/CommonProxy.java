package com.lowdragmc.lowdraglib;

import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.lowdraglib.gui.factory.*;
import com.lowdragmc.lowdraglib.kjs.ui.BlockUIJSFactory;
import com.lowdragmc.lowdraglib.kjs.ui.ItemUIJSFactory;
import com.lowdragmc.lowdraglib.networking.LDLNetworking;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;


public class CommonProxy {
    public static void init() {
        LDLNetworking.init();
        UIFactory.register(BlockEntityUIFactory.INSTANCE);
        UIFactory.register(HeldItemUIFactory.INSTANCE);
        UIFactory.register(UIEditorFactory.INSTANCE);
        if (LDLib.isKubejsLoaded()) {
            UIFactory.register(BlockUIJSFactory.INSTANCE);
            UIFactory.register(ItemUIJSFactory.INSTANCE);
        }
        AnnotationDetector.init();
        TypedPayloadRegistries.init();
    }
}
