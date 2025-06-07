package com.lowdragmc.lowdraglib2.client.shader.uniform;

@FunctionalInterface
public interface IUniformCallback {

	void apply(UniformCache cache);

	default IUniformCallback with(IUniformCallback callback) {
		return cache -> {
			apply(cache);
			callback.apply(cache);
		};
	}


}
