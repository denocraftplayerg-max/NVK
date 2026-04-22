package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.vulkan.util.ColorUtil;

public class ModIconWidget extends VAbstractWidget {
    final FormattedText name;
    final ResourceLocation icon;

    public ModIconWidget(FormattedText name, ResourceLocation icon, int x0, int y0, int width, int height) {
        this.name = name;
        this.icon = icon;
        this.x = x0;
        this.y = y0;
        this.width = width;
        this.height = height;
    }

    public void render(double mX, double mY) {
        int iconBackgroundColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_BLACK, 0.6f);
        int iconBackgroundWidth = this.width;
        int iconBackgroundHeight = this.height;
        GuiRenderer.fill(this.x, this.y, this.x + iconBackgroundWidth, this.y + iconBackgroundHeight, iconBackgroundColor);


        int size = this.height;
        int iconX = this.x;
        int iconY = this.y + (iconBackgroundHeight - size) / 2;
        GuiRenderer.guiGraphics.blit(RenderPipelines.GUI_TEXTURED, icon, iconX, iconY, 0f, 0f, size, size, size, size);

        GuiRenderer.drawString(Minecraft.getInstance().font, (Component) this.name, this.x + size, iconY + this.height / 2 - 4, 0xffffffff);
    }
}
