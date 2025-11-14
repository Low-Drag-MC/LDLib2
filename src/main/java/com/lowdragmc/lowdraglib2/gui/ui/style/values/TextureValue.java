package com.lowdragmc.lowdraglib2.gui.ui.style.values;

import com.lowdragmc.lowdraglib2.LDLib2Registries;
import com.lowdragmc.lowdraglib2.editor.resource.TexturesResource;
import com.lowdragmc.lowdraglib2.gui.texture.*;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextureValue extends StyleValue<IGuiTexture> {
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(\\S+?)\\s*\\(([^)]*)\\)");

    public TextureValue(String rawValue) {
        super(rawValue);
    }

    @Override
    protected @Nullable IGuiTexture doCompute(String rawValue) {
        return parseTexture(rawValue);
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

        // 第一个是“主纹理”
        var first = calls.get(0);
        IGuiTexture texture = parseMainTexture(first.name.toLowerCase(), first.args);
        if (texture == null) return null;

        Transform2D transform = new Transform2D();
        Integer color = null;

        // 后面的都是“修饰函数”（scale/translate/rotation/color ...）
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
                    // 也支持链式 group(...) 再跟一个 transform（已经处理）
                    // 其他未知修饰符可忽略或报错
                }
            }
        }

        if (color != null) {
            texture = texture.copy().setColor(color);
        }
        if (texture instanceof TransformTexture tt) {
            tt = tt.copyWithTransform();
            tt.getTransform2D().copyFrom(transform);
            texture = tt;
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
            case "rect", "sdf" -> {
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

    // 解析整个字符串里的函数调用链：foo(...)[ space ]bar(...)[ space ]baz(...)
    static List<Func> tokenizeFunctions(String s) {
        ArrayList<Func> out = new ArrayList<>();
        int i = 0, n = s.length();

        while (i < n) {
            // 跳过空白
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;

            // 读取函数名：字母或 '_' 开头，后续 [a-zA-Z0-9_-]
            int startName = i;
            if (i < n && (Character.isLetter(s.charAt(i)) || s.charAt(i) == '_')) {
                i++;
                while (i < n) {
                    char c = s.charAt(i);
                    if (Character.isLetterOrDigit(c) || c == '_' || c == '-') i++;
                    else break;
                }
            } else {
                // 遇到非函数名内容（比如纯色 "#ff0"），直接跳出让上层用颜色解析
                break;
            }
            String name = s.substring(startName, i).trim();

            // 跳过空白
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;

            // 必须跟一个 '('
            if (i >= n || s.charAt(i) != '(') {
                // 不是函数调用，回退，让外层处理（比如资源名）
                // 这里也可选择抛错
                // break;
                return out; // 已有的先返回
            }
            i++; // 跳过 '('

            // 寻找匹配的 ')'
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
            int endArgs = i - 1; // 最后一个 ')'
            String args = s.substring(startArgs, endArgs).trim();

            out.add(new Func(name, args));

            // 接受空白，继续解析链式后面的函数（如 scale(...) rotate(...))
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
        }
        return out;
    }

    // 在“顶层逗号”处分割参数（忽略括号内部的逗号）
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
        // 收尾
        if (last <= s.length()) {
            String tail = s.substring(last).trim();
            if (!tail.isEmpty()) parts.add(tail);
        }
        return parts.toArray(String[]::new);
    }
}
