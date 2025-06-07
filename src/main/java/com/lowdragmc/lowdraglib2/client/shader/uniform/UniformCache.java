package com.lowdragmc.lowdraglib2.client.shader.uniform;

import com.lowdragmc.lowdraglib2.Platform;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.util.FastColor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

public class UniformCache {
	protected static final FloatBuffer MATRIX4F_BUFFER = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

	private final Int2ObjectMap<UniformEntry<?>> entryCache = new Int2ObjectOpenHashMap<>();
	private final Object2IntMap<String> locationCache = new Object2IntOpenHashMap<>();
	private final int programId;

	public UniformCache(int programId) {
		this.programId = programId;
	}

	public void invalidate() {
		entryCache.clear();
		locationCache.clear();
	}

	public void glUniform1F(String location, float v0) {
		glUniformF(location, (loc) -> GL20.glUniform1f(loc, v0), v0);
	}

	public void glUniform2F(String location, float v0, float v1) {
		glUniformF(location, (loc) -> GL20.glUniform2f(loc, v0, v1), v0, v1);
	}

	public void glUniform3F(String location, float v0, float v1, float v2) {
		glUniformF(location, (loc) -> GL20.glUniform3f(loc, v0, v1, v2), v0, v1, v2);
	}

	public void glUniform4F(String location, float v0, float v1, float v2, float v3) {
		glUniformF(location, (loc) -> GL20.glUniform4f(loc, v0, v1, v2, v3), v0, v1, v2, v3);
	}

	public void fillRGBAColor(String location, int color) {
		this.glUniform4F(location,
				FastColor.ARGB32.red(color) / 255f,
				FastColor.ARGB32.green(color) / 255f,
				FastColor.ARGB32.blue(color) / 255f,
				FastColor.ARGB32.alpha(color) / 255f);
	}

	private void glUniformF(String location, IntConsumer callback, float... values) {
		glUniform(location, UniformEntry.IS_FLOAT, UniformEntry.FloatUniformEntry.NEW, callback, values);
	}

	public void glUniform1I(String location, int v0) {
		glUniformI(location, (loc) -> GL20.glUniform1i(loc, v0), v0);
	}

	public void glUniform2I(String location, int v0, int v1) {
		glUniformI(location, (loc) -> GL20.glUniform2i(loc, v0, v1), v0, v1);
	}

	public void glUniform3I(String location, int v0, int v1, int v2) {
		glUniformI(location, (loc) -> GL20.glUniform3i(loc, v0, v1, v2), v0, v1, v2);
	}

	public void glUniform4I(String location, int v0, int v1, int v2, int v3) {
		glUniformI(location, (loc) -> GL20.glUniform4i(loc, v0, v1, v2, v3), v0, v1, v2, v3);
	}

	private void glUniformI(String location, IntConsumer callback, int... values) {
		glUniform(location, UniformEntry.IS_INT, UniformEntry.IntUniformEntry.NEW, callback, values);
	}

	public void glUniformMatrix2(String location, boolean transpose, FloatBuffer matrix) {
		glUniformMatrix(location, (loc) -> GL20.glUniformMatrix2fv(loc, transpose, matrix), transpose, matrix);
	}

	public void glUniformMatrix4(String location, boolean transpose, FloatBuffer matrix) {
		glUniformMatrix(location, (loc) -> GL20.glUniformMatrix4fv(loc, transpose, matrix), transpose, matrix);
	}

	public void glUniformMatrix(String location, IntConsumer callback, boolean transpose, FloatBuffer matrix) {
		glUniform(location, UniformEntry.IS_MATRIX, UniformEntry.MatrixUniformEntry.NEW, callback, ImmutablePair.of(matrix, transpose));
	}

	public void glUniformBoolean(String location, boolean value) {
		glUniform(location, UniformEntry.IS_BOOLEAN, UniformEntry.BooleanUniformEntry.NEW, (loc) -> GL20.glUniform1i(loc, value ? 1 : 0), value);
	}

	public void glUniformMatrix4F(String location, Matrix4f matrix4f) {
		glUniform(location, UniformEntry.IS_MATRIX4F, UniformEntry.Matrix4FUniformEntry.NEW, (loc) -> {
			try(MemoryStack stack = MemoryStack.stackPush()) {
				FloatBuffer buffer = stack.mallocFloat(16);
				matrix4f.get(buffer);
				buffer.rewind();
				GL20.glUniformMatrix4fv(loc, false, buffer);
			}
		}, matrix4f);
	}

	private int getUniformLocation(String name) {
		int uniformLocation;
		if (locationCache.containsKey(name)) {
			uniformLocation = locationCache.get(name);
		} else {
			uniformLocation = GL20.glGetUniformLocation(programId, name);
			locationCache.put(name, uniformLocation);
		}
		return uniformLocation;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private <T> void glUniform(String location, Predicate<UniformEntry<?>> isType, Function<T, UniformEntry<T>> createUniform, IntConsumer applyCallback, T value) {
		int loc = getUniformLocation(location);
		if (loc == -1) {
			if (Platform.isDevEnv()) {
//				LDLib.LOGGER.error("can't find uniform with name {}", location);
//				var shaders = new int[2];
//				GL20.glGetAttachedShaders(programId, null, shaders);
//				LDLib.LOGGER.error("attached shader source {}", GL20.glGetShaderSource(shaders[0]));
//				LDLib.LOGGER.error("attached shader source {}", GL20.glGetShaderSource(shaders[1]));
			}
			return;
		}
		boolean update = true;
		if (entryCache.containsKey(loc)) {
			UniformEntry uniformEntry = entryCache.get(loc);
			if (isType.test(uniformEntry)) {
				update = !uniformEntry.check(value);
			}
		}
		if (update) {
			UniformEntry<T> entry = createUniform.apply(value);
			applyCallback.accept(loc);
			entryCache.put(loc, entry);
		}
	}

}
