package com.lowdragmc.lowdraglib2.gui.texture;

import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigColor;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigSetter;
import com.lowdragmc.lowdraglib2.configurator.annotation.Configurable;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.integration.kjs.KJSBindings;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

@KJSBindings
@LDLRegisterClient(name = "rect_texture", registry = "ldlib2:gui_texture")
@Accessors(chain = true)
public class RectTexture extends TransformTexture {
    @Getter
    @Configurable
    @ConfigNumber(range = {0f, Float.MAX_VALUE}, wheel = 1)
    private Vector4f radius = new Vector4f(0, 0, 0, 0);
    @Getter
    @Configurable
    @ConfigNumber(range = {0f, Float.MAX_VALUE}, wheel = 1)
    private float stroke = 0;
    @Getter
    @Configurable
    @ConfigColor
    private int color = 0xFFFFFFFF;
    @Getter
    @Configurable
    @ConfigColor
    private int borderColor = 0xff000000;
    @Getter
    @Configurable
    @ConfigNumber(range = {4, 32}, wheel = 1)
    private int cornerSegments = 8;
    
    private List<Vector2f>[] cachedCornerArcs = null;
    private boolean cachedSegments = false;

    public static RectTexture of(int color) {
        return new RectTexture().setColor(color);
    }
    
    @ConfigSetter(field = "radius")
    public RectTexture setRadius(Vector4f radius) {
        this.radius = radius;
//        this.cachedSegments = false;
        return this;
    }

    @ConfigSetter(field = "stroke")
    public RectTexture setStroke(float stroke) {
        this.stroke = stroke;
//        this.cachedSegments = false;
        return this;
    }

    @Override
    @ConfigSetter(field = "color")
    public RectTexture setColor(int color) {
        this.color = color;
//        this.colorVec4 = ColorUtils.toVector4f(color);
        return this;
    }

    @ConfigSetter(field = "borderColor")
    public RectTexture setBorderColor(int borderColor) {
        this.borderColor = borderColor;
//        this.borderColorVec4 = ColorUtils.toVector4f(borderColor);
        return this;
    }

    @ConfigSetter(field = "cornerSegments")
    public RectTexture setCornerSegments(int cornerSegments) {
        this.cornerSegments = cornerSegments;
        this.cachedSegments = false;
        return this;
    }

    @Override
    public RectTexture copy() {
        var copied = new RectTexture();
        copied.setRadius(new Vector4f(radius));
        copied.setStroke(stroke);
        copied.setColor(color);
        copied.setBorderColor(borderColor);
        copied.setCornerSegments(cornerSegments);
        copied.cachedSegments = cachedSegments;
        copied.copyTransform(this);
        return copied;
    }

    @Override
    public IGuiTexture interpolate(IGuiTexture other, float lerp) {
        if (other.getRawTexture() instanceof RectTexture rect) {
            var blended = new RectTexture();
            blended.setRadius(new Vector4f(radius).lerp(rect.getRadius(), lerp));
            blended.setStroke((1 - lerp) * stroke + rect.stroke * lerp);
            blended.setColor(ColorUtils.blendOklabColor(color, rect.color, lerp));
            blended.setBorderColor(ColorUtils.blendOklabColor(borderColor, rect.borderColor, lerp));
            blended.setCornerSegments(cornerSegments);
            blended.copyTransform(Transform2D.interpolate(getTransform2D(), rect.getTransform2D(), lerp));
            return blended;
        }
        return super.interpolate(other, lerp);
    }
    
    @SuppressWarnings("unchecked")
    private void ensureCornerCache() {
        if (cachedCornerArcs != null && cachedSegments) {
            return;
        }
        
        cachedSegments = true;
        cachedCornerArcs = new List[4];
        
        double[][] angleRanges = {
            {Math.PI, Math.PI * 1.5},       // 左上
            {Math.PI * 1.5, Math.PI * 2},   // 右上
            {0, Math.PI * 0.5},             // 右下
            {Math.PI * 0.5, Math.PI}        // 左下
        };
        
        for (int corner = 0; corner < 4; corner++) {
            cachedCornerArcs[corner] = new ArrayList<>(cornerSegments + 1);
            double startAngle = angleRanges[corner][0];
            double endAngle = angleRanges[corner][1];
            double step = (endAngle - startAngle) / cornerSegments;
            
            for (int i = 0; i <= cornerSegments; i++) {
                double angle = startAngle + i * step;
                cachedCornerArcs[corner].add(new Vector2f((float) Math.cos(angle), (float) Math.sin(angle)));
            }
        }
    }

    List<Vector2f>[] cornerArcs() {
        ensureCornerCache();
        return cachedCornerArcs;
    }
}
