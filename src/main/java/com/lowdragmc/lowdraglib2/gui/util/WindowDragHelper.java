package com.lowdragmc.lowdraglib2.gui.util;

import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class WindowDragHelper {
    public record DragMove(float startX, float startY) {};

    public enum ResizeHandle {
        LEFT(Icons.ARROW_LEFT_RIGHT),
        RIGHT(Icons.ARROW_LEFT_RIGHT),
        TOP(Icons.ARROW_UP_DOWN),
        BOTTOM(Icons.ARROW_UP_DOWN),
        TOP_LEFT(Icons.ARROW_LT_RB),
        TOP_RIGHT(Icons.ARROW_RT_LB),
        BOTTOM_LEFT(Icons.ARROW_RT_LB),
        BOTTOM_RIGHT(Icons.ARROW_LT_RB);
        public final SpriteTexture icon;

        ResizeHandle(SpriteTexture icon) {
            this.icon = icon;
        }
    }

    public record DragResize(
            float startX, float startY,
            float startW, float startH,
            ResizeHandle handle
    ) {}

    public static void setDragMove(UIElement element, UIElement target, @Nullable Predicate<UIEvent> movePredicate, @Nullable Consumer<UIEvent> onFinish) {
        element.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if ((movePredicate == null || movePredicate.test(e))) {
                var icon = Icons.MOVE;
                var width = 12;
                var height = 12;
                element.startDrag(new DragMove(target.getLayoutX(), target.getLayoutY()), icon)
                        .setDragTexture(- width / 2f, -height / 2f, width, height);
                e.stopPropagation();
            }
        });
        element.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            if (e.dragHandler.draggingObject instanceof DragMove(var sx, var sy)) {
                var normalPosOffset = element.getLocalMouseNormal(e.x - e.dragStartX, e.y - e.dragStartY);
                target.getLayout()
                        .left(sx + normalPosOffset.x)
                        .top(sy + normalPosOffset.y);
            }
        });
        if (onFinish != null) element.addEventListener(UIEvents.DRAG_END, onFinish::accept);
    }

    public static void setBorderResize(UIElement element, UIElement target, float border, Vector2f minSize, Vector2f maxSize,
                                       @Nullable Predicate<UIEvent> resizePredicate,
                                       @Nullable BiPredicate<UIEvent, DragResize> dragResizePredicate,
                                       @Nullable Consumer<UIEvent> onFinish) {
        setBorderResize(element, target, border, minSize, maxSize, EnumSet.allOf(ResizeHandle.class),
                resizePredicate, dragResizePredicate, onFinish);
    }

    public static void setBorderResize(UIElement element, UIElement target, float border, Vector2f minSize, Vector2f maxSize,
                                       Set<ResizeHandle> allowedHandles,
                                       @Nullable Predicate<UIEvent> resizePredicate,
                                       @Nullable BiPredicate<UIEvent, DragResize> dragResizePredicate,
                                       @Nullable Consumer<UIEvent> onFinish) {
        element.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (resizePredicate != null && !resizePredicate.test(e)) return;
            var handle = detectResizeHandle(element, e.x, e.y, border, allowedHandles);
            if (handle != null) {
                var icon = handle.icon;
                var width = handle.icon.spriteSize.width;
                var height = handle.icon.spriteSize.height;
                element.startDrag(new DragResize(
                                target.getLayoutX(), target.getLayoutY(),
                                target.getSizeWidth(), target.getSizeHeight(), handle), icon)
                         .setDragTexture(- width / 2f, -height / 2f, width, height);
                e.stopPropagation();
            }
        });

        element.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
            if (!(e.dragHandler.draggingObject instanceof DragResize(
                    float startX, float startY, float startW, float startH, ResizeHandle handle
            ))) return;

            if (dragResizePredicate != null && !dragResizePredicate.test(e, (DragResize) e.dragHandler.draggingObject)) return;
            var d = element.getLocalMouseNormal(e.x - e.dragStartX, e.y - e.dragStartY);
            float dx = d.x;
            float dy = d.y;
            float x = startX;
            float y = startY;
            float w = startW;
            float h = startH;

            float minW = minSize.x, maxW = maxSize.x;
            float minH = minSize.y, maxH = maxSize.y;

            switch (handle) {
                case LEFT -> {
                    float clampedNewW = Math.min(maxW, Math.max(minW, startW - dx));
                    float dxApplied = startW - clampedNewW;     // 实际生效的 dx
                    x = startX + dxApplied;
                    w = clampedNewW;
                }
                case RIGHT -> {
                    w = Math.min(maxW, Math.max(minW, startW + dx));
                    x = startX;
                }
                case TOP -> {
                    float clampedNewH = Math.min(maxH, Math.max(minH, startH - dy));
                    float dyApplied = startH - clampedNewH;
                    y = startY + dyApplied;
                    h = clampedNewH;
                }
                case BOTTOM -> {
                    h = Math.min(maxH, Math.max(minH, startH + dy));
                    y = startY;
                }
                case TOP_LEFT -> {
                    float clampedNewW = Math.min(maxW, Math.max(minW, startW - dx));
                    float dxApplied = startW - clampedNewW;
                    x = startX + dxApplied;
                    w = clampedNewW;

                    float clampedNewH = Math.min(maxH, Math.max(minH, startH - dy));
                    float dyApplied = startH - clampedNewH;
                    y = startY + dyApplied;
                    h = clampedNewH;
                }
                case TOP_RIGHT -> {
                    w = Math.min(maxW, Math.max(minW, startW + dx));
                    x = startX;

                    float clampedNewH = Math.min(maxH, Math.max(minH, startH - dy));
                    float dyApplied = startH - clampedNewH;
                    y = startY + dyApplied;
                    h = clampedNewH;
                }
                case BOTTOM_LEFT -> {
                    float clampedNewW = Math.min(maxW, Math.max(minW, startW - dx));
                    float dxApplied = startW - clampedNewW;
                    x = startX + dxApplied;
                    w = clampedNewW;

                    h = Math.min(maxH, Math.max(minH, startH + dy));
                    y = startY;
                }
                case BOTTOM_RIGHT -> {
                    w = Math.min(maxW, Math.max(minW, startW + dx));
                    h = Math.min(maxH, Math.max(minH, startH + dy));
                    x = startX;
                    y = startY;
                }
            }

            target.getLayout()
                    .left(x)
                    .top(y)
                    .width(w)
                    .height(h);
        });
        if (onFinish != null) element.addEventListener(UIEvents.DRAG_END, onFinish::accept);
    }

    @Nullable
    public static ResizeHandle detectResizeHandle(UIElement element, float mouseWorldX, float mouseWorldY, float padding) {
        return detectResizeHandle(element, mouseWorldX, mouseWorldY, padding, EnumSet.allOf(ResizeHandle.class));
    }

    @Nullable
    public static ResizeHandle detectResizeHandle(UIElement element, float mouseWorldX, float mouseWorldY, float padding, Set<ResizeHandle> allowedHandles) {
        var local = element.getLocalMouse(mouseWorldX, mouseWorldY).sub(element.getPositionX(), element.getPositionY());
        var mx = local.x;
        var my = local.y;

        float w = element.getSizeWidth();
        float h = element.getSizeHeight();

        boolean left = mx >= 0 && mx <= padding;
        boolean right = mx >= (w - padding) && mx <= w;
        boolean top = my >= 0 && my <= padding;
        boolean bottom = my >= (h - padding) && my <= h;

        if (left && top && allowedHandles.contains(ResizeHandle.TOP_LEFT)) return ResizeHandle.TOP_LEFT;
        if (right && top && allowedHandles.contains(ResizeHandle.TOP_RIGHT)) return ResizeHandle.TOP_RIGHT;
        if (left && bottom && allowedHandles.contains(ResizeHandle.BOTTOM_LEFT)) return ResizeHandle.BOTTOM_LEFT;
        if (right && bottom && allowedHandles.contains(ResizeHandle.BOTTOM_RIGHT)) return ResizeHandle.BOTTOM_RIGHT;

        if (left && allowedHandles.contains(ResizeHandle.LEFT)) return ResizeHandle.LEFT;
        if (right && allowedHandles.contains(ResizeHandle.RIGHT)) return ResizeHandle.RIGHT;
        if (top && allowedHandles.contains(ResizeHandle.TOP)) return ResizeHandle.TOP;
        if (bottom && allowedHandles.contains(ResizeHandle.BOTTOM)) return ResizeHandle.BOTTOM;

        return null;
    }

    public static void drawResizeIcon(GUIContext guiContext, UIElement element, float padding) {
        drawResizeIcon(guiContext, element, padding, EnumSet.allOf(ResizeHandle.class));
    }

    public static void drawResizeIcon(GUIContext guiContext, UIElement element, float padding, Set<ResizeHandle> allowedHandles) {
        var handle = WindowDragHelper.detectResizeHandle(element, guiContext.mouseX, guiContext.mouseY, padding, allowedHandles);
        if (handle == null) return;
        guiContext.postRendering(ctx -> {
            // Draw in screen space: reset the pose to identity so that mouseX/mouseY
            // (which are screen coordinates) are not further transformed.
            ctx.pose.pushPose();
            ctx.pose.setIdentity();
            ctx.drawTexture(handle.icon,
                    ctx.mouseX - handle.icon.spriteSize.width / 2f,
                    ctx.mouseY - handle.icon.spriteSize.height / 2f,
                    handle.icon.spriteSize.width,
                    handle.icon.spriteSize.height);
            ctx.pose.popPose();
        });
    }
}
