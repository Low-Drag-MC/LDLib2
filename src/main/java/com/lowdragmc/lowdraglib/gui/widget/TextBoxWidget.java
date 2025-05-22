package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.editor_outdated.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Size;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Configurable(name = "widget.text_box", collapse = false)
@LDLRegister(name = "text_box", group = "widget.basic", registry = "ldlib:widget")
public class TextBoxWidget extends Widget implements IConfigurableWidget {

    // config
    @Configurable(name = "ldlib.gui.editor.name.content")
    public final List<String> content = new ArrayList<>();

    @Configurable(name = "ldlib.gui.editor.name.space")
    public int space = 1;

    @Configurable(name = "ldlib.gui.editor.name.fontSize")
    public int fontSize = 9;

    @Configurable(name = "ldlib.gui.editor.name.color")
    @ConfigColor
    public int fontColor = 0xff000000;

    @Configurable(name = "ldlib.gui.editor.name.isShadow")
    public boolean isShadow = false;

    @Configurable(name = "ldlib.gui.editor.name.isCenter")
    public boolean isCenter = false;

    private transient List<String> textLines;

    public TextBoxWidget() {
        this(0, 0, 60, List.of());
    }

    @Override
    public void initTemplate() {
        setContent(List.of("ldlib.author", "Lorem ipsum"));
        setFontColor(-1);
    }

    public TextBoxWidget(int x, int y, int width, List<String> content) {
        super(x, y, width, 0);
        this.content.addAll(content);
        this.calculate();
    }

    @Override
    @ConfigSetter(field = "size")
    public void setSize(Size size) {
        int lastWidth = getSize().width;
        super.setSize(size);
        if (size.width != lastWidth) {
            calculate();
        }
    }

    @ConfigSetter(field = "content")
    public TextBoxWidget setContent(List<String> content) {
        if (this.content != content) {
            this.content.clear();
            this.content.addAll(content);
        }
        this.calculate();
        return this;
    }

    @ConfigSetter(field = "space")
    public TextBoxWidget setSpace(int space) {
        this.space = space;
        this.calculate();
        return this;
    }

    @ConfigSetter(field = "fontSize")
    public TextBoxWidget setFontSize(int fontSize) {
        this.fontSize = fontSize;
        this.calculate();
        return this;
    }

    public TextBoxWidget setFontColor(int fontColor) {
        this.fontColor = fontColor;
        return this;
    }

    public TextBoxWidget setShadow(boolean shadow) {
        isShadow = shadow;
        return this;
    }

    public TextBoxWidget setCenter(boolean center) {
        isCenter = center;
        return this;
    }

    public int getMaxContentWidth() {
        return content.stream().mapToInt(Minecraft.getInstance().font::width).max().orElse(0);
    }

    protected void calculate() {
        if (isRemote()) {
            this.textLines = new ArrayList<>();
            Font font = Minecraft.getInstance().font;
            this.space = Math.max(space, 0);
            this.fontSize = Math.max(fontSize, 1);
            int wrapWidth = getSize().width * font.lineHeight / fontSize;
            for (String textLine : content) {
                this.textLines.addAll(font.getSplitter()
                        .splitLines(LocalizationUtils.format(textLine), wrapWidth, Style.EMPTY)
                        .stream().map(FormattedText::getString).toList());
            }
            this.setSize(Size.of(this.getSize().width, this.textLines.size() * (fontSize + space)));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        if (!textLines.isEmpty()) {
            Position position = getPosition();
            Size size = getSize();
            Font font = Minecraft.getInstance().font;
            float scale = fontSize * 1.0f / font.lineHeight;
            graphics.pose().pushPose();
            graphics.pose().scale(scale, scale, 1);
            graphics.pose().translate(position.x / scale, position.y / scale, 0);
            float x = 0;
            float y = 0;
            float ySpace = font.lineHeight + space / scale;
            for (String textLine : textLines) {
                if (isCenter) {
                    x = (size.width / scale - font.width(textLine)) / 2;
                }
                graphics.drawString(font, textLine, (int) x, (int) y, fontColor, isShadow);
                y += ySpace;
            }
            graphics.pose().popPose();
        }
        drawOverlay(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean handleDragging(Object dragging) {
        if (dragging instanceof String string) {
            List<String> list = new ArrayList<>();
            list.add(string);
            setContent(list);
            return true;
        } else return IConfigurableWidget.super.handleDragging(dragging);
    }
}