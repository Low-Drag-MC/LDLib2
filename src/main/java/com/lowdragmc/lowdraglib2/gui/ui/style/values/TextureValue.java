package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;
import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class TextureValue extends StyleValue<IGuiTexture> {
    private static final LoadingCache<String, IGuiTexture> CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .removalListener((RemovalNotification<String, IGuiTexture> notification) -> {
                var value = notification.getValue();
                if (value instanceof AutoCloseable closeable) {
                    try {
                        closeable.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            })
            .build(new CacheLoader<>() {
                @Override
                @Nonnull
                public IGuiTexture load(@Nonnull String key) {
                    try {
                        return Optional.ofNullable(parseTexture(key)).orElse(IGuiTexture.MISSING_TEXTURE);
                    } catch (Throwable e) {
                        return IGuiTexture.MISSING_TEXTURE;
                    }
                }
            });

    public TextureValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable IGuiTexture doCompute(String rawValue) {
        var res = CACHE.getUnchecked(rawValue);
        if (res == IGuiTexture.MISSING_TEXTURE) return null;
        return res;
    }

    @Nullable
    public static IGuiTexture parseTexture(String rawValue) {
        if (rawValue.isBlank() || rawValue.equalsIgnoreCase("empty")) {
            return IGuiTexture.EMPTY;
        }

        // Parse function-style syntax: type(arg1, arg2, ...) [scale(...)] [rotation(...)] [translate(...)]
        if (rawValue.contains("(") && rawValue.contains(")")) {
            // Extract the main texture part and transformations
            return parseTextureWithTransforms(rawValue);
        } else {
            // Fallback: try to parse as color
            var color = ColorUtils.parseColor(rawValue);
            if (color != null) {
                return new ColorRectTexture(color);
            }
        }
        return null;
    }

    @Nullable
    private static IGuiTexture parseTextureWithTransforms(String rawValue) {
        var calls = tokenizeFunctions(rawValue);
        if (calls.isEmpty()) return null;

        // get the main texture
        var first = calls.getFirst();
        IGuiTexture texture = parseMainTexture(first.name.toLowerCase(), first.args);
        if (texture == null) return null;

        Transform2D transform = new Transform2D();
        Integer color = null;

        // parse modification (e.g., scale/translate/rotation/color ...)
        for (int k = 1; k < calls.size(); k++) {
            var f = calls.get(k);
            String type = f.name.toLowerCase();
            String[] args = splitTopLevelArgs(f.args);

            switch (type) {
                case "scale" -> {
                    if (args.length == 1) {
                        transform.scale(Float.parseFloat(args[0]));
                    } else if (args.length == 2) {
                        transform.scale(Float.parseFloat(args[0]), Float.parseFloat(args[1]));
                    }
                }
                case "rotation", "rotate" -> {
                    if (args.length == 1) {
                        transform.rotation(Float.parseFloat(args[0]));
                    }
                }
                case "translate" -> {
                    if (args.length == 2) {
                        transform.translate(Float.parseFloat(args[0]), Float.parseFloat(args[1]));
                    }
                }
                case "color" -> {
                    if (args.length == 1) {
                        color = ColorUtils.parseColor(args[0]);
                    } else if (args.length == 3) {
                        int r = Integer.parseInt(args[0]);
                        int g = Integer.parseInt(args[1]);
                        int b = Integer.parseInt(args[2]);
                        color = ColorUtils.color(255, r, g, b);
                    } else if (args.length == 4) {
                        int a = Integer.parseInt(args[0]);
                        int r = Integer.parseInt(args[1]);
                        int g = Integer.parseInt(args[2]);
                        int b = Integer.parseInt(args[3]);
                        color = ColorUtils.color(a, r, g, b);
                    }
                }
                default -> {
                }
            }
        }

        var isCopied = false;
        if (color != null) {
            texture = texture.copy().setColor(color);
            isCopied = true;
        }
        if (!transform.isIdentity()) {
            if (texture instanceof TransformTexture transformTexture) {
                if (isCopied) {
                    transformTexture.copyTransform(transform);
                } else if (transformTexture.copy() instanceof TransformTexture copiedTransformTexture) {
                    copiedTransformTexture.copyTransform(transform);
                    texture = copiedTransformTexture;
                }
            }
        }

        return texture;
    }

    @Nullable
    private static IGuiTexture parseMainTexture(String type, String argsStr) {
        String[] args = argsStr.isEmpty() ? new String[0] : splitTopLevelArgs(argsStr);
        // Trim all arguments
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].trim();
        }

        switch (type) {
            case "group" -> {
                var textures = new ArrayList<IGuiTexture>();
                for (var arg : args) {
                    var texture = parseTexture(arg);
                    if (texture != null) textures.add(texture);
                }
                return GuiTextureGroup.of(textures.toArray(IGuiTexture[]::new));
            }
            case "border" -> {
                if (args.length == 2) {
                    var border = Integer.parseInt(args[0]);
                    var color = ColorUtils.parseColor(args[1]);
                    if (color != null) {
                        return new ColorBorderTexture(border, color);
                    }
                }
            }
            case "sprite" -> {
                if (args.length > 0) {
                    var sprite = SpriteTexture.of(args[0]);
                    if (args.length > 4) {
                        sprite.setSprite(Integer.parseInt(args[1]), Integer.parseInt(args[2]),
                                Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                    }
                    if (args.length > 8) {
                        sprite.setBorder(Integer.parseInt(args[5]), Integer.parseInt(args[6]),
                                Integer.parseInt(args[7]), Integer.parseInt(args[8]));
                    }
                    if (args.length > 9) {
                        sprite.setColor(ColorUtils.parseColor(args[9]));
                    }
                    return sprite;
                }
            }
            case "icon" -> {
                if (args.length > 0) {
                    if (args.length > 1) {
                        return Icons.icon(args[0], args[1]);
                    }
                    return Icons.icon(args[0]);
                }
            }
            case "rect" -> {
                // rect(#FF00FF, 0 0 0 0, 4, #FFFFFF)
                if (args.length > 0) {
                    var rect = new RectTexture();
                    rect.setColor(ColorUtils.parseColor(args[0]));
                    if (args.length > 1) {
                        var par = args[1].split(" ");
                        if (par.length == 1) {
                            rect.setRadius(new Vector4f(Float.parseFloat(par[0])));
                        } else if (par.length == 4) {
                            rect.setRadius(new Vector4f(Float.parseFloat(par[0]), Float.parseFloat(par[1]),
                                    Float.parseFloat(par[2]), Float.parseFloat(par[3])));
                        }
                    }
                    if (args.length > 2) {
                        rect.setStroke(Float.parseFloat(args[2]));
                    }
                    if (args.length > 3) {
                        rect.setBorderColor(ColorUtils.parseColor(args[3]));
                    }
                    return rect;
                }
            }
            case "sdf" -> {
                // rect(#FF00FF, 0 0 0 0, 4, #FFFFFF)
                if (args.length > 0) {
                    var sdf = new SDFRectTexture();
                    sdf.setColor(ColorUtils.parseColor(args[0]));
                    if (args.length > 1) {
                        var par = args[1].split(" ");
                        if (par.length == 1) {
                            sdf.setRadius(new Vector4f(Float.parseFloat(par[0])));
                        } else if (par.length == 4) {
                            sdf.setRadius(new Vector4f(Float.parseFloat(par[0]), Float.parseFloat(par[1]),
                                    Float.parseFloat(par[2]), Float.parseFloat(par[3])));
                        }
                    }
                    if (args.length > 2) {
                        sdf.setStroke(Float.parseFloat(args[2]));
                    }
                    if (args.length > 3) {
                        sdf.setBorderColor(ColorUtils.parseColor(args[3]));
                    }
                    return sdf;
                }
            }
            case "shader" -> {
                if (args.length > 0) {
                    return new ShaderTexture(ResourceLocation.parse(args[0]));
                }
            }
            case "vanilla-sprite" -> {
                if (args.length > 0) {
                    return VanillaSpriteTexture.of(args[0]);
                }
            }
            default -> {
                if (args.length > 0) {
                    try {
                        var resourceType = LDLib2Registries.RESOURCE_PROVIDER_TYPES.get(type);
                        if (resourceType != null) {
                            var path = resourceType.createFullPath(args[0]);
                            return TexturesResource.INSTANCE.getResourceInstance().getResource(path);
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
        return null;
    }


    record Func(String name, String args) { }

    // parse tokens based on the format: foo(...)[ space ]bar(...)[ space ]baz(...)
    static List<Func> tokenizeFunctions(String s) {
        ArrayList<Func> out = new ArrayList<>();
        int i = 0, n = s.length();

        while (i < n) {
            // skip whitespace
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;

            // read function name: starts with a letter or '_', followed by [a-zA-Z0-9_-]
            int startName = i;
            if (i < n && (Character.isLetter(s.charAt(i)) || s.charAt(i) == '_')) {
                i++;
                while (i < n) {
                    char c = s.charAt(i);
                    if (Character.isLetterOrDigit(c) || c == '_' || c == '-') i++;
                    else break;
                }
            } else {
                // match non-function name, skip
                break;
            }
            String name = s.substring(startName, i).trim();

            // skip whitespace
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;

            // require a '('
            if (i >= n || s.charAt(i) != '(') {
                // not a function call, backtrack, let outer handle (e.g., resource name)
                // break;
                return out;
            }
            i++; // skip '('

            // keep matching until ')'
            int depth = 1;
            int startArgs = i;
            while (i < n && depth > 0) {
                char c = s.charAt(i);
                if (c == '(') depth++;
                else if (c == ')') depth--;
                i++;
            }
            if (depth != 0) {
                throw new IllegalArgumentException("Unbalanced parentheses in: " + s);
            }
            int endArgs = i - 1; // last ')'
            String args = s.substring(startArgs, endArgs).trim();

            out.add(new Func(name, args));

            // accept whitespace, continue parsing chained functions (e.g., scale(...) rotate(...))
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
        }
        return out;
    }

    // do ',' split
    static String[] splitTopLevelArgs(String s) {
        ArrayList<String> parts = new ArrayList<>();
        int depth = 0;
        int last = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);
            else if (c == ',' && depth == 0) {
                parts.add(s.substring(last, i).trim());
                last = i + 1;
            }
        }
        // end
        if (last <= s.length()) {
            String tail = s.substring(last).trim();
            if (!tail.isEmpty()) parts.add(tail);
        }
        return parts.toArray(String[]::new);
    }
}
