package com.lowdragmc.lowdraglib.gui.widget.codeeditor;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;

@Getter
public class CodeEditorWidget extends WidgetGroup {
    public static final ResourceLocation MONO_BOLD = LDLib.id("jetbrains_mono_bold");

    public final CodeEditor codeEditor = new CodeEditor();
    protected int scrollXOffset;
    protected int scrollYOffset;
    protected IGuiTexture xBarB = IGuiTexture.EMPTY;
    protected IGuiTexture xBarF = ColorPattern.T_GRAY.rectTexture().setRadius(2);
    protected IGuiTexture yBarB = IGuiTexture.EMPTY;
    protected IGuiTexture yBarF = ColorPattern.T_GRAY.rectTexture().setRadius(2);
    @Setter
    protected Consumer<List<String>> onTextChanged;

    // runtime
    private boolean isHoveringXBar;
    private boolean isHoveringYBar;
    private boolean isDraggingXBar;
    private boolean isDraggingYBar;
    private double lastDeltaX, lastDeltaY;

    public CodeEditorWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        setBackground(ColorPattern.DARK_GRAY.rectTexture());
    }

    public boolean canConsumeInput() {
        return this.isVisible() && this.isFocus() && this.isActive();
    }

    public List<String> getLines() {
        return codeEditor.getLines();
    }

    public void setLines(List<String> lines) {
        this.codeEditor.setLines(lines);
    }

    public void notifyChanged() {
        if (onTextChanged != null) {
            onTextChanged.accept(codeEditor.getLines());
        }
    }

    @Nullable
    public Cursor getCursor(double mouseX, double mouseY) {
        var pos = getPosition();
        var size = getSize();
        var font = Minecraft.getInstance().font;
        var lineHeight = font.lineHeight + 2;
        var xOffset = 2;
        var yOffset = 2;


        var x = mouseX - pos.x - xOffset + scrollXOffset;
        var y = mouseY - pos.y - yOffset + scrollYOffset;

        var visibleLines = codeEditor.getVisibleStyledLines();
        var line = Mth.clamp((int) Math.floor(y / lineHeight), 0, visibleLines.size() - 1);
        var visibleLine = visibleLines.get(line);
        var width = visibleLine.getWidth(font, Style.EMPTY.withFont(MONO_BOLD));
        var column = Math.min(1, x / width) * codeEditor.getDocument().getLine(visibleLine.line()).length() + 0.5;
        return new Cursor(visibleLines.get(line).line(), (int) column);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHoveringXBar) {
            isDraggingXBar = true;
            return true;
        } else if (isHoveringYBar) {
            isDraggingYBar = true;
            return true;
        }
        if (isMouseOverElement(mouseX, mouseY)) {
            setFocus(true);
            codeEditor.clearSelection();
            codeEditor.setCursor(getCursor(mouseX, mouseY));
            codeEditor.startSelection();
            codeEditor.startSelection();
            return true;
        }
        if (isFocus()) {
            setFocus(false);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        double dx = dragX + lastDeltaX;
        double dy = dragY + lastDeltaY;
        dragX = (int) dx;
        dragY = (int) dy;
        lastDeltaX = dx- dragX;
        lastDeltaY = dy - dragY;
        if (isDraggingXBar || isDraggingYBar) {
            var size = getSize();
            var font = Minecraft.getInstance().font;
            var lineHeight = font.lineHeight + 2;
            var visibleLines = codeEditor.getVisibleStyledLines();

            var fullHeight = lineHeight * visibleLines.size();
            int fullWidth = visibleLines.stream().map(styledLine -> styledLine.getWidth(font, Style.EMPTY.withFont(MONO_BOLD))).max(Integer::compareTo).orElse(0) + 3;
            var hasXBar = fullWidth > size.width;
            var availableHeight = size.height - (hasXBar ? 4 : 0);
            var hasYBar = fullHeight > availableHeight;
            var availableWidth = size.width - (hasYBar ? 4 : 0);
            hasXBar = fullWidth > availableWidth;
            if (isDraggingXBar && hasXBar) {
                scrollXOffset += (int) dragX;
                scrollXOffset = Mth.clamp(scrollXOffset, 0, fullWidth - availableWidth);
            } else if (isDraggingYBar && hasYBar) {
                scrollYOffset += (int) dragY;
                scrollYOffset = Mth.clamp(scrollYOffset, 0, fullHeight - availableHeight);
            }
            return true;
        }

        if (isMouseOverElement(mouseX, mouseY) && codeEditor.isSelecting()) {
            codeEditor.setCursor(getCursor(mouseX, mouseY));
            codeEditor.updateSelection();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        codeEditor.endSelection();
        isDraggingXBar = false;
        isDraggingYBar = false;
        lastDeltaX = 0;
        lastDeltaY = 0;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (canConsumeInput()) {
            var previous = getLines();
            if (Screen.isSelectAll(keyCode)) {
                this.codeEditor.selectAll();
            } else if (Screen.isCopy(keyCode)) {
                Minecraft.getInstance().keyboardHandler.setClipboard(codeEditor.copySelection());
            } else if (Screen.isPaste(keyCode)) {
                this.codeEditor.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
            } else if (Screen.isCut(keyCode)) {
                Minecraft.getInstance().keyboardHandler.setClipboard(codeEditor.copySelection());
                this.codeEditor.deleteSelection();
            } else {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> codeEditor.startSelection();
                    case GLFW.GLFW_KEY_ENTER -> codeEditor.enter();
                    case GLFW.GLFW_KEY_BACKSPACE -> codeEditor.backspace();
                    case GLFW.GLFW_KEY_DELETE -> codeEditor.deleteForwardText();
                    case GLFW.GLFW_KEY_RIGHT -> {
                        codeEditor.moveCursorRight();
                        if (isShiftDown()) {
                            codeEditor.updateSelection();
                        } else {
                            codeEditor.clearSelection();
                        }
                    }
                    case GLFW.GLFW_KEY_LEFT -> {
                        codeEditor.moveCursorLeft();
                        if (isShiftDown()) {
                            codeEditor.updateSelection();
                        } else {
                            codeEditor.clearSelection();
                        }
                    }
                    case GLFW.GLFW_KEY_DOWN -> {
                        codeEditor.moveCursorDown();
                        if (isShiftDown()) {
                            codeEditor.updateSelection();
                        } else {
                            codeEditor.clearSelection();
                        }
                    }
                    case GLFW.GLFW_KEY_UP -> {
                        codeEditor.moveCursorUp();
                        if (isShiftDown()) {
                            codeEditor.updateSelection();
                        } else {
                            codeEditor.clearSelection();
                        }
                    }
                    case GLFW.GLFW_KEY_TAB -> codeEditor.insertText(codeEditor.getIndentString());
                    case GLFW.GLFW_KEY_HOME -> codeEditor.moveCursorStart();
                    case GLFW.GLFW_KEY_END -> codeEditor.moveCursorEnd();
                };
            }
            adaptCursor();
            if (!previous.equals(getLines())) {
                notifyChanged();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @OnlyIn(Dist.CLIENT)
    public void adaptCursor() {
        var pos = getPosition();
        var size = getSize();
        var font = Minecraft.getInstance().font;
        var lineHeight = font.lineHeight + 2;
        var xOffset = 2;
        var yOffset = 2;
        var cursorPos = codeEditor.getCursor();

        var visibleLines = codeEditor.getVisibleStyledLines();

        var fullHeight = lineHeight * visibleLines.size();
        int fullWidth = visibleLines.stream().map(styledLine -> styledLine.getWidth(font, Style.EMPTY.withFont(MONO_BOLD))).max(Integer::compareTo).orElse(0) + 3;
        var hasXBar = fullWidth > size.width;
        var availableHeight = size.height - (hasXBar ? 4 : 0);
        var hasYBar = fullHeight > availableHeight;
        var availableWidth = size.width - (hasYBar ? 4 : 0);
        hasXBar = fullWidth > availableWidth;

        for (StyledLine visibleLine : visibleLines) {
            if (visibleLine.line() > cursorPos.line()) break;
            if (visibleLine.line() == cursorPos.line()) {
                // found
                var cursorX = font.width(Component.literal(codeEditor.getDocument().getLine(cursorPos.line()).substring(0, cursorPos.column()))
                        .withStyle(Style.EMPTY.withFont(MONO_BOLD))) - 1  + xOffset + pos.x - scrollXOffset;
                if (cursorX < pos.x) {
                    scrollXOffset += (cursorX - pos.x);
                } else if (cursorX > pos.x + availableWidth - 2) {
                    scrollXOffset += (cursorX - pos.x - availableWidth + 2);
                }

                var cursorY = pos.y + cursorPos.line() * lineHeight + font.lineHeight + yOffset - 2 - scrollYOffset;
                if (cursorY < pos.y + font.lineHeight) {
                    scrollYOffset += (cursorY - pos.y - font.lineHeight);
                } else if (cursorY > pos.y + availableHeight) {
                    scrollYOffset += (cursorY - pos.y - availableHeight);
                }
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (canConsumeInput()) {
            if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                codeEditor.endSelection();
                return true;
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean charTyped(char codePoint, int modifiers) {
        if (canConsumeInput()) {
            if (StringUtil.isAllowedChatCharacter(codePoint)) {
                codeEditor.insertText(Character.toString(codePoint));
                adaptCursor();
                notifyChanged();
            }
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOverElement(mouseX, mouseY)) {
            var size = getSize();
            var font = Minecraft.getInstance().font;
            var lineHeight = font.lineHeight + 2;
            var visibleLines = codeEditor.getVisibleStyledLines();

            var fullHeight = lineHeight * visibleLines.size();
            int fullWidth = visibleLines.stream().map(styledLine -> styledLine.getWidth(font, Style.EMPTY.withFont(MONO_BOLD))).max(Integer::compareTo).orElse(0) + 3;
            var hasXBar = fullWidth > size.width;
            var availableHeight = size.height - (hasXBar ? 4 : 0);
            var hasYBar = fullHeight > availableHeight;

            if (hasYBar) {
                int moveDelta = (int) (-Mth.clamp(scrollY, -1, 1) * 13);
                scrollYOffset += moveDelta;
                scrollYOffset = Mth.clamp(scrollYOffset, 0, fullHeight - availableHeight);
            } else {
                scrollYOffset = 0;
            }
            return true;
        }
        return super.mouseWheelMove(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        drawBackgroundTexture(graphics, mouseX, mouseY);
        var pos = getPosition();
        var size = getSize();
        var font = Minecraft.getInstance().font;
        var lineHeight = font.lineHeight + 2;
        var xOffset = 2;
        var yOffset = 2;
        var visibleLines = codeEditor.getVisibleStyledLines();

        // scroll bar
        var fullHeight = lineHeight * visibleLines.size();
        int fullWidth = visibleLines.stream().map(styledLine -> styledLine.getWidth(font, Style.EMPTY.withFont(MONO_BOLD))).max(Integer::compareTo).orElse(0) + 3;
        var hasXBar = fullWidth > size.width;
        var availableHeight = size.height - (hasXBar ? 4 : 0);
        var hasYBar = fullHeight > availableHeight;
        var availableWidth = size.width - (hasYBar ? 4 : 0);
        hasXBar = fullWidth > availableWidth;

        if (hasYBar) {
            var barHeight = (availableHeight * availableHeight / fullHeight);
            isHoveringYBar = isMouseOver(pos.x + size.width - 4,
                    pos.y + scrollYOffset * availableHeight / fullHeight, 4, barHeight, mouseX, mouseY);
            scrollYOffset = Mth.clamp(scrollYOffset, 0, fullHeight - availableHeight);
            yBarB.draw(graphics, mouseX, mouseY, pos.x + size.width - 4, pos.y, 4, availableHeight, partialTicks);
            yBarF.draw(graphics, mouseX, mouseY, pos.x + size.width - 4,
                    pos.y + scrollYOffset * availableHeight * 1f / fullHeight, 4, barHeight, partialTicks);
        } else {
            scrollYOffset = 0;
            isHoveringYBar = false;
        }

        if (hasXBar) {
            var barWidth = (size.width * availableWidth / fullWidth);
            isHoveringXBar = isMouseOver(pos.x + scrollXOffset * size.width / fullWidth,
                    pos.y + size.height - 4, barWidth, 4, mouseX, mouseY);
            scrollXOffset = Mth.clamp(scrollXOffset, 0, fullWidth - availableWidth);
            xBarB.draw(graphics, mouseX, mouseY, pos.x, pos.y + size.height - 4, size.width, 4, partialTicks);
            xBarF.draw(graphics, mouseX, mouseY, pos.x + scrollXOffset * size.width * 1f / fullWidth,
                    pos.y + size.height - 4, barWidth, 4, partialTicks);
        } else {
            scrollXOffset = 0;
            isHoveringXBar = false;
        }

        // scissor
        var trans = graphics.pose().last().pose();
        var realPos = trans.transform(new Vector4f(pos.x, pos.y, 0, 1));
        var realPos2 = trans.transform(new Vector4f(pos.x + availableWidth, pos.y + availableHeight, 0, 1));
        graphics.enableScissor((int) realPos.x, (int) realPos.y, (int) realPos2.x, (int) realPos2.y);
        graphics.pose().pushPose();
        graphics.pose().translate(-scrollXOffset, -scrollYOffset, 0);

        // draw selection
        if (codeEditor.isSelectionValid()) {
            var range = codeEditor.getSelection().getSelectionRange();
            graphics.drawManaged(() -> {
                for (int i = 0; i < visibleLines.size(); i++) {
                    var visibleLine = visibleLines.get(i);
                    var line = visibleLine.line();
                    if (line < range[0]) {
                        continue;
                    }
                    if (line > range[2]) {
                        break;
                    }
                    var start = line == range[0] ? font.width(Component.literal(codeEditor.getDocument().getLine(line).substring(0, range[1]))
                            .withStyle(Style.EMPTY.withFont(MONO_BOLD))) - 1 : 0;
                    var end = line == range[2] ? font.width(Component.literal(codeEditor.getDocument().getLine(line).substring(0, range[3]))
                            .withStyle(Style.EMPTY.withFont(MONO_BOLD))) - 1 : getSizeWidth();
                    graphics.fill(pos.x + start + xOffset,
                            pos.y + i * lineHeight + yOffset - 2,
                            pos.x + end + xOffset,
                            pos.y + i * lineHeight + font.lineHeight + yOffset - 2, 0x80FFFFFF);
                }
            });
        }
        // draw text
        graphics.drawManaged(() -> {
            var y = 0;
            for (var styledLine : visibleLines) {
                var x = 0;
                for (var styledText : styledLine.text()) {
                    var literal = Component.literal(styledText.getText()).withStyle(styledText.getStyle().withFont(MONO_BOLD));
                    graphics.drawString(font, literal, pos.x + x + xOffset - 1, pos.y + y + yOffset, -1, false);
                    x += font.width(literal) - 1;
                }
                y += lineHeight;
            }
        });
        // draw cursor
        if (canConsumeInput() && System.currentTimeMillis() % 1000 < 500) {
            var cursorPos = codeEditor.getCursor();
            for (StyledLine visibleLine : visibleLines) {
                if (visibleLine.line() > cursorPos.line()) break;
                if (visibleLine.line() == cursorPos.line()) {
                    // found
                    var cursorX = font.width(Component.literal(codeEditor.getDocument().getLine(cursorPos.line()).substring(0, cursorPos.column()))
                            .withStyle(Style.EMPTY.withFont(MONO_BOLD))) - 1;
                    graphics.fill(pos.x + cursorX + xOffset,
                            pos.y + cursorPos.line() * lineHeight + yOffset - 2,
                            pos.x + cursorX + 1 + xOffset,
                            pos.y + cursorPos.line() * lineHeight + font.lineHeight + yOffset - 2,
                            -1);
                }
            }
        }
        graphics.pose().popPose();
        graphics.disableScissor();

        // draw widgets
        drawWidgetsBackground(graphics, mouseX, mouseY, partialTicks);

    }
}
