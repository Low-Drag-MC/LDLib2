package com.lowdragmc.lowdraglib2.integration.kjs.ui;

import dev.latvian.mods.kubejs.event.*;

public interface UIEvents {
    EventGroup INSTANCE = EventGroup.of("LDLib2UI");
    TargetedEventHandler<String> PLAYER = INSTANCE.common("player", () -> KJSPlayerUIMenuType.PlayerUIEventJS.class).requiredTarget(EventTargetType.STRING);
    TargetedEventHandler<String> BLOCK = INSTANCE.common("block", () -> KJSBlockUIMenuType.BlockUIEventJS.class).requiredTarget(EventTargetType.STRING);
    TargetedEventHandler<String> ITEM = INSTANCE.common("item", () -> KJSHeldItemUIMenuType.ItemUIEventJS.class).requiredTarget(EventTargetType.STRING);
}
