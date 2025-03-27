package com.lowdragmc.lowdraglib.compass.component;

import com.lowdragmc.lowdraglib.compass.ILayoutComponent;
import com.lowdragmc.lowdraglib.compass.LayoutPageWidget;
import com.lowdragmc.lowdraglib.gui.texture.*;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.utils.XmlUtils;
import lombok.NoArgsConstructor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.w3c.dom.Element;

import java.util.Arrays;

/**
 * @author KilaBash
 * @date 2022/9/4
 * @implNote TextureComponent
 */
@NoArgsConstructor
public class ImageComponent extends TextBoxComponent {

    protected int width = 50;
    protected int height = 50;
    protected float u0 = 0, v0 = 0;
    protected float u1 = 1, v1 = 1;
    protected IGuiTexture guiTexture = new ColorBorderTexture(-1, -1);

    @Override
    public ILayoutComponent fromXml(Element element) {
        this.width = XmlUtils.getAsInt(element, "width", width);
        this.height = XmlUtils.getAsInt(element, "height", height);
        this.u0 = XmlUtils.getAsFloat(element, "u0", u0);
        this.v0 = XmlUtils.getAsFloat(element, "v0", v0);
        this.u1 = XmlUtils.getAsFloat(element, "u1", u1);
        this.v1 = XmlUtils.getAsFloat(element, "v1", v1);
        String type = XmlUtils.getAsString(element, "type", "resource");
        String url = XmlUtils.getAsString(element, "url", "");

        guiTexture = switch (type) {
            case "resource" -> new ResourceTexture(url).getSubTexture(u0, v0, u1, v1);
            case "item" -> {
                var item = XmlUtils.getIngredient(element);
                var items = Arrays.stream(item.ingredient().getItems()).map(i -> {
                    var copied = i.copy();
                    copied.setCount(item.count());
                    return copied;
                }).toArray(ItemStack[]::new);
                yield new ItemStackTexture(items);
            }
            case "shader" -> ShaderTexture.createShader(ResourceLocation.parse(url));
            default -> IGuiTexture.EMPTY;
        };

        isCenter = true;
        return super.fromXml(element);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public LayoutPageWidget addWidgets(LayoutPageWidget currentPage) {
        var imageWidget = new ImageWidget(0, 0, width, height, guiTexture);
        if (this.hoverInfo != null) {
            imageWidget.setHoverTooltips(hoverInfo);
        }
        currentPage.addStreamWidget(wrapper(imageWidget));
        currentPage.addOffsetSpace(3);
        return super.addWidgets(currentPage);
    }
}
