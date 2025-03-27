package com.lowdragmc.lowdraglib.integration.jei;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.Rect2i;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author mezz
 * @date 2024/08/11
 * @implNote IClickableIngredient implementation
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ClickableIngredient<T> implements IClickableIngredient<T> {
	private final ITypedIngredient<T> ingredient;
	private final int x;
	private final int y;
	private final int width;
	private final int height;

	public ClickableIngredient(ITypedIngredient<T> ingredient, int x, int y, int width, int height) {
		this.ingredient = ingredient;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	@Override
	public ITypedIngredient<T> getTypedIngredient() {
		return ingredient;
	}

	@Override
	public Rect2i getArea() {
		return new Rect2i(x, y, width, height);
	}
}
