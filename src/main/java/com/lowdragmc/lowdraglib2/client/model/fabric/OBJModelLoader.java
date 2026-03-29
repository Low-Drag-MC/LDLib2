package com.lowdragmc.lowdraglib2.client.model.fabric;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.LDLib2;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.joml.Vector2f;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class OBJModelLoader implements ModelResolver {
    public static final OBJModelLoader INSTANCE = new OBJModelLoader();

    private final Map<ResourceLocation, UnbakedModel> cache = new HashMap<>();

    @Override
    public @Nullable UnbakedModel resolveModel(Context context) {
        ResourceLocation id = context.id();
        if (id.getPath().endsWith(".obj")) {
            return cache.computeIfAbsent(id, this::loadOBJ);
        } else if (id.getPath().endsWith(".json")) {
            try {
                var resource = Minecraft.getInstance().getResourceManager().getResource(id);
                if (resource.isEmpty()) return null;
                try (var reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
                    JsonObject json = GsonHelper.parse(reader);
                    if (json.has("loader") && (json.get("loader").getAsString().equals("neoforge:obj") || json.get("loader").getAsString().equals("ldlib2:obj"))) {
                        ResourceLocation modelLocation = ResourceLocation.parse(json.get("model").getAsString());
                        boolean flipV = json.has("flip_v") && json.get("flip_v").getAsBoolean();
                        // Recursive call or direct load
                        UnbakedModel model = cache.computeIfAbsent(modelLocation, this::loadOBJ);
                        if (model instanceof OBJUnbakedModel objModel) {
                            return new OBJWrapperUnbakedModel(objModel, flipV);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private UnbakedModel loadOBJ(ResourceLocation location) {
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) return null;
            
            List<Vector3f> vertices = new ArrayList<>();
            List<Vector2f> uvs = new ArrayList<>();
            List<Vector3f> normals = new ArrayList<>();
            List<int[][]> faces = new ArrayList<>(); // vertex/uv/normal indices

            try (var reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("\\s+");
                    switch (parts[0]) {
                        case "v" -> vertices.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                        case "vt" -> uvs.add(new Vector2f(Float.parseFloat(parts[1]), 1.0f - Float.parseFloat(parts[2])));
                        case "vn" -> normals.add(new Vector3f(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])));
                        case "f" -> {
                            int[][] face = new int[parts.length - 1][3];
                            for (int i = 1; i < parts.length; i++) {
                                String[] indices = parts[i].split("/");
                                face[i - 1][0] = Integer.parseInt(indices[0]) - 1;
                                if (indices.length > 1 && !indices[1].isEmpty()) face[i - 1][1] = Integer.parseInt(indices[1]) - 1;
                                if (indices.length > 2 && !indices[2].isEmpty()) face[i - 1][2] = Integer.parseInt(indices[2]) - 1;
                            }
                            faces.add(face);
                        }
                    }
                }
            }
            return new OBJUnbakedModel(location, vertices, uvs, normals, faces);
        } catch (Exception e) {
            LDLib2.LOGGER.error("Failed to load OBJ model: " + location, e);
            return null;
        }
    }

    private static class OBJUnbakedModel implements UnbakedModel {
        private final ResourceLocation location;
        private final List<Vector3f> vertices;
        private final List<Vector2f> uvs;
        private final List<Vector3f> normals;
        private final List<int[][]> faces;
        private final boolean flipV;

        public OBJUnbakedModel(ResourceLocation location, List<Vector3f> vertices, List<Vector2f> uvs, List<Vector3f> normals, List<int[][]> faces) {
            this(location, vertices, uvs, normals, faces, false);
        }

        public OBJUnbakedModel(ResourceLocation location, List<Vector3f> vertices, List<Vector2f> uvs, List<Vector3f> normals, List<int[][]> faces, boolean flipV) {
            this.location = location;
            this.vertices = vertices;
            this.uvs = uvs;
            this.normals = normals;
            this.faces = faces;
            this.flipV = flipV;
        }

        @Override
        public Collection<ResourceLocation> getDependencies() {
            return Collections.emptyList();
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {
        }

        @Nullable
        @Override
        public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
            // Try to use the model location as a texture location (removing 'models/' prefix if present)
            String path = location.getPath();
            if (path.startsWith("models/")) path = path.substring(7);
            if (path.endsWith(".obj")) path = path.substring(0, path.length() - 4);
            TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.fromNamespaceAndPath(location.getNamespace(), path)));
            if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("missingno")));
            }
            return new OBJBakedModel(vertices, uvs, normals, faces, sprite, flipV);
        }
    }

    private static class OBJWrapperUnbakedModel implements UnbakedModel {
        private final OBJUnbakedModel model;
        private final boolean flipV;

        public OBJWrapperUnbakedModel(OBJUnbakedModel model, boolean flipV) {
            this.model = model;
            this.flipV = flipV;
        }

        @Override
        public Collection<ResourceLocation> getDependencies() {
            return model.getDependencies();
        }

        @Override
        public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {
            model.resolveParents(modelGetter);
        }

        @Nullable
        @Override
        public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
            String path = model.location.getPath();
            if (path.startsWith("models/")) path = path.substring(7);
            if (path.endsWith(".obj")) path = path.substring(0, path.length() - 4);
            TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.fromNamespaceAndPath(model.location.getNamespace(), path)));
            if (sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("missingno")));
            }
            return new OBJBakedModel(model.vertices, model.uvs, model.normals, model.faces, sprite, flipV);
        }
    }

    private static class OBJBakedModel implements BakedModel {
        private final List<BakedQuad> quads = new ArrayList<>();
        private final TextureAtlasSprite particleIcon;
        private final boolean flipV;

        public OBJBakedModel(List<Vector3f> vertices, List<Vector2f> uvs, List<Vector3f> normals, List<int[][]> faces, TextureAtlasSprite sprite, boolean flipV) {
            this.particleIcon = sprite;
            this.flipV = flipV;
            for (int[][] face : faces) {
                if (face.length < 3) continue;
                // Simple triangulation for N-gons
                for (int i = 1; i < face.length - 1; i++) {
                    quads.add(createQuad(vertices, uvs, normals, face[0], face[i], face[i + 1], face[i + 1], sprite));
                }
            }
        }

        private BakedQuad createQuad(List<Vector3f> vertices, List<Vector2f> uvs, List<Vector3f> normals, 
                                     int[] v1, int[] v2, int[] v3, int[] v4, TextureAtlasSprite sprite) {
            int[] data = new int[32];
            fillVertex(data, 0, vertices.get(v1[0]), uvs.isEmpty() ? null : uvs.get(v1[1]), normals.isEmpty() ? null : normals.get(v1[2]), sprite);
            fillVertex(data, 1, vertices.get(v2[0]), uvs.isEmpty() ? null : uvs.get(v2[1]), normals.isEmpty() ? null : normals.get(v2[2]), sprite);
            fillVertex(data, 2, vertices.get(v3[0]), uvs.isEmpty() ? null : uvs.get(v3[1]), normals.isEmpty() ? null : normals.get(v3[2]), sprite);
            fillVertex(data, 3, vertices.get(v4[0]), uvs.isEmpty() ? null : uvs.get(v4[1]), normals.isEmpty() ? null : normals.get(v4[2]), sprite);
            return new BakedQuad(data, 0, Direction.UP, sprite, true);
        }

        private void fillVertex(int[] data, int index, Vector3f pos, @Nullable Vector2f uv, @Nullable Vector3f normal, TextureAtlasSprite sprite) {
            int offset = index * 8;
            // Position
            data[offset] = Float.floatToRawIntBits(pos.x());
            data[offset + 1] = Float.floatToRawIntBits(pos.y());
            data[offset + 2] = Float.floatToRawIntBits(pos.z());
            // Color (0xFFFFFFFF)
            data[offset + 3] = -1;
            // UV0
            if (uv != null) {
                float v = flipV ? 1.0f - uv.y() : uv.y();
                data[offset + 4] = Float.floatToRawIntBits(sprite.getU(uv.x() * 16));
                data[offset + 5] = Float.floatToRawIntBits(sprite.getV(v * 16));
            } else {
                data[offset + 4] = Float.floatToRawIntBits(sprite.getU0());
                data[offset + 5] = Float.floatToRawIntBits(sprite.getV0());
            }
            // UV1 (Light)
            data[offset + 6] = 0;
            // Normal
            if (normal != null) {
                int nx = ((int) (normal.x() * 127)) & 0xFF;
                int ny = ((int) (normal.y() * 127)) & 0xFF;
                int nz = ((int) (normal.z() * 127)) & 0xFF;
                data[offset + 7] = nx | (ny << 8) | (nz << 16) | (0xFF << 24); // 0xFF alpha/padding
            } else {
                data[offset + 7] = 0;
            }
        }


        @Override
        public List<BakedQuad> getQuads(@Nullable net.minecraft.world.level.block.state.BlockState state, @Nullable Direction side, net.minecraft.util.RandomSource rand) {
            return side == null ? quads : Collections.emptyList();
        }

        @Override
        public boolean useAmbientOcclusion() { return true; }
        @Override
        public boolean isGui3d() { return true; }
        @Override
        public boolean usesBlockLight() { return true; }
        @Override
        public boolean isCustomRenderer() { return false; }
        @Override
        public TextureAtlasSprite getParticleIcon() { return particleIcon; }
        @Override
        public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
        @Override
        public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
    }
}
