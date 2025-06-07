package com.lowdragmc.lowdraglib2.gui.widget;

import com.lowdragmc.lowdraglib2.gui.ColorPattern;
import com.lowdragmc.lowdraglib2.editor_outdated.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.util.TreeNode;
import com.lowdragmc.lowdraglib2.utils.LocalizationUtils;
import com.lowdragmc.lowdraglib2.math.Position;
import com.lowdragmc.lowdraglib2.math.Size;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Accessors(chain = true)
public class MenuWidget<K, T> extends WidgetGroup {

    public static IGuiTexture NODE_TEXTURE = new IGuiTexture() {
        @Override
        @OnlyIn(Dist.CLIENT)
        public void draw(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
            ColorPattern.BLACK.rectTexture().draw(graphics, mouseX, mouseY, x, y, width, height, partialTicks);
            Icons.RIGHT.draw(graphics, mouseX, mouseY, x + width - height + 3, y + 3, height - 6, height - 6, partialTicks);
        }
    };
    public static IGuiTexture LEAF_TEXTURE = ColorPattern.BLACK.rectTexture();
    public static IGuiTexture NODE_HOVER_TEXTURE = ColorPattern.T_GRAY.rectTexture();
    public static IGuiTexture BACKGROUND = new GuiTextureGroup(new ColorRectTexture(0xff3C4146), ColorPattern.GRAY.borderTexture(1));

    protected final TreeNode<K, T> root;
    protected final int nodeHeight;

    @Setter @Nullable
    protected IGuiTexture nodeTexture;
    @Setter @Nullable
    protected IGuiTexture leafTexture;
    @Setter @Nullable
    protected IGuiTexture nodeHoverTexture;
    @Setter @Nullable
    protected Consumer<TreeNode<K, T>> onNodeClicked;
    @Setter @Nullable
    protected Function<K, IGuiTexture> keyIconSupplier;
    @Setter @Nullable
    protected Function<K, String> keyNameSupplier;
    @Setter @Nullable
    protected Predicate<K> crossLinePredicate;
    @Setter
    protected boolean autoClose;

    protected Map<TreeNode<K, T>, WidgetGroup> children;
    protected MenuWidget<K, T> opened;

    public MenuWidget(int xPosition, int yPosition, int nodeHeight, TreeNode<K, T> root) {
        super(xPosition, yPosition, 100, nodeHeight);
        this.root = root;
        this.autoClose = true;
        this.nodeHeight = nodeHeight;
        this.children = new LinkedHashMap<>();
    }

    public void close(){
        if (this.parent != null) {
            this.parent.waitToRemoved(this);
        }
    }

