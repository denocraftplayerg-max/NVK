package net.vulkanmod.config.gui.util;

import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.function.BiFunction;

public record Dimension<T extends Number>(T x, T y, T width, T height, BiFunction<T, T, T> adder,
                                          BiFunction<T, Integer, T> divider) {
    public Dimension {
        if (adder == null || divider == null) {
            throw new IllegalArgumentException("Adder and divider functions must not be null.");
        }
    }

    public static Dimension<Integer> ofInt(int x, int y, int width, int height) {
        return new Dimension<>(x, y, width, height, Integer::sum, (a, b) -> a / b);
    }

    public static Dimension<Float> ofFloat(float x, float y, float width, float height) {
        return new Dimension<>(x, y, width, height, Float::sum, (a, b) -> a / b);
    }

    public T xLimit() {
        return adder.apply(x, width);
    }

    public T yLimit() {
        return adder.apply(y, height);
    }

    public T centerX() {
        return divider.apply(adder.apply(x, xLimit()), 2);
    }

    public T centerY() {
        return divider.apply(adder.apply(y, yLimit()), 2);
    }

    public Dimension<T> withX(T newX) {
        return new Dimension<>(newX, y, width, height, adder, divider);
    }

    public Dimension<T> withY(T newY) {
        return new Dimension<>(x, newY, width, height, adder, divider);
    }

    public Dimension<T> withWidth(T newWidth) {
        return new Dimension<>(x, y, newWidth, height, adder, divider);
    }

    public Dimension<T> withHeight(T newHeight) {
        return new Dimension<>(x, y, width, newHeight, adder, divider);
    }

    public ScreenRectangle rectangle() {
        return new ScreenRectangle(x.intValue(), y.intValue(), width.intValue(), height.intValue());
    }

    public boolean isPointInside(double px, double py) {
        return px >= x.doubleValue() && px < xLimit().doubleValue() && py >= y.doubleValue() && py < yLimit().doubleValue();
    }
}
