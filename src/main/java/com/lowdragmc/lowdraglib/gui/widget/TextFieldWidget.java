package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.editor.annotation.*;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Size;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import dev.latvian.mods.rhino.util.RemapForJS;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Configurable(name = "widget.text_field", collapse = false)
@LDLRegister(name = "text_field", group = "widget.basic")
public class TextFieldWidget extends Widget implements IConfigurableWidget {

    @OnlyIn(Dist.CLIENT)
    protected EditBox textField;

    @Configurable(name = "ldlib.gui.editor.name.maxStringLength")
    @NumberRange(range = {0, Integer.MAX_VALUE})
    protected int maxStringLength = Integer.MAX_VALUE;
    protected Function<String, String> textValidator = (s)->s;
    protected Supplier<String> textSupplier;
    protected Consumer<String> textResponder;

    @Configurable(name = "ldlib.gui.editor.name.currentString")
    protected String currentString;

    @Configurable(name = "ldlib.gui.editor.name.isBordered")
    protected boolean isBordered;

    @Configurable(name = "ldlib.gui.editor.name.color")
    @NumberColor
    protected int textColor = -1;

    protected float wheelDur;
    protected NumberFormat numberInstance;
    protected Component hover;
    private boolean isDragging;

    public TextFieldWidget() {
        this(0, 0, 60, 15, null, null);
    }

    public TextFieldWidget(int xPosition, int yPosition, int width, int height, Supplier<String> textSupplier, Consumer<String> textResponder) {
        super(Position.of(xPosition, yPosition), Size.of(width, height));
        if (isRemote()) {
            Font fontRenderer = Minecraft.getInstance().font;
            this.textField = new EditBox(fontRenderer, xPosition, yPosition, width, height, Component.literal("text field"));
            this.textField.setBordered(true);
            isBordered = true;
            this.textField.setMaxLength(this.maxStringLength);
            this.textField.setResponder(this::onTextChanged);
        }
        this.textSupplier = textSupplier;
        this.textResponder = textResponder;
    }

    public TextFieldWidget setTextSupplier(Supplier<String> textSupplier) {
        this.textSupplier = textSupplier;
        return this;
    }

    public TextFieldWidget setTextResponder(Consumer<String> textResponder) {
        this.textResponder = textResponder;
        return this;
    }

    public TextFieldWidget setBackground(IGuiTexture background) {
        super.setBackground(background);
        return this;
    }

    @ConfigSetter(field = "currentString")
    public TextFieldWidget setCurrentString(Object currentString) {
        this.currentString = currentString.toString();
        if (isRemote()) {
            if (!this.textField.getValue().equals(currentString)) {
                this.textField.setValue(currentString.toString());
            }
        }
        return this;
    }

    public String getCurrentString() {
        return this.currentString == null ? "" : this.currentString;
    }

    public String getRawCurrentString() {
        if (isRemote()) {
            return textField.getValue();
        }
        return getCurrentString();
    }

    @Override
    public void onFocusChanged(@Nullable Widget lastFocus, Widget focus) {
        this.textField.setFocused(isFocus());
    }

    @Override
    protected void onPositionUpdate() {
        if (isRemote() && textField != null) {
            Position position = getPosition();
            Size size = getSize();
            this.textField.setX(isBordered ? position.x : position.x + 2);
            this.textField.setY(isBordered ? position.y : position.y + (size.height - Minecraft.getInstance().font.lineHeight) / 2 + 1);
        }
    }

    @Override
    protected void onSizeUpdate() {
        if (isRemote() && textField != null) {
            Position position = getPosition();
            Size size = getSize();
            this.textField.setWidth(isBordered ? size.width : size.width - 2);
//            this.textField.setHeight( size.height);
            this.textField.setY(isBordered ? position.y : position.y + (getSize().height -  Minecraft.getInstance().font.lineHeight) / 2 + 1);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        this.textField.render(graphics, mouseX, mouseY, partialTicks);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1,1,1,1);
        drawOverlay(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            isDragging = true;
        }
        setFocus(isMouseOverElement(mouseX, mouseY));
        this.textField.setFocused(isMouseOverElement(mouseX, mouseY));
        return this.textField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return keyCode != 256 && (this.textField.keyPressed(keyCode, scanCode, modifiers) || isFocus());
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.textField.charTyped(codePoint, modifiers);
    }

