package com.lowdragmc.lowdraglib2.gui.widget;

import com.lowdragmc.lowdraglib2.gui.util.ClickData;
import com.lowdragmc.lowdraglib2.math.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author KilaBash
 * @date 2023/3/16
 * @implNote ComponentTextWidget
 */
@Accessors(fluent = true)
public class ComponentPanelWidget extends Widget {
    protected int maxWidthLimit;
    @Setter @Nullable
    protected Consumer<List<Component>> textSupplier;
    @Setter
    protected BiConsumer<String, ClickData> clickHandler;
    protected List<Component> lastText = new ArrayList<>();
    @Getter
    protected List<FormattedCharSequence> cacheLines = Collections.emptyList();
    protected boolean isCenter = false;
    protected int space = 2;

    public ComponentPanelWidget(int x, int y, @Nonnull Consumer<List<Component>> textSupplier) {
        super(x, y, 0, 0);
        this.textSupplier = textSupplier;
        this.textSupplier.accept(lastText);
    }

    public ComponentPanelWidget(int x, int y, List<Component> text) {
        super(x, y, 0, 0);
        this.lastText.addAll(text);
    }

    public static Component withButton(Component textComponent, String componentData) {
        var style = textComponent.getStyle();
        style = style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "@!" + componentData));
        style = style.withColor(ChatFormatting.YELLOW);
        return textComponent.copy().withStyle(style);
    }

    public static Component withButton(Component textComponent, String componentData, int color) {
        var style = textComponent.getStyle();
        style = style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "@!" + componentData));
        style = style.withColor(color);
        return textComponent.copy().withStyle(style);
    }

    public static Component withHoverTextTranslate(Component textComponent, Component hover) {
        Style style = textComponent.getStyle();
        style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
        return textComponent.copy().withStyle(style);
    }

    public ComponentPanelWidget setMaxWidthLimit(int maxWidthLimit) {
        this.maxWidthLimit = maxWidthLimit;
        if (isRemote()) {
            formatDisplayText();
            updateComponentTextSize();
        }
        return this;
    }

    public ComponentPanelWidget setCenter(boolean center) {
        isCenter = center;
        if (isRemote()) {
            formatDisplayText();
            updateComponentTextSize();
        }
        return this;
    }

    public ComponentPanelWidget setSpace(int space) {
        this.space = space;
        if (isRemote()) {
            formatDisplayText();
            updateComponentTextSize();
        }
        return this;
    }

    @Override
    public void writeInitialData(RegistryFriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        buffer.writeVarInt(lastText.size());
        for (Component textComponent : lastText) {
            ComponentSerialization.STREAM_CODEC.encode(buffer, textComponent);
        }
    }

    @Override
    public void readInitialData(RegistryFriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        readUpdateInfo(1, buffer);
    }

    @Override
    public void initWidget() {
        super.initWidget();
        if (textSupplier != null) {
            lastText.clear();
            textSupplier.accept(lastText);
        }
        if (isClientSideWidget && isRemote()) {
            formatDisplayText();
            updateComponentTextSize();
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (isClientSideWidget && textSupplier != null) {
            List<Component> textBuffer = new ArrayList<>();
            textSupplier.accept(textBuffer);
            if (!lastText.equals(textBuffer)){
                this.lastText = textBuffer;
                formatDisplayText();
                updateComponentTextSize();
            }
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (textSupplier != null) {
            List<Component> textBuffer = new ArrayList<>();
            textSupplier.accept(textBuffer);
            if (!lastText.equals(textBuffer)) {
                this.lastText = textBuffer;
                writeUpdateInfo(1, buffer -> {
                    buffer.writeVarInt(lastText.size());
                    for (Component textComponent : lastText) {
                        ComponentSerialization.STREAM_CODEC.encode(buffer, textComponent);
                    }
                });
            }
        }
    }

    @Override
    public void readUpdateInfo(int id, RegistryFriendlyByteBuf buffer) {
        if (id == 1) {
            this.lastText.clear();
            int count = buffer.readVarInt();
            for (int i = 0; i < count; i++) {
                this.lastText.add(ComponentSerialization.STREAM_CODEC.decode(buffer));
            }
            formatDisplayText();
            updateComponentTextSize();
        }
    }

    @Override
    public void handleClientAction(int id, RegistryFriendlyByteBuf buffer) {
        if (id == 1) {
            ClickData clickData = ClickData.readFromBuf(buffer);
            String componentData = buffer.readUtf();
            if (clickHandler != null) {
                clickHandler.accept(componentData, clickData);
            }
        } else {
            super.handleClientAction(id, buffer);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void updateComponentTextSize() {
        var fontRenderer = Minecraft.getInstance().font;
        int totalHeight = cacheLines.size() * (fontRenderer.lineHeight + space);
        if (totalHeight > 0) {
            totalHeight -= space;
        }
        if (isCenter) {
            setSize(Size.of(maxWidthLimit, totalHeight));
        } else {
            int maxStringWidth = 0;
            for (var line : cacheLines) {
                maxStringWidth = Math.max(fontRenderer.width(line), maxStringWidth);
            }
            setSize(Size.of(maxWidthLimit == 0 ? maxStringWidth : Math.min(maxWidthLimit, maxStringWidth), totalHeight));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void formatDisplayText() {
        var fontRenderer = Minecraft.getInstance().font;
        int maxTextWidthResult = maxWidthLimit == 0 ? Integer.MAX_VALUE : maxWidthLimit;
        this.cacheLines = lastText.stream().flatMap(component ->
                ComponentRenderUtils.wrapComponents(component, maxTextWidthResult, fontRenderer).stream())
                .toList();
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    protected Style getStyleUnderMouse(double mouseX, double mouseY) {
        var fontRenderer = Minecraft.getInstance().font;
        var position = getPosition();
        var size = getSize();

        var selectedLine = (mouseY - position.y) / (fontRenderer.lineHeight + space);
        if (isCenter) {
            if (selectedLine >= 0 && selectedLine < cacheLines.size()) {
                var cacheLine = cacheLines.get((int) selectedLine);
                var lineWidth = fontRenderer.width(cacheLine);
                var offsetX = position.x + (size.width - lineWidth) / 2f;
                if (mouseX >= offsetX) {
                    var mouseOffset = (int)(mouseX - position.x);
                    return fontRenderer.getSplitter().componentStyleAtWidth(cacheLine, mouseOffset);
                }
            }
        } else {
            if (mouseX >= position.x && selectedLine >= 0 && selectedLine < cacheLines.size()) {
                var cacheLine = cacheLines.get((int) selectedLine);
                var mouseOffset = (int)(mouseX - position.x);
                return fontRenderer.getSplitter().componentStyleAtWidth(cacheLine, mouseOffset);
            }
        }
        return null;
    }
    
    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var style = getStyleUnderMouse(mouseX, mouseY);
        if (style != null) {
            if (style.getClickEvent() != null) {
                ClickEvent clickEvent = style.getClickEvent();
                String componentText = clickEvent.getValue();
                if (clickEvent.getAction() == ClickEvent.Action.OPEN_URL) {
                    if (componentText.startsWith("@!")) {
                        String rawText = componentText.substring(2);
                        ClickData clickData = new ClickData();
                        if (clickHandler != null) {
                            clickHandler.accept(rawText, clickData);
                        }
                        writeClientAction(1, buf -> {
                            clickData.writeToBuf(buf);
                            buf.writeUtf(rawText);
                        });
                    } else if (componentText.startsWith("@#")) {
                        String rawText = componentText.substring(2);
                        Util.getPlatform().openUri(rawText);
                    }
                    playButtonClickSound();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@NotNull @Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        var style = getStyleUnderMouse(mouseX, mouseY);
        if (style != null) {
            if (style.getHoverEvent() != null) {
                var hoverEvent = style.getHoverEvent();
                var hoverTips = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
                if (hoverTips != null) {
                    gui.getModularUIGui().setHoverTooltip(List.of(hoverTips), ItemStack.EMPTY, null, null);
                    return;
                }
            }
        }
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        var fontRenderer = Minecraft.getInstance().font;
        var position = getPosition();
        var size = getSize();
        for (int i = 0; i < cacheLines.size(); i++) {
            var cacheLine = cacheLines.get(i);
            if (isCenter) {
                var lineWidth = fontRenderer.width(cacheLine);
                graphics.drawString(fontRenderer, cacheLine, position.x + (size.width - lineWidth) / 2, position.y + i * (fontRenderer.lineHeight + space), -1);
            } else {
                graphics.drawString(fontRenderer, cacheLines.get(i), position.x, position.y + i * (fontRenderer.lineHeight + 2), -1);
            }
        }
    }
}
