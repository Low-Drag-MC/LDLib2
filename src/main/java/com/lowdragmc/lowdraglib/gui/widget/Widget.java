package com.lowdragmc.lowdraglib.gui.widget;

import com.google.common.base.Preconditions;
import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.animation.Animation;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.modular.WidgetUIAccess;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.layout.Align;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Rect;
import com.lowdragmc.lowdraglib.utils.Size;
import com.mojang.blaze3d.platform.InputConstants;
import dev.latvian.mods.rhino.util.HideFromJS;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Widget is functional element of ModularUI
 * It can draw, perform actions, react to key press and mouse
 * It's information is also synced to client
 */
@SuppressWarnings("UnusedReturnValue")
@RemapPrefixForJS("kjs$")
@Configurable(name = "ldlib.gui.editor.group.basic_info", collapse = false)
@Accessors(chain = true)
public class Widget {

    @Getter
    protected ModularUI gui;
    @Setter
    protected WidgetUIAccess uiAccess;
    @Configurable(tips = "ldlib.gui.editor.tips.id")
    @Setter
    @Getter
    protected String id = "";
    @Getter
    private Position parentPosition = Position.ORIGIN;
    @Configurable(name = "ldlib.gui.editor.name.pos", tips = "ldlib.gui.editor.tips.pos")
    @Getter
    private Position selfPosition;
    @Getter
    private Position position;
    @Configurable(name = "ldlib.gui.editor.name.size")
    @Getter
    private Size size;
    @Configurable(name = "ldlib.gui.editor.name.align", tips = "ldlib.gui.editor.tips.align")
    @Getter @Setter
    private Align align = Align.NONE;
    @Getter @Setter
    private boolean isVisible;
    @Getter @Setter
    private boolean isActive;
    @Getter
    private boolean isFocus;
    @Getter
    protected boolean isClientSideWidget;
    @Getter
    @Configurable(name = "ldlib.gui.editor.name.hover_tips", tips = "ldlib.gui.editor.tips.hover_tips")
    protected final List<Component> tooltipTexts = new ArrayList<>();
    @Getter
    @Configurable(name = "ldlib.gui.editor.name.background")
    protected IGuiTexture backgroundTexture;
    @Configurable(name = "ldlib.gui.editor.name.draw_background_when_hover")
    protected boolean drawBackgroundWhenHover = true;
    @Configurable(name = "ldlib.gui.editor.name.hover_texture")
    protected IGuiTexture hoverTexture;
    @Getter
    @Setter
    @Configurable(name = "ldlib.gui.editor.name.overlayTexture")
    protected IGuiTexture overlay;
    @Getter
    protected WidgetGroup parent;
    @Getter
    protected Animation animation;
    @Getter
    protected boolean initialized;
    protected boolean tryToDrag = false;
    protected Supplier<Object> draggingProvider;
    protected BiFunction<Object, Position, IGuiTexture> draggingRenderer;
    protected Predicate<Object> draggingAccept = o -> false;
    protected Consumer<Object> draggingIn;
    protected Consumer<Object> draggingOut;
    protected Consumer<Object> draggingSuccess;
    protected Object draggingElement;

    public Widget(Position selfPosition, Size size) {
        Preconditions.checkNotNull(selfPosition, "selfPosition");
        Preconditions.checkNotNull(size, "size");
        this.selfPosition = selfPosition;
        this.size = size;
        this.position = this.parentPosition.add(selfPosition);
        this.isVisible = true;
        this.isActive = true;
    }

    public Widget(int x, int y, int width, int height) {
        this(new Position(x, y), new Size(width, height));
    }

    public Widget setClientSideWidget() {
        isClientSideWidget = true;
        return this;
    }

    @HideFromJS
    public Widget setHoverTooltips(String... tooltipText) {
        tooltipTexts.clear();
        appendHoverTooltips(tooltipText);
        return this;
    }

    @HideFromJS
    public Widget setHoverTooltips(Component... tooltipText) {
        tooltipTexts.clear();
        appendHoverTooltips(tooltipText);
        return this;
    }

    @HideFromJS
    public Widget setHoverTooltips(List<Component> tooltipText) {
        tooltipTexts.clear();
        appendHoverTooltips(tooltipText);
        return this;
    }

    public Widget appendHoverTooltips(String... tooltipText) {
        Arrays.stream(tooltipText).filter(Objects::nonNull).filter(s->!s.isEmpty()).map(
                Component::translatable).forEach(tooltipTexts::add);
        return this;
    }

    public Widget appendHoverTooltips(Component... tooltipText) {
        Arrays.stream(tooltipText).filter(Objects::nonNull).forEach(tooltipTexts::add);
        return this;
    }

