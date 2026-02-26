package com.lowdragmc.lowdraglib2.integration.kjs.ui;

import dev.latvian.mods.kubejs.event.*;
import dev.latvian.mods.kubejs.script.ScriptTypePredicate;

public interface UIEvents {
    EventGroup INSTANCE = EventGroup.of("LDLib2UI");
    TargetedEventHandler<String> PLAYER = INSTANCE.add("player", ScriptTypePredicate.ALL, () -> KJSPlayerUIMenuType.PlayerUIEventJS.class).requiredTarget(EventTargetType.STRING);
    TargetedEventHandler<String> BLOCK = INSTANCE.add("block", ScriptTypePredicate.ALL, () -> KJSBlockUIMenuType.BlockUIEventJS.class).requiredTarget(EventTargetType.STRING);
    TargetedEventHandler<String> ITEM = INSTANCE.add("item", ScriptTypePredicate.ALL, () -> KJSHeldItemUIMenuType.ItemUIEventJS.class).requiredTarget(EventTargetType.STRING);
}
