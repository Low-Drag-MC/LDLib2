package com.lowdragmc.lowdraglib.gui.widget;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.util.TreeNode;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.joml.Vector4f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Accessors(chain = true)
public class TreeListWidget<K, T> extends Widget {
    protected int scrollOffset;
    protected List<TreeNode<K, T>> list;
    protected TreeNode<K, T> selected;
    @Setter @Nullable
    protected IGuiTexture nodeTexture;
    @Setter @Nullable
    protected IGuiTexture leafTexture;
    @Setter @Nullable
    protected Consumer<TreeNode<K, T>> onSelected;
    @Setter @Nullable
    protected Consumer<TreeNode<K, T>> onDoubleClickLeaf;
    @Setter @Nullable
    protected Function<K, IGuiTexture> keyIconSupplier;
    @Setter @Nullable
    protected Function<K, String> keyNameSupplier;
    @Setter @Nullable
    protected Function<T, IGuiTexture> contentIconSupplier;
    @Setter @Nullable
    protected Function<T, String> contentNameSupplier;
    @Setter
    protected int lineHeight = 11;
    protected boolean canSelectNode;
    // runtime
    protected long lastClickTime;

    public TreeListWidget(int xPosition, int yPosition, int width, int height, TreeNode<K, T> root, Consumer<TreeNode<K, T>> onSelected) {
        super(xPosition, yPosition, width, height);
        list = new ArrayList<>();
        if (root.getChildren() != null) {
            list.addAll(root.getChildren());
        }
        this.onSelected = onSelected;
    }

    public TreeListWidget<K, T> canSelectNode(boolean canSelectNode) {
        this.canSelectNode = canSelectNode;
        return this;
    }

    public TreeListWidget<K, T> setBackground(IGuiTexture background) {
        super.setBackground(background);
        return this;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void updateScreen() {
        super.updateScreen();
        if (nodeTexture != null) nodeTexture.updateTick();
        if (leafTexture != null) leafTexture.updateTick();
    }


    @Override
    @Environment(EnvType.CLIENT)
    public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
        if (this.isMouseOverElement(mouseX, mouseY)) {
            int moveDelta = (int) (-Mth.clamp(wheelDelta, -1, 1) * 5);
            this.scrollOffset = Mth.clamp(scrollOffset + moveDelta, 0, Math.max(list.size() * lineHeight - getSize().height, 0));
            return true;
        }
        return false;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void drawInBackground(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        int x = getPosition().x;
        int y = getPosition().y;
        int width = getSize().width;
        int height = getSize().height;
        var trans = graphics.pose().last().pose();
        var realPos = trans.transform(new Vector4f(x, y, 0, 1));
        var realPos2 = trans.transform(new Vector4f(x + width, y + height, 0, 1));
        graphics.enableScissor((int) realPos.x, (int) realPos.y, (int) realPos2.x, (int) realPos2.y);
        int minToRender = scrollOffset / lineHeight;
        int maxToRender = Math.min(list.size(), height / lineHeight + 2 + minToRender);
        for (int i = minToRender; i < maxToRender; i++) {
            RenderSystem.setShaderColor(1,1,1,1);
            TreeNode<K, T> node = list.get(i);
            int sX = x + lineHeight * node.dimension + 3;
            int sY = y - scrollOffset + i * lineHeight;
            String name = node.toString();
            if (node.isLeaf()) {
                if (leafTexture != null) {
                    leafTexture.draw(graphics, mouseX, mouseY, x, sY, width, lineHeight);
                }
                if (node.getContent() != null) {
                    String nameS = contentNameSupplier == null ? null : contentNameSupplier.apply(node.getContent());
                    name = nameS == null ? name : nameS;
                    IGuiTexture icon = contentIconSupplier == null ? null : contentIconSupplier.apply(node.getContent());
                    if (icon != null) {
                        icon.draw(graphics, mouseX, mouseY, sX - 2 - lineHeight, sY, lineHeight, lineHeight);
                    }
                }
            } else {
                if (nodeTexture != null) {
                    nodeTexture.draw(graphics, mouseX, mouseY, x, sY, width, lineHeight);
                }
                String nameS = keyNameSupplier == null ? null : keyNameSupplier.apply(node.getKey());
                name = nameS == null ? name : nameS;
                IGuiTexture icon = keyIconSupplier == null ? null : keyIconSupplier.apply(node.getKey());
                if (icon != null) {
                    icon.draw(graphics, mouseX, mouseY, sX - 2 - lineHeight, sY, lineHeight, lineHeight);
                }
            }
            if (node == selected) {
                DrawerHelper.drawSolidRect(graphics, x + 1, sY, width - 2, lineHeight, 0x7f000000);
            }
            int maxWidth = Math.max(width - lineHeight * node.dimension, lineHeight - 3);
            new TextTexture(name).setType(TextTexture.TextType.LEFT_HIDE).setWidth(maxWidth).draw(graphics, mouseX, mouseY, sX, sY, maxWidth, lineHeight);
        }
        graphics.disableScissor();

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1,1,1,1);
    }

    public TreeNode<K, T> jumpTo(List<K> path) {
        list.removeIf(node->node.dimension != 1);
        this.selected = null;
        int dim = 1;
        int index = 0;
        boolean flag = false;
        TreeNode<K, T> node = null;
        for (K key : path) {
            flag = false;
            for (int i = index; i < list.size(); i++) {
                node = list.get(i);
                if (node.dimension != dim) {
                    return null;
                } else if (node.getKey().equals(key)) { //expand
                    if(!node.isLeaf() && path.size() > dim) {
                        for (int j = 0; j < node.getChildren().size(); j++) {
                            list.add(index + 1 + j, node.getChildren().get(j));
                        }
                    }
                    index++;
                    dim++;
                    flag = true;
                    break;
                } else {
                    index++;
                }
            }
            if (!flag) return null;
        }
        if (flag) {
            this.selected = node;
            this.scrollOffset = Mth.clamp(lineHeight * (index - 1), 0, Math.max(list.size() * lineHeight - getSize().height, 0));
            return this.selected;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOverElement(mouseX, mouseY)) {
            int index = (int) (((mouseY - getPosition().y) + scrollOffset) / lineHeight);
            if (index < list.size()) {
                TreeNode<K, T> node = list.get(index);
                if (node.isLeaf()) {
                    if (node != this.selected) {
                        this.selected = node;
                        if (onSelected != null){
                            onSelected.accept(node);
                        }
                        lastClickTime = System.currentTimeMillis();
                    } else if (onDoubleClickLeaf != null && System.currentTimeMillis() - lastClickTime < 500) {
                        onDoubleClickLeaf.accept(node);
                    } else {
                        lastClickTime = System.currentTimeMillis();
                    }
                } else {
                    if (canSelectNode && this.selected != node) {
                        this.selected = node;
                        if (onSelected != null){
                            onSelected.accept(node);
                        }
                    } else if (node.getChildren().size() > 0 && list.contains(node.getChildren().get(0))){
                        removeNode(node);
                    } else {
                        for (int i = 0; i < node.getChildren().size(); i++) {
                            list.add(index + 1 + i, node.getChildren().get(i));
                        }
                    }
                }
                playButtonClickSound();
            }
            return true;
        }
        return false;
    }

    private void removeNode(TreeNode<?, T> node) {
        if(node.isLeaf()) return;
        for (TreeNode<?, T> child : node.getChildren()) {
            list.remove(child);
            removeNode(child);
        }
    }
}