    @Override
    public void updateScreen() {
        if (this.isVisible() && this.isActive() && textSupplier != null && isClientSideWidget&& !textSupplier.get().equals(getCurrentString())) {
            setCurrentString(textSupplier.get());
        }
    }

    @Override
    public void writeInitialData(RegistryFriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        buffer.writeUtf(getCurrentString());
    }

    @Override
    public void readInitialData(RegistryFriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        setCurrentString(buffer.readUtf());
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (textSupplier != null && !textSupplier.get().equals(getCurrentString())) {
            setCurrentString(textSupplier.get());
            writeUpdateInfo(1, buffer -> buffer.writeUtf(getCurrentString()));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, RegistryFriendlyByteBuf buffer) {
        super.readUpdateInfo(id, buffer);
        if (id == 1) {
            setCurrentString(buffer.readUtf());
        }
    }

    protected void onTextChanged(String newTextString) {
        String lastText = currentString;
        String newText = textValidator.apply(newTextString);
        if (!newText.equals(lastText)) {
            this.textField.setTextColor(textColor);
            setCurrentString(newText);
            if (isClientSideWidget && textResponder != null) {
                textResponder.accept(newText);
            }
            writeClientAction(1, buffer -> buffer.writeUtf(newText));
        } else if (!newTextString.equals(newText)){
            this.textField.setTextColor(0xffdf0000);
        } else {
            this.textField.setTextColor(textColor);
        }
    }

    @Override
    public void handleClientAction(int id, RegistryFriendlyByteBuf buffer) {
        super.handleClientAction(id, buffer);
        if (id == 1) {
            String lastText = getCurrentString();
            String newText = textValidator.apply(buffer.readUtf());
            newText = newText.substring(0, Math.min(newText.length(), maxStringLength));
            if (lastText == null || !lastText.equals(newText)) {
                setCurrentString(newText);
                if (textResponder != null) {
                    this.textResponder.accept(newText);
                }
            }
        }
    }

    @ConfigSetter(field = "isBordered")
    public TextFieldWidget setBordered(boolean bordered) {
        isBordered = bordered;
        if (isRemote()) {
            this.textField.setBordered(bordered);
            onPositionUpdate();
            onSizeUpdate();
        }
        return this;
    }

    @ConfigSetter(field = "textColor")
    public TextFieldWidget setTextColor(int textColor) {
        this.textColor = textColor;
        if (isRemote()) {
            this.textField.setTextColor(textColor);
        }
        return this;
    }

    @ConfigSetter(field = "maxStringLength")
    public TextFieldWidget setMaxStringLength(int maxStringLength) {
        this.maxStringLength = maxStringLength;
        if (isRemote()) {
            this.textField.setMaxLength(maxStringLength);
        }
        return this;
    }

    public TextFieldWidget setValidator(Function<String, String> validator) {
        this.textValidator = validator;
        return this;
    }

    public TextFieldWidget setCompoundTagOnly() {
        setValidator(s -> {
            try {
                TagParser.parseTag(s);
                return s;
            } catch (Exception ignored) { }
            return this.currentString;
        });
        hover = Component.translatable("ldlib.gui.text_field.compound_tag");
        return this;
    }

    public TextFieldWidget setResourceLocationOnly() {
        setValidator(s -> {
            try {
                s = s.toLowerCase();
                s = s.replace(' ', '_');
                ResourceLocation.parse(s);
                return s;
            } catch (NumberFormatException | ResourceLocationException ignored) { }
            return this.currentString;
        });
        hover = Component.translatable("ldlib.gui.text_field.resourcelocation");
        return this;
    }

    @RemapForJS("setNumbersOnlyLong")
    public TextFieldWidget setNumbersOnly(long minValue, long maxValue) {
        setValidator(s -> {
            try {
                if (s == null || s.isEmpty()) return minValue + "";
                long value = Long.parseLong(s);
                if (minValue <= value && value <= maxValue) return s;
                if (value < minValue) return minValue + "";
                return maxValue + "";
            } catch (NumberFormatException ignored) { }
            return this.currentString;
        });
        if (minValue == Long.MIN_VALUE && maxValue == Long.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.3");
        } else if (minValue == Long.MIN_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.2", maxValue);
        } else if (maxValue == Long.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.1", minValue);
        } else {
            hover = Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue);
        }
        return setWheelDur(1);
    }

