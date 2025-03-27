package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.editor.ColorPattern;
import com.lowdragmc.lowdraglib.editor.annotation.*;
import com.lowdragmc.lowdraglib.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.math.Position;
import com.lowdragmc.lowdraglib.math.Size;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@LDLRegister(name = "selector", group = "widget.basic")
public class SelectorWidget extends WidgetGroup {
    protected List<SelectableWidgetGroup> selectables;
    @Configurable(name = "ldlib.gui.editor.name.candidates")
    protected List<String> candidates;
    @Configurable(name = "ldlib.gui.editor.name.currentValue")
    protected String currentValue;
    @Configurable(name = "ldlib.gui.editor.name.maxCount")
    @NumberRange(range = {1, 20})
    protected int maxCount = 5;
    @Configurable(name = "ldlib.gui.editor.name.color")
    @NumberColor
    protected int fontColor = -1;
    @Configurable(name = "ldlib.gui.editor.name.showUp")
    protected boolean showUp;
    protected boolean isShow;
    protected IGuiTexture popUpTexture = new ColorRectTexture(0xAA000000);
    private Supplier<List<String>> candidatesSupplier;
    private Supplier<String> supplier;
    private Consumer<String> onChanged;
    public final TextTexture textTexture;
    protected final DraggableScrollableWidgetGroup popUp;
    protected final ButtonWidget button;

    public SelectorWidget() {
        this(0, 0, 60, 15, List.of(), -1);
    }

    @Override
    public void initTemplate() {
        setCandidates(new ArrayList<>(List.of("A", "B", "C", "D", "E", "F", "G")));
        setButtonBackground(ColorPattern.T_GRAY.rectTexture());
        setValue("D");
    }

    public SelectorWidget(int x, int y, int width, int height, List<String> candidates, int fontColor) {
        super(Position.of(x, y), Size.of(width, height));
        this.button = new ButtonWidget(0,0, width, height, IGuiTexture.EMPTY, d -> {
            if (d.isRemote) setShow(!isShow);
        });
        this.candidates = candidates;
        this.selectables = new ArrayList<>();
        this.addWidget(button);
        this.addWidget(new ImageWidget(0, 1, width, height - 1, textTexture = new TextTexture("", fontColor).setWidth(width).setType(TextTexture.TextType.ROLL)));
        this.addWidget(popUp = new DraggableScrollableWidgetGroup(0, height, width, 15));
        popUp.setBackground(popUpTexture);
        popUp.setVisible(false);
        popUp.setActive(false);
        currentValue = "";
        computeLayout();
    }

    protected void computeLayout() {
        int height = Math.min(maxCount, candidates.size()) * 15;
        popUp.clearAllWidgets();
        selectables.clear();
        popUp.setSize(Size.of(getSize().width, height));
        popUp.setSelfPosition(showUp ? Position.of(0, -height) : Position.of(0, getSize().height));
        if (candidates.size() > maxCount) {
            popUp.setYScrollBarWidth(4).setYBarStyle(null, new ColorRectTexture(-1));
        }
        int y = 0;
        int width = candidates.size() > maxCount ? getSize().width -4 : getSize().width;
        for (String candidate : candidates) {
            SelectableWidgetGroup select = new SelectableWidgetGroup(0, y, width, 15);
            select.addWidget(new ImageWidget(0, 0, width, 15, new TextTexture(candidate, fontColor).setWidth(width).setType(TextTexture.TextType.ROLL)));
            select.setSelectedTexture(-1, -1);
            select.setOnSelected(s -> {
                setValue(candidate);
                if (onChanged != null) {
                    onChanged.accept(candidate);
                }
                writeClientAction(2, buffer -> buffer.writeUtf(candidate));
                setShow(false);
            });
            popUp.addWidget(select);
            selectables.add(select);
            y += 15;
        }
        popUp.setScrollYOffset(0);
    }

    @ConfigSetter(field = "maxCount")
    public SelectorWidget setMaxCount(int maxCount) {
        this.maxCount = maxCount;
        computeLayout();
        return this;
    }

    @ConfigSetter(field = "showUp")
    public SelectorWidget setIsUp(boolean isUp) {
        this.showUp = isUp;
        computeLayout();
        return this;
    }

    @ConfigSetter(field = "fontColor")
    public SelectorWidget setFontColor(int fontColor) {
        this.fontColor = fontColor;
        computeLayout();
        return this;
    }

    @ConfigSetter(field = "currentValue")
    public SelectorWidget setValue(String value) {
        if (!value.equals(currentValue)) {
            currentValue = value;
            int index = candidates.indexOf(value);
            textTexture.updateText(value);
            for (int i = 0; i < selectables.size(); i++) {
                selectables.get(i).isSelected = index == i;
            }
        }
        return this;
    }

    @ConfigSetter(field = "candidates")
    public void setCandidates(List<String> candidates) {
        this.candidates = candidates;
        computeLayout();
    }

