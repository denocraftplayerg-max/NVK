package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.function.Consumer;

public class VTextInputWidget extends VAbstractWidget {
    boolean selected = false;
    Consumer<VTextInputWidget> onSearch; // when the search is "activated", like pressing enter
    Component message;
    Component placeholder;

    public VTextInputWidget(int x, int y, int width, int height, Component placeholder, Consumer<VTextInputWidget> onSearch) {
        this.setPosition(x, y, width, height);

        this.message = placeholder;
        this.placeholder = placeholder;
        this.onSearch = onSearch;
    }

    public void renderWidget(double mouseX, double mouseY) {
        if (!this.isVisible()) return;
        int backgroundColor = this.isActive()
                ? ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_BLACK, 0.45f)
                : ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_BLACK, 0.3f);

        int textColor = VGuiConstants.COLOR_WHITE;

        if (this.message.getString().equals(this.placeholder.getString())) {
            textColor = VGuiConstants.COLOR_GRAY;
        }

        //noinspection DuplicatedCode
        int selectionOutlineColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, 0.8f);
        int selectionFillColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, 0.2f);

        GuiRenderer.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);

        if (this.selected) {
            GuiRenderer.fill(this.x, this.y, this.x + 2, this.y + this.height, selectionOutlineColor);
            GuiRenderer.fill(this.x, this.y, this.x + this.width, this.y + this.height, selectionFillColor);
        }

        // this is down here because of layering
        GuiRenderer.drawString(
                Minecraft.getInstance().font,
                this.message,
                this.x + 8, (this.y + this.height / 2) - 4,
                textColor | (Mth.ceil(255.0f) << 24));

    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (keyEvent.key() == GLFW.GLFW_KEY_ENTER ||  keyEvent.key() == GLFW.GLFW_KEY_KP_ENTER) {
            this.onSearch.accept(this);
        } else {
            if (this.message.getString().equals(this.placeholder.getString())) {
                this.message = Component.empty();
            }

            if (keyEvent.key() == GLFW.GLFW_KEY_BACKSPACE) {
                String string = this.message.getString();
                if (!string.isEmpty()) {
                    string = string.substring(0, string.length() - 1);
                }
                this.message = Component.literal(string);
            } else {
                if (!keyEvent.hasShiftDown()) {
                    String string = this.message.getString();
                    String name = GLFW.glfwGetKeyName(keyEvent.key(), keyEvent.scancode());
                    if (name == null) {
                        return false;
                    }
                    string += name;
                    this.message = Component.literal(string);
                } else {
                    String string = this.message.getString();
                    String name = Objects.requireNonNull(GLFW.glfwGetKeyName(keyEvent.key(), keyEvent.scancode())).toUpperCase();
                    string += name;
                    this.message = Component.literal(string);
                }
            }

        }
        return true;
    }

    public String getInput() {
        return this.message.getString();
    }

    @SuppressWarnings("unused")
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!this.active || !this.visible)
            return null;
        return super.nextFocusPath(event);
    }
}
