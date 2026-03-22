package com.lowdragmc.lowdraglib2.test.ui;

import com.lowdragmc.lowdraglib2.gui.texture.SDFRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import dev.vfyjxf.taffy.style.FlexDirection;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@LDLRegisterClient(name = "mcp", registry = "ldlib2:screen_test")
@NoArgsConstructor
public class TestMCP implements IScreenTest {
    private static final int BOARD_SIZE = 20;
    private static final int STEP_INTERVAL = 6;

    @Override
    public ModularUI createUI(Player player) {
        var random = new Random();
        var snake = new ArrayDeque<Point>();
        var cells = new UIElement[BOARD_SIZE][BOARD_SIZE];
        var emptyCell = SDFRectTexture.of(0xFF2A2A2A).setRadius(0).setStroke(1).setBorderColor(0xFF3F3F3F);
        var bodyCell = SDFRectTexture.of(0xFF3AA655).setRadius(0).setStroke(1).setBorderColor(0xFF86D69D);
        var headCell = SDFRectTexture.of(0xFF7CFF7C).setRadius(0).setStroke(1).setBorderColor(0xFFD4FFD4);
        var foodCell = SDFRectTexture.of(0xFFFF5A5A).setRadius(0).setStroke(1).setBorderColor(0xFFFFD0D0);

        final int[] directionX = {1};
        final int[] directionY = {0};
        final int[] nextDirectionX = {1};
        final int[] nextDirectionY = {0};
        final int[] foodX = {0};
        final int[] foodY = {0};
        final int[] score = {0};
        final int[] tickCounter = {0};
        final boolean[] alive = {true};

        var scoreLabel = label("score_label", "Score: 0", 10, Horizontal.LEFT);
        var statusLabel = label("status_label", "Status: Running", 10, Horizontal.LEFT);
        var hintLabel = label("hint_label", "Arrows to move", 9, Horizontal.LEFT);

        var board = new UIElement().setId("board")
                .layout(layout -> {
                    layout.width(140);
                    layout.height(140);
                    layout.paddingAll(1);
                    layout.gapAll(0);
                })
                .style(style -> style.backgroundTexture(emptyCell));

        for (int y = 0; y < BOARD_SIZE; y++) {
            var row = new UIElement()
                    .layout(layout -> {
                        layout.widthPercent(100);
                        layout.height(7);
                        layout.flexDirection(FlexDirection.ROW);
                        layout.gapAll(0);
                    });
            for (int x = 0; x < BOARD_SIZE; x++) {
                var cell = new UIElement()
                        .setId("cell_" + x + "_" + y)
                        .layout(layout -> {
                            layout.width(7);
                            layout.height(7);
                        })
                        .style(style -> style.backgroundTexture(emptyCell));
                cells[y][x] = cell;
                row.addChild(cell);
            }
            board.addChild(row);
        }

        Runnable[] renderBoard = new Runnable[1];
        Runnable[] resetGame = new Runnable[1];
        Runnable[] stepGame = new Runnable[1];

        renderBoard[0] = () -> {
            for (int y = 0; y < BOARD_SIZE; y++) {
                for (int x = 0; x < BOARD_SIZE; x++) {
                    cells[y][x].style(style -> style.backgroundTexture(emptyCell));
                }
            }
            boolean head = true;
            for (var segment : snake) {
                var texture = head ? headCell : bodyCell;
                cells[segment.y][segment.x].style(style -> style.backgroundTexture(texture));
                head = false;
            }
            cells[foodY[0]][foodX[0]].style(style -> style.backgroundTexture(foodCell));
            scoreLabel.setText("Score: " + score[0]);
            statusLabel.setText(alive[0] ? "Status: Running" : "Status: Game Over");
        };

        resetGame[0] = () -> {
            snake.clear();
            snake.addFirst(new Point(11, 10));
            snake.addLast(new Point(10, 10));
            snake.addLast(new Point(9, 10));
            directionX[0] = 1;
            directionY[0] = 0;
            nextDirectionX[0] = 1;
            nextDirectionY[0] = 0;
            score[0] = 0;
            tickCounter[0] = 0;
            alive[0] = true;
            spawnFood(random, snake, foodX, foodY);
            renderBoard[0].run();
        };

        stepGame[0] = () -> {
            if (!alive[0]) {
                return;
            }
            directionX[0] = nextDirectionX[0];
            directionY[0] = nextDirectionY[0];

            var head = snake.peekFirst();
            int nextX = head.x + directionX[0];
            int nextY = head.y + directionY[0];

            if (nextX < 0 || nextX >= BOARD_SIZE || nextY < 0 || nextY >= BOARD_SIZE) {
                alive[0] = false;
                renderBoard[0].run();
                return;
            }

            var nextPoint = new Point(nextX, nextY);
            var tail = snake.peekLast();
            boolean hitsBody = snake.stream().anyMatch(segment -> segment.x == nextPoint.x && segment.y == nextPoint.y);
            if (hitsBody && (tail == null || tail.x != nextPoint.x || tail.y != nextPoint.y)) {
                alive[0] = false;
                renderBoard[0].run();
                return;
            }

            snake.addFirst(nextPoint);
            if (nextX == foodX[0] && nextY == foodY[0]) {
                score[0]++;
                spawnFood(random, snake, foodX, foodY);
            } else {
                snake.removeLast();
            }
            renderBoard[0].run();
        };

        var restartButton = new Button();
        restartButton.setId("restart_button");
        restartButton.setText("Restart");
        restartButton.layout(layout -> layout.width(70).height(20));
        restartButton.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            resetGame[0].run();
            if (restartButton.getParent() != null) {
                restartButton.getParent().focus();
            }
        });

        var hudTop = new UIElement().setId("hud_top")
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(6);
                })
                .addChildren(
                        scoreLabel,
                        new UIElement().layout(layout -> layout.flex(1)),
                        restartButton
                );

        var hudBottom = new UIElement().setId("hud_bottom")
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(6);
                })
                .addChildren(statusLabel, new UIElement().layout(layout -> layout.flex(1)), hintLabel);

        var hud = new UIElement().setId("hud")
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.gapAll(4);
                })
                .addChildren(hudTop, hudBottom);

        var root = new UIElement().setId("root")
                .layout(layout -> {
                    layout.width(176);
                    layout.height(194);
                    layout.paddingAll(8);
                    layout.gapAll(6);
                })
                .addClass("panel_bg")
                .addChildren(hud, board);

        root.setFocusable(true);
        root.focus();
        root.addEventListener(UIEvents.KEY_DOWN, event -> {
            if (!alive[0]) {
                return;
            }
            if (event.keyCode == GLFW.GLFW_KEY_UP && directionY[0] != 1) {
                nextDirectionX[0] = 0;
                nextDirectionY[0] = -1;
            } else if (event.keyCode == GLFW.GLFW_KEY_DOWN && directionY[0] != -1) {
                nextDirectionX[0] = 0;
                nextDirectionY[0] = 1;
            } else if (event.keyCode == GLFW.GLFW_KEY_LEFT && directionX[0] != 1) {
                nextDirectionX[0] = -1;
                nextDirectionY[0] = 0;
            } else if (event.keyCode == GLFW.GLFW_KEY_RIGHT && directionX[0] != -1) {
                nextDirectionX[0] = 1;
                nextDirectionY[0] = 0;
            }
        });
        root.addEventListener(UIEvents.TICK, event -> {
            tickCounter[0]++;
            if (tickCounter[0] >= STEP_INTERVAL) {
                tickCounter[0] = 0;
                stepGame[0].run();
            }
        });

        resetGame[0].run();

        return new ModularUI(UI.of(root, List.of(StylesheetManager.INSTANCE.getStylesheetSafe(StylesheetManager.MC))), player);
    }

    private static void spawnFood(Random random, ArrayDeque<Point> snake, int[] foodX, int[] foodY) {
        var freeCells = new ArrayList<Point>();
        for (int y = 0; y < BOARD_SIZE; y++) {
            for (int x = 0; x < BOARD_SIZE; x++) {
                int finalX = x;
                int finalY = y;
                boolean occupied = snake.stream().anyMatch(segment -> segment.x == finalX && segment.y == finalY);
                if (!occupied) {
                    freeCells.add(new Point(x, y));
                }
            }
        }
        if (freeCells.isEmpty()) {
            foodX[0] = 0;
            foodY[0] = 0;
            return;
        }
        var point = freeCells.get(random.nextInt(freeCells.size()));
        foodX[0] = point.x;
        foodY[0] = point.y;
    }

    private static Label label(String id, String text, int fontSize, Horizontal align) {
        var label = new Label();
        label.setId(id);
        label.setText(text);
        label.textStyle(style -> style
                .fontSize(fontSize)
                .textShadow(false)
                .textAlignHorizontal(align)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }
    private record Point(int x, int y) {
    }
}
