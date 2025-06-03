package com.lowdragmc.lowdraglib.compass.component.animation;

import com.lowdragmc.lowdraglib.utils.data.BlockInfo;
import com.lowdragmc.lowdraglib.utils.data.BlockPosFace;
import com.lowdragmc.lowdraglib.utils.data.EntityInfo;
import com.lowdragmc.lowdraglib.utils.XmlUtils;
import lombok.val;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec2;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.joml.Vector3f;
import org.w3c.dom.Element;

import java.util.*;

/**
 * @author KilaBash
 * @date 2023/7/28
 * @implNote SceneAction
 */
public class SceneAction extends Action {
    private final List<Tuple<BlockAnima, BlockInfo>> addedBlocks = new ArrayList<>();
    private final List<BlockAnima> removedBlocks = new ArrayList<>();
    private final Map<BlockPos, BlockInfo> modifiedTags = new HashMap<>();
    private final Map<BlockPosFace, Integer> highlightedBlocks = new HashMap<>();
    private final List<Tuple<EntityInfo, Vector3f>> addedEntities = new ArrayList<>();
    private final List<Tuple<EntityInfo, Vector3f>> modifiedEntities = new ArrayList<>();
    private final List<Tuple<EntityInfo, Boolean>> removedEntities = new ArrayList<>();
    private final Map<Vector3f, MutableTriple<Tuple<XmlUtils.SizedIngredient, List<Component>>, Vec2, Integer>> tooltipBlocks = new HashMap<>();
    private Float rotation;
    //runtime
    private int duration = -1;

    public SceneAction() {
    }

    public SceneAction(Element element) {
        super(element);
        final var nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            final var node = nodes.item(i);
            if (node instanceof Element data) {
                val nodeName = data.getNodeName();
                val blockPos = XmlUtils.getAsBlockPos(data, "pos", BlockPos.ZERO);
                val pos = XmlUtils.getAsVector3f(data, "pos", new Vector3f());
                switch (nodeName) {
                    case "add" -> addedBlocks.add(new Tuple<>(new BlockAnima(blockPos, XmlUtils.getAsVector3f(data, "offset", new Vector3f(0, 0.7f, 0)), XmlUtils.getAsInt(data, "duration", 15)), XmlUtils.getBlockInfo(data)));
                    case "remove" -> removedBlocks.add(new BlockAnima(blockPos, XmlUtils.getAsVector3f(data, "offset", new Vector3f(0, 0.7f, 0)), XmlUtils.getAsInt(data, "duration", 15)));
                    case "modify" -> modifiedTags.put(blockPos, XmlUtils.getBlockInfo(data));
                    case "add-entity" -> addedEntities.add(new Tuple<>(XmlUtils.getEntityInfo(data), pos));
                    case "modify-entity" -> modifiedEntities.add(new Tuple<>(XmlUtils.getEntityInfo(data), XmlUtils.getAsVector3f(data, "pos", null)));
                    case "remove-entity" -> removedEntities.add(new Tuple<>(XmlUtils.getEntityInfo(data), XmlUtils.getAsBoolean(data, "force", false)));
                    case "rotation" -> rotation = XmlUtils.getAsFloat(data, "degree", 0f);
                    case "highlight" -> highlightedBlocks.put(new BlockPosFace(blockPos, XmlUtils.getAsEnum(data, "face", Direction.class, null)), XmlUtils.getAsInt(data, "duration", 40));
                    case "tooltip" -> tooltipBlocks.put(XmlUtils.getAsVector3f(data, "pos", new Vector3f(0, 0, 0)), MutableTriple.of(new Tuple<>(XmlUtils.getIngredient(data), new ArrayList<>(XmlUtils.getComponents(data, Style.EMPTY))), XmlUtils.getAsVec2(data, "screen-offset", new Vec2(0.3f, 0.3f)), XmlUtils.getAsInt(data, "duration", 40)));
                }
            }
        }
    }

    public SceneAction rotation(Float rotation) {
        this.rotation = rotation;
        return this;
    }

    public SceneAction addedBlock(BlockPos pos, BlockInfo blockInfo, Vector3f offset, int duration) {
        addedBlocks.add(new Tuple<>(new BlockAnima(pos, offset, duration), blockInfo));
        return this;
    }

    public SceneAction removedBlock(BlockPos pos, Vector3f offset, int duration) {
        removedBlocks.add(new BlockAnima(pos, offset, duration));
        return this;
    }

    public SceneAction modifiedTag(BlockPos pos, BlockInfo blockInfo) {
        modifiedTags.put(pos, blockInfo);
        return this;
    }

    public SceneAction highlightedBlock(BlockPos pos, Direction face, int duration) {
        highlightedBlocks.put(new BlockPosFace(pos, face), duration);
        return this;
    }

    public SceneAction addedEntity(EntityInfo entityInfo, Vector3f pos) {
        addedEntities.add(new Tuple<>(entityInfo, pos));
        return this;
    }

    public SceneAction modifiedEntity(EntityInfo entityInfo, Vector3f pos) {
        modifiedEntities.add(new Tuple<>(entityInfo, pos));
        return this;
    }

    public SceneAction removedEntity(EntityInfo entityInfo, boolean force) {
        removedEntities.add(new Tuple<>(entityInfo, force));
        return this;
    }

    public SceneAction tooltip(Vector3f pos, Tuple<XmlUtils.SizedIngredient, List<Component>> tooltip, Vec2 screenOffset, int duration) {
        tooltipBlocks.put(pos, MutableTriple.of(tooltip, screenOffset, duration));
        return this;
    }

    @Override
    public int getDuration() {
        if (duration == -1) {
            duration = 0;
            for (var tuple : addedBlocks) {
                duration = Math.max(duration, tuple.getA().duration());
            }
            for (var block : removedBlocks) {
                duration = Math.max(duration, block.duration());
            }
            for (var entry : highlightedBlocks.entrySet()) {
                duration = Math.max(duration, entry.getValue());
            }
            for (var entry : tooltipBlocks.entrySet()) {
                duration = Math.max(duration, entry.getValue().getRight());
            }
        }
        return duration + 5;
    }

    @Override
    public void performAction(AnimationFrame frame, CompassScene scene, boolean anima) {
        for (Tuple<BlockAnima, BlockInfo> tuple : addedBlocks) {
            var blockInfo = tuple.getB();
            scene.addBlock(tuple.getA().pos(), blockInfo, anima ? tuple.getA() : null);
        }

        for (var block : removedBlocks) {
            scene.removeBlock(block.pos(), anima ? block : null);
        }
        for (var entry : modifiedTags.entrySet()) {
            scene.addBlock(entry.getKey(), entry.getValue(), null);
        }
        for (var tuple : addedEntities) {
            var pos = tuple.getB();
            scene.addEntity(tuple.getA(), pos, false);
        }
        for (var tuple : modifiedEntities) {
            var pos = tuple.getB();
            scene.addEntity(tuple.getA(), pos, true);
        }
        for (var tuple : removedEntities) {
            scene.removeEntity(tuple.getA(), tuple.getB());
        }
        if (anima) {
            for (var entry : highlightedBlocks.entrySet()) {
                scene.highlightBlock(entry.getKey(), entry.getValue());
            }
        }
        if (anima) {
            for (var entry : tooltipBlocks.entrySet()) {
                scene.addTooltip(entry.getKey(), entry.getValue().getLeft(), entry.getValue().getMiddle(), entry.getValue().getRight());
            }
        }
        if (rotation != null) {
            scene.rotate(rotation, anima);
        }
    }
}
