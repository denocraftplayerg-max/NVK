package net.vulkanmod.config.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.util.VGuiConstants;
import net.vulkanmod.vulkan.util.ColorUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class VTextInputWidget extends VAbstractWidget {
    boolean selected = false;
    Consumer<VTextInputWidget> onSearch; // when the search is "activated", like pressing enter
    private String text;
    private final Component placeholder;

    public VTextInputWidget(int x, int y, int width, int height, Component placeholder, Consumer<VTextInputWidget> onSearch) {
        this.setPosition(x, y, width, height);

        this.placeholder = placeholder;
        this.onSearch = onSearch;
        this.text = "";
    }

    @Override
    public void renderWidget(double mouseX, double mouseY) {
        if (!this.isVisible()) return;

        boolean hasText = !this.text.isEmpty();

        int backgroundColor = this.focused || this.selected
                ? ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_BLACK, 0.3f)
                : ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_BLACK, 0.45f);

        int textColor = hasText ? VGuiConstants.COLOR_WHITE : VGuiConstants.COLOR_GRAY;

        //noinspection DuplicatedCode
        int selectionOutlineColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, 0.8f);
        int selectionFillColor = ColorUtil.ARGB.multiplyAlpha(VGuiConstants.COLOR_RED, 0.2f);

        GuiRenderer.fill(this.x, this.y, this.x + this.width, this.y + this.height, backgroundColor);

        if (this.selected || this.focused) {
            GuiRenderer.renderBorder(x, y, x + width, y + height, 1, selectionOutlineColor);
            GuiRenderer.fill(this.x, this.y, this.x + this.width, this.y + this.height, selectionFillColor);
        }

        Component displayText = hasText ? Component.literal(this.text) : this.placeholder;

        GuiRenderer.drawString(
                Minecraft.getInstance().font,
                displayText,
                this.x + 8,
                this.y + (this.height - 8) / 2,
                textColor | 0xFF000000
        );
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        if (!this.focused && !this.selected) return false;

        if (keyEvent.key() == GLFW.GLFW_KEY_ENTER || keyEvent.key() == GLFW.GLFW_KEY_KP_ENTER) {
            this.onSearch.accept(this);
            return true;
        }

        if (keyEvent.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (!this.text.isEmpty()) {
                this.text = this.text.substring(0, this.text.length() - 1);
                this.onSearch.accept(this);   // live search
            }
            return true;
        }

        String keyName = GLFW.glfwGetKeyName(keyEvent.key(), keyEvent.scancode());
        if (keyName != null && keyName.length() == 1) {
            if (keyEvent.hasShiftDown()) keyName = keyName.toUpperCase();
            this.text += keyName;
            this.onSearch.accept(this);
            return true;
        }

        return false;
    }

    public String getInput() {
        return this.text;
    }

    public void setInput(String input) {
        this.text = input != null ? input : "";
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (!this.active || !this.visible) return false;

        boolean clicked = this.clicked(event.x(), event.y());
        if (clicked) {
            this.setFocused(true);
            this.selected = true;
            return true;
        } else {
            this.setFocused(false);
            this.selected = false;
            return false;
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.selected = false;
        }
    }
}
