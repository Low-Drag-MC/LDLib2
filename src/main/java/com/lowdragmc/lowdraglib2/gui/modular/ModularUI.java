package com.lowdragmc.lowdraglib2.gui.modular;

import com.google.common.base.Preconditions;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib2.gui.widget.Widget;
import com.lowdragmc.lowdraglib2.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import lombok.Getter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import javax.annotation.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Author: KilaBash
 * Date: 2022/04/23
 * Description: ModularUI.
 */
public final class ModularUI {

    @Getter
    private final HashMap<Slot, SlotWidget> slotMap = new LinkedHashMap<>();
    public final WidgetGroup mainGroup;
    @Getter
    private int screenWidth, screenHeight;
    @Getter
    private int width, height;
    @Getter
    private boolean fullScreen;
    @OnlyIn(Dist.CLIENT)
    private ModularUIGuiContainer guiContainer;
    private ModularUIContainer container;
    private final List<Runnable> uiCloseCallback;
    @Getter
    private long tickCount;

    /**
     * UIHolder of this modular UI
     */
    public final IUIHolder holder;
    public final Player entityPlayer;

    public ModularUI(Size size, IUIHolder holder, Player entityPlayer) {
        this(new WidgetGroup(Position.ORIGIN, size), holder, entityPlayer);
    }

    public ModularUI(int width, int height, IUIHolder holder, Player entityPlayer) {
        this(Size.of(width, height), holder, entityPlayer);
    }

    public ModularUI(WidgetGroup mainGroup, IUIHolder holder, Player entityPlayer) {
        this.mainGroup = mainGroup;
        mainGroup.setSelfPosition(Position.ORIGIN);
        this.width = mainGroup.getSize().width;
        this.height = mainGroup.getSize().height;
        this.holder = holder;
        this.entityPlayer = entityPlayer;
        this.uiCloseCallback = new ArrayList<>();
    }

    public ModularUI(IUIHolder holder, Player entityPlayer) {
        this(0, 0, holder, entityPlayer);
        fullScreen = true;
    }

    public void setFullScreen() {
        this.fullScreen = true;
        setSize(getScreenWidth(), getScreenHeight());
    }

    @Nullable
    public Widget getFirstWidgetById(String regex) {
        return mainGroup.getFirstWidgetById(Pattern.compile(regex));
    }

    public List<Widget> getWidgetsById(String regex) {
        return mainGroup.getWidgetsById(Pattern.compile(regex));
    }

    public ModularUIContainer getModularUIContainer() {
        return container;
    }

    //WARNING! WIDGET CHANGES SHOULD BE *STRICTLY* SYNCHRONIZED BETWEEN SERVER AND CLIENT,
    //OTHERWISE ID MISMATCH CAN HAPPEN BETWEEN ASSIGNED SLOTS!
    public void addNativeSlot(Slot slotHandle, SlotWidget slotWidget) {
        if (this.slotMap.containsKey(slotHandle)) {
            LDLib2.LOGGER.error("duplicated slot {}, {}", slotHandle, slotWidget);
        }
        this.slotMap.put(slotHandle, slotWidget);
        if (container != null) {
            container.addSlot(slotHandle);
        }
    }

    //WARNING! WIDGET CHANGES SHOULD BE *STRICTLY* SYNCHRONIZED BETWEEN SERVER AND CLIENT,
    //OTHERWISE ID MISMATCH CAN HAPPEN BETWEEN ASSIGNED SLOTS!
    public void removeNativeSlot(Slot slotHandle) {
        if (this.slotMap.containsKey(slotHandle)) {
            this.slotMap.remove(slotHandle);
            if (container != null) {
                container.removeSlot(slotHandle);
            }
        }
    }

    public void setModularUIContainer(ModularUIContainer container) {
        this.container = container;
        for (Slot slot : slotMap.keySet()) {
            this.container.addSlot(slot);
        }
    }


    public void registerCloseListener(Runnable runnable) {
        uiCloseCallback.add(runnable);
    }

    public void triggerCloseListeners() {
        uiCloseCallback.forEach(Runnable::run);
    }

    @OnlyIn(Dist.CLIENT)
    public ModularUIGuiContainer getModularUIGui() {
        return guiContainer;
    }

    @OnlyIn(Dist.CLIENT)
    public void setModularUIGui(ModularUIGuiContainer modularUIGuiContainer) {
        this.guiContainer = modularUIGuiContainer;
    }

    public List<Widget> getFlatVisibleWidgetCollection() {
        List<Widget> widgetList = new ArrayList<>();

        for (Widget widget : mainGroup.widgets) {
            if (!widget.isVisible()) continue;
            widgetList.add(widget);

            if (widget instanceof WidgetGroup)
                widgetList.addAll(((WidgetGroup) widget).getContainedWidgets(false));
        }

        return widgetList;
    }


    void addTick() {
        this.tickCount += 1;
    }
    
    public List<Widget> getFlatWidgetCollection() {
        List<Widget> widgetList = new ArrayList<>();
        for (Widget widget : mainGroup.widgets) {
            widgetList.add(widget);
            if (widget instanceof WidgetGroup) {
                widgetList.addAll(((WidgetGroup) widget).getContainedWidgets(true));
            }
        }
        return widgetList;
    }

    @OnlyIn(Dist.CLIENT)
    public void setSize(int width, int height) {
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            mainGroup.setSize(Size.of(width, height));
            if (getModularUIGui() != null) {
                getModularUIGui().init();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void updateScreenSize(int screenWidth, int screenHeight) {
        if (fullScreen && (screenWidth != width || screenHeight != height)) {
            width = screenWidth;
            height = screenHeight;
            if (getModularUIGui() != null) {
                getModularUIGui().init();
            }
            return;
        }
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        Position displayOffset = Position.of(getGuiLeft(), getGuiTop());
        mainGroup.setParentPosition(displayOffset);
        mainGroup.onScreenSizeUpdate(screenWidth, screenHeight);
    }

    public void initWidgets() {
        mainGroup.setGui(this);
        mainGroup.initWidget();
    }

    public int getGuiLeft() {
        return (getScreenWidth() - getWidth()) / 2;
    }

    public int getGuiTop() {
        return (getScreenHeight() - getHeight()) / 2;
    }

    public Rectangle toScreenCoords(Rectangle widgetRect) {
        return new Rectangle(getGuiLeft() + widgetRect.x, getGuiTop() + widgetRect.y, widgetRect.width, widgetRect.height);
    }

    public ModularUI widget(Widget widget) {
        Preconditions.checkNotNull(widget);
        mainGroup.addWidget(widget);
        return this;
    }

    public ModularUI background(IGuiTexture... textures) {
        mainGroup.setBackground(textures);
        return this;
    }
}