    @RemapForJS("setNumbersOnlyInt")
    public TextFieldWidget setNumbersOnly(int minValue, int maxValue) {
        setValidator(s -> {
            try {
                if (s == null || s.isEmpty()) return minValue + "";
                int value = Integer.parseInt(s);
                if (minValue <= value && value <= maxValue) return s;
                if (value < minValue) return minValue + "";
                return maxValue + "";
            } catch (NumberFormatException ignored) { }
            return this.currentString;
        });
        if (minValue == Integer.MIN_VALUE && maxValue == Integer.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.3");
        } else if (minValue == Integer.MIN_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.2", maxValue);
        } else if (maxValue == Integer.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.1", minValue);
        } else {
            hover = Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue);
        }
        return setWheelDur(1);
    }

    @RemapForJS("setNumbersOnlyFloat")
    public TextFieldWidget setNumbersOnly(float minValue, float maxValue) {
        setValidator(s -> {
            try {
                if (s == null || s.isEmpty()) return minValue + "";
                float value = Float.parseFloat(s);
                if (minValue <= value && value <= maxValue) return s;
                if (value < minValue) return minValue + "";
                return maxValue + "";
            } catch (NumberFormatException ignored) { }
            return this.currentString;
        });
        if (minValue == -Float.MAX_VALUE && maxValue == Float.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.3");
        } else if (minValue == -Float.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.2", maxValue);
        } else if (maxValue == Float.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.1", minValue);
        } else {
            hover = Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue);
        }
        return setWheelDur(0.1f);
    }

    public TextFieldWidget setWheelDur(float wheelDur) {
        this.wheelDur = wheelDur;
        this.numberInstance = NumberFormat.getNumberInstance();
        numberInstance.setMaximumFractionDigits(4);
        return this;
    }

    public TextFieldWidget setWheelDur(int digits, float wheelDur) {
        this.wheelDur = wheelDur;
        this.numberInstance = NumberFormat.getNumberInstance();
        numberInstance.setMaximumFractionDigits(digits);
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (wheelDur > 0 && numberInstance != null && isMouseOverElement(mouseX, mouseY) && isFocus()) {
            try {
                onTextChanged(numberInstance.format(Float.parseFloat(getCurrentString()) + (scrollY > 0 ? 1 : -1) * wheelDur));
            } catch (Exception ignored) {
            }
            setFocus(true);
            return true;
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && numberInstance != null && isFocus()) {
            try {
                onTextChanged(numberInstance.format(Float.parseFloat(getCurrentString()) + dragX * wheelDur));
            } catch (Exception ignored) {
            }
            setFocus(true);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (isMouseOverElement(mouseX, mouseY) && gui != null &&  gui.getModularUIGui() != null) {
            List<Component> tips = new ArrayList<>();
            if (tooltipTexts != null) {
                tips.addAll(tooltipTexts);
            }
            if (hover != null) {
                tips.add(hover);
            }
            if (wheelDur > 0 && numberInstance != null && isFocus()) {
                tips.add(Component.translatable("ldlib.gui.text_field.number.wheel", numberInstance.format(wheelDur)));
            }
            if (!tips.isEmpty()) {
                gui.getModularUIGui().setHoverTooltip(tips, ItemStack.EMPTY, null, null);
            }
        }
    }
}
