package net.vulkanmod.config.gui.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GuiRenderer {

    public static Minecraft minecraft;
    public static GuiGraphics guiGraphics;
    public static PoseStack pose;
    public static BufferBuilder bufferBuilder;

    // Scrolling text state management
    private static final Map<String, ScrollingTextState> scrollingTextStates = new HashMap<>();
    private static final float SCROLL_SPEED = 30.0f; // pixels per second
    private static final float SCROLL_PAUSE_DURATION = 1.0f; // seconds to pause at ends

    private static class ScrollingTextState {
        float scrollOffset = 0.0f;
        long lastUpdateTime = System.currentTimeMillis();
        boolean scrollingForward = true;
        float pauseTimer = 0.0f;

        void update(float textWidth, float maxWidth) {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
            lastUpdateTime = currentTime;

            if (textWidth <= maxWidth) {
                scrollOffset = 0.0f;
                return;
            }

            // Handle pause at ends
            if (pauseTimer > 0) {
                pauseTimer -= deltaTime;
                return;
            }

            float maxScroll = textWidth - maxWidth;

            if (scrollingForward) {
                scrollOffset += SCROLL_SPEED * deltaTime;
                if (scrollOffset >= maxScroll) {
                    scrollOffset = maxScroll;
                    scrollingForward = false;
                    pauseTimer = SCROLL_PAUSE_DURATION;
                }
            } else {
                scrollOffset -= SCROLL_SPEED * deltaTime;
                if (scrollOffset <= 0) {
                    scrollOffset = 0;
                    scrollingForward = true;
                    pauseTimer = SCROLL_PAUSE_DURATION;
                }
            }
        }

        void reset() {
            scrollOffset = 0.0f;
            scrollingForward = true;
            pauseTimer = 0.0f;
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    public static void enableScissor(int i, int j, int k, int l) {
        guiGraphics.enableScissor(i, j, k, l);
    }

    public static void disableScissor() {
        guiGraphics.disableScissor();
    }

    public static void fillBox(int x0, int y0, int width, int height, int color) {
        fill(x0, y0, x0 + width, y0 + height, 0, color);
    }

    public static void fill(int x0, int y0, int x1, int y1, int color) {
        fill(x0, y0, x1, y1, 0, color);
    }

    public static void fill(int x0, int y0, int x1, int y1, int z, int color) {
        guiGraphics.fill(x0, y0, x1, y1, color);
    }

    public static void fillGradient(int x0, int y0, int x1, int y1, int color1, int color2) {
        fillGradient(x0, y0, x1, y1, 0, color1, color2);
    }

    public static void fillGradient(int x0, int y0, int x1, int y1, int z, int color1, int color2) {
        guiGraphics.fillGradient(x0, y0, x1, y1, color1, color2);
    }

    public static void renderBoxBorder(int x0, int y0, int width, int height, int borderWidth, int color) {
        renderBorder(x0, y0, x0 + width, y0 + height, borderWidth, color);
    }

    public static void renderBorder(int x0, int y0, int x1, int y1, int width, int color) {
        GuiRenderer.fill(x0, y0, x1, y0 + width, color);
        GuiRenderer.fill(x0, y1 - width, x1, y1, color);

        GuiRenderer.fill(x0, y0 + width, x0 + width, y1 - width, color);
        GuiRenderer.fill(x1 - width, y0 + width, x1, y1 - width, color);
    }

    public static void drawString(Font font, Component component, int x, int y, int color) {
        drawString(font, component.getVisualOrderText(), x, y, color);
    }

    public static void drawString(Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color) {
        guiGraphics.drawString(font, formattedCharSequence, x, y, color);
    }

    public static void drawString(Font font, Component component, int x, int y, int color, boolean shadow) {
        drawString(font, component.getVisualOrderText(), x, y, color, shadow);
    }

    public static void drawString(Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color, boolean shadow) {
        guiGraphics.drawString(font, formattedCharSequence, x, y, color, shadow);
    }

    public static void drawCenteredString(Font font, Component component, int x, int y, int color) {
        FormattedCharSequence formattedCharSequence = component.getVisualOrderText();
        guiGraphics.drawString(font, formattedCharSequence, x - font.width(formattedCharSequence) / 2, y, color);
    }

    public static void drawScrollingString(Font font, Component component, int x, int y, int maxWidth, int color) {
        int textWidth = font.width(component);
        if (textWidth <= maxWidth) {
            drawCenteredString(font, component, x, y, color);
        } else {
            int x0 = x - maxWidth / 2, x1 = x + maxWidth / 2;
            int scrollAmount = textWidth - maxWidth;
            double currentTimeInSeconds = (double) Util.getMillis() / 1000.0;
            double scrollSpeed = Math.max(scrollAmount * 0.5, 3.0);
            double scrollingOffset = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * currentTimeInSeconds / scrollSpeed)) / 2.0 + 0.5;
            double horizontalScroll = Mth.lerp(scrollingOffset, 0.0, scrollAmount);

            enableScissor(x0 - 1, 0, x1, Minecraft.getInstance().getWindow().getScreenHeight());
            drawString(font, component, (int) (x0 - horizontalScroll), y, color);
            disableScissor();
        }
    }

    public static int getMaxTextWidth(Font font, List<FormattedCharSequence> list) {
        int maxWidth = 0;
        for (var text : list) {
            int width = font.width(text);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth;
    }

    public static void submitPolygon(RenderPipeline renderPipeline, TextureSetup textureSetup, float[][] vertices, int color) {
        guiGraphics.guiRenderState.submitGuiElement(
                new PolygonRenderState(
                        renderPipeline, textureSetup, new Matrix3x2f(), vertices, color, guiGraphics.scissorStack.peek()
                )
        );
    }
}