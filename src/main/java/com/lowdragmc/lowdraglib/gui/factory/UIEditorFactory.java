package com.lowdragmc.lowdraglib.gui.factory;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.editor.ui.UIEditor;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class UIEditorFactory extends UIFactory<UIEditorFactory> implements IUIHolder {

	public static final UIEditorFactory INSTANCE = new UIEditorFactory();

	private UIEditorFactory(){
		super(LDLib.location("ui_editor"));
	}

	@Override
	protected ModularUI createUITemplate(UIEditorFactory holder, Player entityPlayer) {
		return createUI(entityPlayer);
	}

	@Override
	protected UIEditorFactory readHolderFromSyncData(RegistryFriendlyByteBuf syncData) {
		return this;
	}

	@Override
	protected void writeHolderToSyncData(RegistryFriendlyByteBuf syncData, UIEditorFactory holder) {

	}

	@Override
	public ModularUI createUI(Player entityPlayer) {
		return new ModularUI(this, entityPlayer)
				.widget(new UIEditor(LDLib.MOD_ID));
	}

	@Override
	public boolean isInvalid() {
		return false;
	}

	@Override
	public boolean isRemote() {
		return LDLib.isRemote();
	}

	@Override
	public void markAsDirty() {

	}
}
