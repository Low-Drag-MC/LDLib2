package com.lowdragmc.lowdraglib.client.model.custommodel;

import com.lowdragmc.lowdraglib.client.bakedpipeline.Quad;
import com.lowdragmc.lowdraglib.client.bakedpipeline.Submap;
import com.lowdragmc.lowdraglib.client.model.ModelFactory;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

import static com.lowdragmc.lowdraglib.client.model.forge.LDLRendererModel.RendererBakedModel.POS;
import static com.lowdragmc.lowdraglib.client.model.forge.LDLRendererModel.RendererBakedModel.WORLD;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Used to bake the model with emissive effect, as well as connected textures.
 */
@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomBakedModel<T extends BakedModel> extends BakedModelWrapper<T> {
    private final ConcurrentMap<Direction, ConcurrentMap<Connections, List<BakedQuad>>> sideCache;
    private final List<BakedQuad> noSideCache;

    public CustomBakedModel(T parent) {
        super(parent);
        this.sideCache = new ConcurrentHashMap<>();
        this.noSideCache = new ArrayList<>();
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        BlockAndTintGetter level = data.get(WORLD);
        BlockPos pos = data.get(POS);
        if (level != null && pos != null && state != null) {
            return getCustomQuads(level, pos, state, side, rand, data, renderType);
        } else {
            return super.getQuads(state, side, rand, data, renderType);
        }
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        modelData = super.getModelData(level, pos, state, modelData);
        return modelData.derive()
                .with(WORLD, level)
                .with(POS, pos)
                .build();
    }

    @Nonnull
    public List<BakedQuad> getCustomQuads(BlockAndTintGetter level, BlockPos pos, @Nonnull BlockState state, @Nullable Direction side, RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        var connections = Connections.checkConnections(level, pos, state, side);
        if (side == null) {
            if (noSideCache.isEmpty()) {
                synchronized (noSideCache) {
                    if (noSideCache.isEmpty()) {
                        noSideCache.addAll(buildCustomQuads(connections, getQuads(state, null, rand), 0.0f));
                    }
                }
            }
            return noSideCache;
        }
        return sideCache
                .computeIfAbsent(side, key -> new ConcurrentHashMap<>())
                .computeIfAbsent(connections, key -> buildCustomQuads(connections, getQuads(state, side, rand), 0.0f));
    }

    public static List<BakedQuad> reBakeCustomQuads(List<BakedQuad> quads, BlockAndTintGetter level, BlockPos pos, @Nonnull BlockState state, @Nullable Direction side, float offset) {
        return buildCustomQuads(Connections.checkConnections(level, pos, state, side), quads, offset);
    }

    public static List<BakedQuad> buildCustomQuads(Connections connections, List<BakedQuad> base, float offset) {
        List<BakedQuad> result = new LinkedList<>();
        for (BakedQuad bakedQuad : base) {
            var section = LDLMetadataSection.getMetadata(bakedQuad.getSprite());
            TextureAtlasSprite connection = section.connection == null ? null : ModelFactory.getBlockSprite(section.connection);
            if (connection == null) {
                result.add(makeQuad(bakedQuad, section, offset).rebake());
                continue;
            }

            Quad quad = makeQuad(bakedQuad, section, offset).derotate();
            Quad[] quads = quad.subdivide(4);

            int[] ctm = connections.getSubmapIndices();

            for (int j = 0; j < quads.length; j++) {
                Quad q = quads[j];
                if (q != null) {
                    int ctmid = q.getUvs().normalize().getQuadrant();
                    quads[j] = q.grow().transformUVs(ctm[ctmid] > 15 ? bakedQuad.getSprite() : connection, Submap.uvs[ctm[ctmid]]);
                }
            }
            result.addAll(Arrays.stream(quads).filter(Objects::nonNull).map(Quad::rebake).toList());
        }
        return result;
    }

    protected static Quad makeQuad(BakedQuad bq, LDLMetadataSection section, float offset) {
        Quad q = Quad.from(bq, offset);
        if (section.emissive) {
            q = q.setLight(15, 15);
        }
        return q;
    }
}
