package com.lowdragmc.lowdraglib2.gui.factory;

import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.editor_outdated.ui.UIEditor;
import com.lowdragmc.lowdraglib2.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib2.gui.modular.ModularUI;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class UIEditorFactory extends UIFactory<UIEditorFactory> implements IUIHolder {

	public static final UIEditorFactory INSTANCE = new UIEditorFactory();

	private UIEditorFactory(){
		super(LDLib2.id("ui_editor"));
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
				.widget(new UIEditor(LDLib2.MOD_ID));
	}

	@Override
	public boolean isInvalid() {
		return false;
	}

	@Override
	public boolean isRemote() {
		return LDLib2.isRemote();
	}

	@Override
	public void markAsDirty() {

	}
}