    public Widget appendHoverTooltips(List<Component> tooltipText) {
        tooltipTexts.addAll(tooltipText);
        return this;
    }

    public Widget kjs$setHoverTooltips(Component... tooltipText) {
        tooltipTexts.clear();
        Arrays.stream(tooltipText).filter(Objects::nonNull).forEach(tooltipTexts::add);
        return this;
    }

    public Widget setBackground(IGuiTexture... backgroundTexture) {
        this.backgroundTexture = backgroundTexture.length > 1 ? new GuiTextureGroup(backgroundTexture) : backgroundTexture[0];
        return this;
    }

    public Widget setDrawBackgroundWhenHover(boolean drawBackgroundWhenHover) {
        this.drawBackgroundWhenHover = drawBackgroundWhenHover;
        return this;
    }

    public Widget setHoverTexture(IGuiTexture... hoverTexture) {
        this.hoverTexture = hoverTexture.length > 1 ? new GuiTextureGroup(hoverTexture) : hoverTexture[0];
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T> Widget setDraggingProvider(Supplier<T> draggingProvider, BiFunction<T, Position, IGuiTexture> draggingRenderer) {
        this.draggingProvider = (Supplier<Object>) draggingProvider;
        this.draggingRenderer = (BiFunction<Object, Position, IGuiTexture>) draggingRenderer;
        return this;
    }

    public Widget setDraggingConsumer(Predicate<Object> draggingAccept, Consumer<Object> draggingIn, Consumer<Object> draggingOut, Consumer<Object> draggingSuccess) {
        this.draggingAccept = draggingAccept;
        this.draggingIn = draggingIn;
        this.draggingOut = draggingOut;
        this.draggingSuccess = draggingSuccess;
        return this;
    }

    public void animation(Animation animation) {
        if (isRemote()) {
            this.animation = animation;
            this.animation.setWidget(this);
        } else {
            Runnable runnable = animation.getOnFinish();
            if (runnable != null){
                runnable.run();
            }
        }
    }

    public boolean inAnimate() {
        return animation != null && !animation.isFinish();
    }

    public void setGui(ModularUI gui) {
        this.gui = gui;
    }

    public void setParentPosition(Position parentPosition) {
        this.parentPosition = parentPosition;
        recomputePosition();
    }

    @ConfigSetter(field = "selfPosition")
    public void setSelfPosition(Position selfPosition) {
        if (this.selfPosition.equals(selfPosition)) return;
        this.selfPosition = selfPosition;
        recomputePosition();
        if (isParent(parent)) {
            parent.onChildSelfPositionUpdate(this);
        }
    }

    public final void setSelfPosition(int x, int y) {
        setSelfPosition(new Position(x, y));
    }

    public final void setSelfPositionX(int x) {
        setSelfPosition(x, getSelfPosition().y);
    }

    public final void setSelfPositionY(int y) {
        setSelfPosition(getSelfPosition().x, y);
    }

    public Position addSelfPosition(int addX, int addY) {
        setSelfPosition(new Position(selfPosition.x + addX, selfPosition.y + addY));
        return this.selfPosition;
    }

    public final int getSelfPositionX() {
        return getSelfPosition().x;
    }

    public final int getSelfPositionY() {
        return getSelfPosition().y;
    }

    @ConfigSetter(field = "size")
    public void setSize(Size size) {
        if (this.size.equals(size)) return;
        this.size = size;
        onSizeUpdate();
        if (isParent(parent)) {
            parent.onChildSizeUpdate(this);
        }
    }

    public final void setSize(int width, int height) {
        setSize(new Size(width, height));
    }

    public final void setSizeWidth(int width) {
        setSize(width, getSize().height);
    }

    public final void setSizeHeight(int height) {
        setSize(getSize().width, height);
    }

    public final int getPositionX() {
        return getPosition().x;
    }

    public final int getPositionY() {
        return getPosition().y;
    }

    public final int getSizeWidth() {
        return getSize().width;
    }

    public final int getSizeHeight() {
        return getSize().height;
    }

    public final Rect getRect() {
        return Rect.of(position, size);
    }

    @OnlyIn(Dist.CLIENT)
    public Rect2i toRectangleBox() {
        Position pos = getPosition();
        Size size = getSize();
        return new Rect2i(pos.x, pos.y, size.width, size.height);
    }

    protected void recomputePosition() {
        this.position = this.parentPosition.add(selfPosition);
        onPositionUpdate();
    }

    protected void onPositionUpdate() {
    }

    protected void onSizeUpdate() {
    }

    public boolean isMouseOverElement(double mouseX, double mouseY) {
        Position position = getPosition();
        Size size = getSize();
        return isMouseOver(position.x, position.y, size.width, size.height, mouseX, mouseY);
    }

    @Nullable
    public Widget getHoverElement(double mouseX, double mouseY) {
        Position position = getPosition();
        Size size = getSize();
        if (isMouseOver(position.x, position.y, size.width, size.height, mouseX, mouseY)) {
            return this;
        }
        return null;
    }

    public static boolean isMouseOver(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && x + width > mouseX && y + height > mouseY;
    }

    /**
     * Called on both sides to initialize widget data
     */
    public void initWidget() {
        initialized = true;
    }

    public void writeInitialData(RegistryFriendlyByteBuf buffer) {
    }

    public void readInitialData(RegistryFriendlyByteBuf buffer) {
        
    }
    
    /**
     * Called on serverside to detect changes and synchronize them with clients
     */
    public void detectAndSendChanges() {
    }

    /**
     * Called clientside every tick with this modular UI open
     */
    @OnlyIn(Dist.CLIENT)
    public void updateScreen() {
        if (align != Align.NONE && isParent(parent)) {
            switch (align) {
                case TOP_LEFT -> setSelfPosition(0, 0);
                case TOP_CENTER -> setSelfPosition((parent.getSize().width - getSize().width) / 2, 0);
                case TOP_RIGHT -> setSelfPosition(parent.getSize().width - getSize().width, 0);
                case LEFT_CENTER -> setSelfPosition(0, (parent.getSize().height - getSize().height) / 2);
                case CENTER -> setSelfPosition((parent.getSize().width - getSize().width) / 2, (parent.getSize().height - getSize().height) / 2);
                case RIGHT_CENTER -> setSelfPosition(parent.getSize().width - getSize().width, (parent.getSize().height - getSize().height) / 2);
                case BOTTOM_LEFT -> setSelfPosition(0, parent.getSize().height - getSize().height);
                case BOTTOM_CENTER -> setSelfPosition((parent.getSize().width - getSize().width) / 2, parent.getSize().height - getSize().height);
                case BOTTOM_RIGHT -> setSelfPosition(parent.getSize().width - getSize().width, parent.getSize().height - getSize().height);
            }
        }
        if (backgroundTexture != null) {
            backgroundTexture.updateTick();
        }
        if (hoverTexture != null) {
            hoverTexture.updateTick();
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void drawTooltipTexts(int mouseX, int mouseY) {
        if (tooltipTexts.size() > 0 && isMouseOverElement(mouseX, mouseY) && getHoverElement(mouseX, mouseY) == this && gui != null && gui.getModularUIGui() != null) {
            gui.getModularUIGui().setHoverTooltip(tooltipTexts, ItemStack.EMPTY, null, null);
        }
    }

    /**
     * Called each draw tick to draw this widget in GUI
     */
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawTooltipTexts(mouseX, mouseY);
    }

    @OnlyIn(Dist.CLIENT)
    protected void drawBackgroundTexture(@Nonnull GuiGraphics graphics, int mouseX, int mouseY) {
        var isHovered = isMouseOverElement(mouseX, mouseY);
        if (backgroundTexture != null && (!isHovered || drawBackgroundWhenHover)) {
            Position pos = getPosition();
            Size size = getSize();
            backgroundTexture.draw(graphics, mouseX, mouseY, pos.x, pos.y, size.width, size.height);
        }
        if (hoverTexture != null && isHovered && isActive()) {
            Position pos = getPosition();
            Size size = getSize();
            hoverTexture.draw(graphics, mouseX, mouseY, pos.x, pos.y, size.width, size.height);
        }
    }

    /**
     * Called each draw tick to draw this widget in GUI
     */
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawBackgroundTexture(graphics, mouseX, mouseY);
    }

    @OnlyIn(Dist.CLIENT)
    public void drawOverlay(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (overlay != null) {
            Position pos = getPosition();
            overlay.draw(graphics, mouseX, mouseY, pos.x, pos.y, size.width, size.height);
        }
    }

    /**
     * Called when mouse wheel is moved in GUI
     * For some -redacted- reason mouseX position is relative against GUI not game window as in other mouse events
     */
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    /**
     * Called when mouse is clicked in GUI
     */
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        draggingElement = null;
        tryToDrag = false;
        if (draggingProvider != null && isMouseOverElement(mouseX, mouseY)) {
            tryToDrag = true;
            return false;
        }
        return false;
    }