    @Override
    public void initWidget() {
        int maxWidth = getSize().width;
        int maxHeight = 1;

        if (!root.isLeaf()) {
            if (isRemote()) {
                for (TreeNode<K, T> child : root.getChildren()) {
                    var key = child.getKey();
                    var name = key.toString();
                    if (keyNameSupplier != null) {
                        name = keyNameSupplier.apply(key);
                    }
                    maxWidth = Math.max(Minecraft.getInstance().font.width(LocalizationUtils.format(name)) + 4 + 2 * nodeHeight, maxWidth);
                }
            }
            for (TreeNode<K, T> child : root.getChildren()) {
                var key = child.getKey();
                if (crossLinePredicate != null && crossLinePredicate.test(key)) { // cross line
                    maxHeight += 1;
                    continue;
                }
                var name = key.toString();
                if (keyNameSupplier != null) {
                    name = keyNameSupplier.apply(key);
                }
                var group = new WidgetGroup(0, maxHeight, maxWidth, nodeHeight);
                children.put(child, group);
                if (child.isLeaf()) {
                    group.setBackground(Objects.requireNonNullElseGet(leafTexture, () -> LEAF_TEXTURE));
                    group.addWidget(new ButtonWidget(0, 0, maxWidth, nodeHeight, null, cd -> {
                        if (onNodeClicked != null) {
                            onNodeClicked.accept(child);
                        }
                        if (autoClose) {
                            WidgetGroup p = this;
                            while (p != null) {
                                if (p.parent != null && !(p.parent instanceof MenuWidget<?,?>)) {
                                    p.parent.waitToRemoved(p);
                                    return;
                                }
                                p = p.parent;
                            }
                        }
                    }).setHoverTexture(Objects.requireNonNullElseGet(nodeHoverTexture, () -> NODE_HOVER_TEXTURE)));

                } else {
                    group.setBackground(Objects.requireNonNullElseGet(nodeTexture, () -> NODE_TEXTURE));
                    group.addWidget(new ButtonWidget(0, 0, maxWidth, nodeHeight, null).setHoverTexture(Objects.requireNonNullElseGet(nodeHoverTexture, () -> NODE_HOVER_TEXTURE)));
                }
                if (keyIconSupplier != null) {
                    group.addWidget(new ImageWidget(2, 1, nodeHeight - 2, nodeHeight - 2, keyIconSupplier.apply(child.getKey())));
                }
                group.addWidget(new ImageWidget(nodeHeight + 2, 0, maxWidth - 2 * nodeHeight - 4, nodeHeight, new TextTexture(name).setType(TextTexture.TextType.LEFT)));
                addWidget(group);
                maxHeight += nodeHeight;
            }
        }
        Position pos = getPosition();
        setSize(Size.of(maxWidth, maxHeight));
        // check width
        int rightSpace = getGui().getScreenWidth() - pos.getX();
        int bottomSpace = getGui().getScreenHeight() - pos.getY();
        if (rightSpace < maxWidth) { // move to Left
            if (parent instanceof MenuWidget<?,?> menuWidget) {
                addSelfPosition(-menuWidget.getSize().width - maxWidth, 0);
            }
            rightSpace = getGui().getScreenWidth() - getPosition().getX();
            if (rightSpace < maxWidth) {
                addSelfPosition(-(maxWidth - rightSpace), 0);
            }
            int leftSpace = getPosition().getX();
            if (leftSpace < 0) {
                addSelfPosition(-leftSpace, 0);
            }
        }
        // check height
        if (bottomSpace < maxHeight) {
            if (parent instanceof MenuWidget) {
                addSelfPosition(0, nodeHeight - maxHeight);
            }
            bottomSpace = getGui().getScreenHeight() - getPosition().getY();
            if (bottomSpace < maxHeight) {
                addSelfPosition(0, -(maxHeight - bottomSpace));
            }
            int topSpace = getPosition().getY();
            if (topSpace < 0) {
                addSelfPosition(0, -topSpace);
            }
        }
        super.initWidget();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!super.mouseClicked(mouseX, mouseY, button)) {
            if (autoClose && !(parent instanceof MenuWidget)) {
                close();
            }
            return false;
        }
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseMoved(double mouseX, double mouseY) {
        if (super.mouseMoved(mouseX, mouseY)) {
            return true;
        }
        if (root.getChildren() != null) {
            int maxHeight = 0;
            for (var node : root.getChildren()) {
                if (crossLinePredicate != null && crossLinePredicate.test(node.getKey())) { // cross line
                    maxHeight += 1;
                    continue;
                }
                var widget = children.get(node);
                if (widget.isMouseOverElement(mouseX, mouseY)) {
                    // if opened
                    if (opened != null && opened.root == node) return true;

                    // close previous
                    if (opened != null) {
                        removeWidget(opened);
                        opened = null;
                    }

                    // open a new menu
                    if (!node.isLeaf()) {
                        opened = new MenuWidget<>(getSize().width, maxHeight, nodeHeight, node)
                                .setNodeHoverTexture(nodeHoverTexture)
                                .setNodeTexture(nodeTexture)
                                .setLeafTexture(leafTexture)
                                .setOnNodeClicked(onNodeClicked)
                                .setKeyIconSupplier(keyIconSupplier)
                                .setKeyNameSupplier(keyNameSupplier)
                                .setCrossLinePredicate(crossLinePredicate);
                        addWidget(opened.setBackground(backgroundTexture));
                    }
                    return true;
                }
                maxHeight += nodeHeight;
            }
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double scrollX, double scrollY) {
        var pos = getPosition();
        var size = getSize();
        var screenHeight = getGui().getScreenHeight();
        if (screenHeight < size.height && isMouseOverElement(mouseX, mouseY)) {
            var offsetY = Mth.clamp(pos.getY() + (scrollY > 0 ? -nodeHeight : nodeHeight), 0, screenHeight - size.height);
            addSelfPosition(0, offsetY - pos.getY());
            return true;
        }
        return super.mouseWheelMove(mouseX, mouseY, scrollX, scrollY);
    }
}
