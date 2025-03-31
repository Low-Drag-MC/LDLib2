package com.lowdragmc.lowdraglib.math;

import lombok.Data;

/**
 * a combination of position and size with a series of methods<br>
 */
@Data(staticConstructor = "of")
public final class Rect {
	public static final Rect ZERO = new Rect(0, 0, 0, 0);
	public final int left;
	public final int right;
	public final int up;
	public final int down;

	public static Rect ofAbsolute(int left, int right, int up, int down) {
		return new Rect(left, right, up, down);
	}

	public static Rect ofRelative(int left, int width, int up, int height) {
		return new Rect(left, left + width, up, up + height);
	}

	public static Rect of(Position position, Size size) {
		return new Rect(position.x, position.x + size.width, position.y, position.y + size.height);
	}

	public Position toLeftUp() {
		return Position.of(left, up);
	}

	public Position toLeftCenter() {
		return Position.of(left, (up + down) / 2);
	}

	public Position toLeftDown() {
		return Position.of(left, down);
	}

	public Position toDownCenter() {
		return Position.of((left + right) / 2, down);
	}

	public Position toRightDown() {
		return Position.of(right, down);
	}

	public Position toRightCenter() {
		return Position.of(right, (up + down) / 2);
	}

	public Position toRightUp() {
		return Position.of(right, up);
	}

	public Position toUpCenter() {
		return Position.of((left + right) / 2, up);
	}

	public Position upAnd(int x) {
		return Position.of(x, up);
	}

	public Position rightAnd(int y) {
		return Position.of(right, y);
	}

	public Position downAnd(int x) {
		return Position.of(x, down);
	}

	public Position leftAnd(int y) {
		return Position.of(left, y);
	}

	public Rect expand(int expand) {
		return expand(expand, expand);
	}

	public Rect expand(int x, int y) {
		return new Rect(left - x, right + x, up - y, down + y);
	}

	public Rect horizontalExpand(int x) {
		return expand(x, 0);
	}

	public Rect horizontalExpand(int left, int right) {
		return new Rect(this.left - left, this.right + right, up, down);
	}

	public Rect verticalExpand(int y) {
		return expand(0, y);
	}

	public Rect verticalExpand(int up, int down) {
		return new Rect(left, right, this.up - up, this.down + down);
	}

	public Rect expandLeft(int expand) {
		return new Rect(left - expand, right, up, down);
	}

	public Rect expandRight(int expand) {
		return new Rect(left, right + expand, up, down);
	}

	public Rect expandUp(int expand) {
		return new Rect(left, right, up - expand, down);
	}

	public Rect expandDown(int expand) {
		return new Rect(left, right, up, down + expand);
	}

	public int getWidth() {
		return right - left;
	}

	public int getHeight() {
		return down - up;
	}

	public int getWidthCenter() {
		return (right + left) / 2;
	}

	public int getHeightCenter() {
		return (down + up) / 2;
	}

	public Rect withLeft(int left) {
		return new Rect(left, right, up, down);
	}

	public Rect withRight(int right) {
		return new Rect(left, right, up, down);
	}

	public Rect withUp(int up) {
		return new Rect(left, right, up, down);
	}

	public Rect withDown(int down) {
		return new Rect(left, right, up, down);
	}

	public Rect withLeftFixedWidth(int width) {
		return new Rect(left, left + width, up, down);
	}

	public Rect withRightFixedWidth(int width) {
		return new Rect(right - width, right, up, down);
	}

	public Rect withUpFixedHeight(int height) {
		return new Rect(left, right, up, up + height);
	}

	public Rect withDownFixedHeight(int height) {
		return new Rect(left, right, down - height, down);
	}

	public Rect moveHorizontal(int delta) {
		return new Rect(left + delta, right + delta, up, down);
	}

	public Rect moveVertical(int delta) {
		return new Rect(left, right, up + delta, down + delta);
	}

	public Rect move(int deltaX, int deltaY) {
		return new Rect(left + deltaX, right + deltaX, up + deltaY, down + deltaY);
	}

	public Rect move(Position delta) {
		return new Rect(left + delta.x, right + delta.x, up + delta.y, down + delta.y);
	}

	public Rect move(Size size) {
		return new Rect(left + size.width, right + size.width, up + size.height, down + size.height);
	}

	public boolean isCollide(Rect rect) {
		return left < rect.right && right > rect.left && up < rect.down && down > rect.up;
	}
}
