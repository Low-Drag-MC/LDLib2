package com.lowdragmc.lowdraglib2.gui.ui.style.properties;

import com.lowdragmc.lowdraglib2.configurator.ui.ColorConfigurator;
import com.lowdragmc.lowdraglib2.configurator.ui.Configurator;
import com.lowdragmc.lowdraglib2.gui.ui.style.Property;
import com.lowdragmc.lowdraglib2.gui.ui.style.values.ColorValue;
import com.mojang.serialization.Codec;
import lombok.experimental.Accessors;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Accessors(chain = true)
public class ColorProperty extends Property<Integer> {
    public ColorProperty(String name, int initialValue) {
        super(name, Integer.class, Codec.INT, initialValue, ColorValue::new);
        setAllowTransition(true);
        setInterpolator(this::interpolate);
    }

    @Override
    public Configurator createConfiguratorInternal(String name, Supplier<Integer> getter, Consumer<Integer> setter) {
        return new ColorConfigurator(name, getter, setter, initialValue, true);
    }

    private int interpolate(int from, int to, float interpolation) {
        // Get RGB components
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int fa = (from >> 24) & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int ta = (to >> 24) & 0xFF;

        // Convert to linear sRGB
        double[] fromLinear = {fr / 255.0, fg / 255.0, fb / 255.0};
        double[] toLinear = {tr / 255.0, tg / 255.0, tb / 255.0};

        // Convert to OKLAB
        double[] fromOklab = rgbToOklab(fromLinear);
        double[] toOklab = rgbToOklab(toLinear);

        // Interpolate in OKLAB
        double[] result = new double[3];
        for (int i = 0; i < 3; i++) {
            result[i] = fromOklab[i] + (toOklab[i] - fromOklab[i]) * interpolation;
        }

        // Convert back to RGB
        double[] rgb = oklabToRGB(result);

        // Convert to 8-bit color components
        int r = (int) Math.round(rgb[0] * 255);
        int g = (int) Math.round(rgb[1] * 255);
        int b = (int) Math.round(rgb[2] * 255);
        int a = (int) (fa + (ta - fa) * interpolation);

        // Clamp values
        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));
        a = Math.min(255, Math.max(0, a));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private double[] rgbToOklab(double[] rgb) {
        // Convert to linear sRGB to OKLAB
        double l = 0.4122214708 * rgb[0] + 0.5363325363 * rgb[1] + 0.0514459929 * rgb[2];
        double m = 0.2119034982 * rgb[0] + 0.6806995451 * rgb[1] + 0.1073969566 * rgb[2];
        double s = 0.0883024619 * rgb[0] + 0.2817188376 * rgb[1] + 0.6299787005 * rgb[2];

        l = Math.cbrt(l);
        m = Math.cbrt(m);
        s = Math.cbrt(s);

        return new double[]{
                0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
                1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
                0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        };
    }

    private double[] oklabToRGB(double[] oklab) {
        double l = oklab[0] + 0.3963377774 * oklab[1] + 0.2158037573 * oklab[2];
        double m = oklab[0] - 0.1055613458 * oklab[1] - 0.0638541728 * oklab[2];
        double s = oklab[0] - 0.0894841775 * oklab[1] - 1.2914855480 * oklab[2];

        l = l * l * l;
        m = m * m * m;
        s = s * s * s;

        return new double[]{
                +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
                -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
                -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
        };
    }
}