    public SelectorWidget setButtonBackground(IGuiTexture... guiTexture) {
        super.setBackground(guiTexture);
        return this;
    }

    @ConfigSetter(field = "popUpTexture")
    public SelectorWidget setBackground(IGuiTexture background) {
        popUpTexture = background;
        popUp.setBackground(background);
        return this;
    }

    @Override
    public void setSize(Size size) {
        super.setSize(size);
        button.setSize(size);
        computeLayout();
    }

    @OnlyIn(Dist.CLIENT)
    public void setShow(boolean isShow) {
        if (isShow) {
            setFocus(true);
        }
        this.isShow = isShow;
        popUp.setVisible(isShow);
        popUp.setActive(isShow);
    }

    public String getValue() {
        return currentValue;
    }

    public SelectorWidget setCandidatesSupplier(Supplier<List<String>> candidatesSupplier) {
        this.candidatesSupplier = candidatesSupplier;
        return this;
    }

    public SelectorWidget setOnChanged(Consumer<String> onChanged) {
        this.onChanged = onChanged;
        return this;
    }

    public SelectorWidget setSupplier(Supplier<String> supplier) {
        this.supplier = supplier;
        return this;
    }

    @Override
    public boolean isMouseOverElement(double mouseX, double mouseY) {
        return super.isMouseOverElement(mouseX, mouseY) || (isShow && popUp.isMouseOverElement(mouseX, mouseY));
    }

    @Override
    public @Nullable Widget getHoverElement(double mouseX, double mouseY) {
        return isMouseOverElement(mouseX, mouseY) ? this : null;
    }

    @Override
    public void onFocusChanged(@Nullable Widget lastFocus, Widget focus) {
        if (lastFocus != null && !lastFocus.isParent(this) && focus != this) {
            setShow(false);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (isClientSideWidget) {
            if (candidatesSupplier != null) {
                var latest = candidatesSupplier.get();
                if (!latest.equals(candidates)) {
                    setCandidates(latest);
                }
            }
            if (supplier != null) {
                setValue(supplier.get());
            }
        }
        if (gui != null) {
            ModularUIGuiContainer container = gui.getModularUIGui();
            if (container != null && container.lastFocus != null && container.lastFocus.isParent(this)) {
                setFocus(true);
            }
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!isClientSideWidget) {
            if (candidatesSupplier != null) {
                var latest = candidatesSupplier.get();
                if (!latest.equals(candidates)) {
                    setCandidates(latest);
                    writeUpdateInfo(4, buffer -> {
                        buffer.writeVarInt(latest.size());
                        for (String candidate : latest) {
                            buffer.writeUtf(candidate);
                        }
                    });
                }
            }
            if (supplier != null) {
                var last = currentValue;
                setValue(supplier.get());
                if (!last.equals( currentValue)) {
                    writeUpdateInfo(3, buffer -> buffer.writeUtf(currentValue));
                }
            }
        }
    }

    @Override
    public void writeInitialData(RegistryFriendlyByteBuf buffer) {
        super.writeInitialData(buffer);
        if (supplier != null) {
            setValue(supplier.get());
        }
        buffer.writeUtf(currentValue);
    }

    @Override
    public void readInitialData(RegistryFriendlyByteBuf buffer) {
        super.readInitialData(buffer);
        setValue(buffer.readUtf());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        boolean lastVisible = popUp.isVisible();
        popUp.setVisible(false);
        super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        popUp.setVisible(lastVisible);

        if(isShow) {
            graphics.pose().translate(0, 0, 200);
            popUp.drawInBackground(graphics, mouseX, mouseY, partialTicks);
            popUp.drawInForeground(graphics, mouseX, mouseY, partialTicks);
            graphics.pose().translate(0, 0, -200);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        boolean lastVisible = popUp.isVisible();
        popUp.setVisible(false);
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        popUp.setVisible(lastVisible);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!super.mouseClicked(mouseX, mouseY, button)) {
            setFocus(false);
            return false;
        }
        return true;
    }

    @Override
    public void handleClientAction(int id, RegistryFriendlyByteBuf buffer) {
        super.handleClientAction(id, buffer);
        if (id == 2) {
            setValue(buffer.readUtf());
            if (onChanged != null) {
               onChanged.accept(getValue()); 
            }
        }
    }

    @Override
    public void readUpdateInfo(int id, RegistryFriendlyByteBuf buffer) {
        super.readUpdateInfo(id, buffer);
        if (id == 3) {
            setValue(buffer.readUtf());
        } else if (id == 4) {
            var size = buffer.readVarInt();
            List<String> latest = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                latest.add(buffer.readUtf());
            }
            setCandidates(latest);
        }
    }

    @Override
    public void addWidgetsConfigurator(ConfiguratorGroup father) {

    }

    @Override
    public boolean canWidgetAccepted(IConfigurableWidget widget) {
        return false;
    }
}