    /**
     * Called when mouse is pressed and hold down in GUI
     */
    @OnlyIn(Dist.CLIENT)
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isMouseOverElement(mouseX, mouseY) && tryToDrag && draggingProvider != null && draggingRenderer != null) {
            var element = draggingProvider.get();
            if (element != null) {
                getGui().getModularUIGui().setDraggingElement(element, draggingRenderer.apply(element, new Position((int) mouseX, (int) mouseY)));
            }
        }
        if (isMouseOverElement(mouseX, mouseY) && draggingAccept.test(getGui().getModularUIGui().getDraggingElement())) {
            var element = getGui().getModularUIGui().getDraggingElement();
            if (draggingElement != element && draggingIn != null) {
                draggingElement = element;
                draggingIn.accept(element);
            }
            return true;
        }
        if (draggingElement != null && draggingOut != null) {
            draggingOut.accept(draggingElement);
            draggingElement = null;
        }
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean mouseMoved(double mouseX, double mouseY) {
        return false;
    }

    /**
     * Called when mouse is released in GUI
     */
    @OnlyIn(Dist.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        tryToDrag = false;
        if (isMouseOverElement(mouseX, mouseY) && getGui() != null && draggingAccept.test(getGui().getModularUIGui().getDraggingElement())) {
            var element = getGui().getModularUIGui().getDraggingElement();
            if (draggingElement == element && draggingSuccess != null) {
                draggingSuccess.accept(element);
                draggingElement = null;
                return true;
            }
        }
        draggingElement = null;
        return false;
    }

    /**
     * Called when key is typed in GUI
     */
    @OnlyIn(Dist.CLIENT)
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    /**
     * setFocus should always be called after child widgets logic
     */
    @OnlyIn(Dist.CLIENT)
    public final void setFocus(boolean focus) {
        if (gui != null) {
            ModularUIGuiContainer guiContainer = gui.getModularUIGui();
            Widget lastFocus = guiContainer.lastFocus;
            if (!focus) {
                isFocus = false;
                if (guiContainer.lastFocus == this) {
                    guiContainer.lastFocus = null;
                }
                onFocusChanged(lastFocus, guiContainer.lastFocus);
            } else {
                if (guiContainer.switchFocus(this)) {
                    isFocus = true;
                    onFocusChanged(lastFocus, guiContainer.lastFocus);
                }
            }
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    public void onFocusChanged(@Nullable Widget lastFocus, Widget focus) {
        
    }

    /**
     * Read data received from server's {@link #writeUpdateInfo}
     */
    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, RegistryFriendlyByteBuf buffer) {
    }

    public void handleClientAction(int id, RegistryFriendlyByteBuf buffer) {
    }

    /**
     * Writes data to be sent to client's {@link #readUpdateInfo}
     */
    protected final void writeUpdateInfo(int id, Consumer<RegistryFriendlyByteBuf> buffer) {
        if (uiAccess != null && gui != null) {
            uiAccess.writeUpdateInfo(this, id, buffer);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected final void writeClientAction(int id, Consumer<RegistryFriendlyByteBuf> buffer) {
        if (uiAccess != null && !isClientSideWidget) {
            uiAccess.writeClientAction(this, id, buffer);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void playButtonClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isShiftDown() {
        long id = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isCtrlDown() {
        return Screen.hasControlDown();
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isAltDown() {
        long id = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(id, GLFW.GLFW_KEY_RIGHT_ALT);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isKeyDown(int keyCode) {
        long id = Minecraft.getInstance().getWindow().getWindow();
        return InputConstants.isKeyDown(id, keyCode);
    }

    @Environment(EnvType.CLIENT)
    public boolean isMouseDown(int button) {
        return gui != null && gui.getModularUIGui().isButtonPressed(button);
    }

    public boolean isRemote() {
        return (gui != null && gui.holder != null) ? gui.holder.isRemote() : LDLib.isRemote();
    }

    protected void setParent(WidgetGroup parent) {
        this.parent = parent;
    }

    public boolean isParent(WidgetGroup widgetGroup) {
        if (parent == null) return false;
        if (parent == widgetGroup) return true;
        return parent.isParent(widgetGroup);
    }

    @OnlyIn(Dist.CLIENT)
    public void onScreenSizeUpdate(int screenWidth, int screenHeight) {
    }

    @OnlyIn(Dist.CLIENT)
    public List<Rect2i> getGuiExtraAreas(Rect2i guiRect, List<Rect2i> list) {
        Rect2i rect2i = toRectangleBox();
        if (rect2i.getX() < guiRect.getX()
                || rect2i.getX() + rect2i.getWidth() > guiRect.getX() + guiRect.getWidth()
                || rect2i.getY() < guiRect.getY()
                || rect2i.getY() + rect2i.getHeight() > guiRect.getY() + guiRect.getHeight()) {
            list.add(toRectangleBox());
        }
        return list;
    }

}
