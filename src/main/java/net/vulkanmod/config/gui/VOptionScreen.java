package net.vulkanmod.config.gui;

import com.google.common.collect.Lists;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.gui.render.GuiRenderer;
import net.vulkanmod.config.gui.widget.VAbstractWidget;
import net.vulkanmod.config.gui.widget.VButtonWidget;
import net.vulkanmod.config.option.OptionPage;
import net.vulkanmod.config.option.Options;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public class VOptionScreen extends Screen {
    final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("vulkanmod", "vlogo_transparent.png");

    private final Screen parent;

    private final List<OptionPage> optionPages;

    private int currentListIdx = 0;

    private int tooltipWidth;

    private VButtonWidget supportButton;

    private VButtonWidget doneButton;
    private VButtonWidget applyButton;
    private VButtonWidget undoButton;

    private final List<VButtonWidget> pageButtons = Lists.newArrayList();
    private final List<VButtonWidget> buttons = Lists.newArrayList();


    public VOptionScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;

        this.optionPages = new ArrayList<>();
    }

    private void addPages() {
        this.optionPages.clear();

        OptionPage page = new OptionPage(
                Component.translatable("vulkanmod.options.pages.video").getString(),
                Options.getVideoOpts()
        );
        this.optionPages.add(page);

        page = new OptionPage(
                Component.translatable("vulkanmod.options.pages.graphics").getString(),
                Options.getGraphicsOpts()
        );
        this.optionPages.add(page);

        page = new OptionPage(
                Component.translatable("vulkanmod.options.pages.optimizations").getString(),
                Options.getOptimizationOpts()
        );
        this.optionPages.add(page);

        page = new OptionPage(
                Component.translatable("vulkanmod.options.pages.other").getString(),
                Options.getOtherOpts()
        );
        this.optionPages.add(page);
    }

    @Override
    protected void init() {
        this.addPages();
        this.captureOriginalState();

        int top = 40;
        int bottom = 60;
        int itemHeight = 20;

        int leftMargin = 100;
        int rightMargin = 20;
        int listWidth = this.width - rightMargin - leftMargin;
        int listHeight = this.height - top - bottom;

        this.buildLists(leftMargin, top, listWidth, listHeight, itemHeight);

        int x = leftMargin + listWidth + 10;
        int width = this.width - x - 10;

        if (width < 200) {
            width = listWidth;
        }

        this.tooltipWidth = width;

        buildPage();

        this.applyButton.active = false;
        this.undoButton.visible = false;
    }

    private void captureOriginalState() {
        for (OptionPage page : this.optionPages) {
            page.captureOriginalState();
        }
    }

    private void undo() {
        for (OptionPage page : this.optionPages) {
            page.resetToOriginalState();
        }

        buildPage();
    }

    private void buildLists(int left, int top, int listWidth, int listHeight, int itemHeight) {
        for (OptionPage page : this.optionPages) {
            page.createList(left, top, listWidth, listHeight, itemHeight);
        }
    }

    private void addPageButtons(int x0, int y0, int width, int height) {
        int x = x0;
        int y = y0;
        for (int i = 0; i < this.optionPages.size(); ++i) {
            var page = this.optionPages.get(i);
            final int finalIdx = i;
            VButtonWidget widget = new VButtonWidget(x, y, width, height, Component.nullToEmpty(page.name), button -> this.setOptionList(finalIdx));
            this.buttons.add(widget);
            this.pageButtons.add(widget);
            this.addWidget(widget);

            y += height;
        }

        this.pageButtons.get(this.currentListIdx).setSelected(true);
    }

    private void buildPage() {
        this.buttons.clear();
        this.pageButtons.clear();
        this.clearWidgets();

        this.addPageButtons(10, 40, 80, VGuiConstants.WIDGET_HEIGHT);

        VOptionList currentList = this.optionPages.get(this.currentListIdx).getOptionList();
        this.addWidget(currentList);

        this.addButtons();
    }

    private void addButtons() {
        int rightMargin = 20;
        int padding = 10;
        int buttonWidth = minecraft.font.width(CommonComponents.GUI_DONE) + 2 * padding;
        int x0 = (this.width - buttonWidth - rightMargin);
        int y0 = this.height - VGuiConstants.WIDGET_HEIGHT - 7;

        this.doneButton = new VButtonWidget(
                x0, y0,
                buttonWidth, VGuiConstants.WIDGET_HEIGHT,
                CommonComponents.GUI_DONE,
                button -> this.minecraft.setScreen(this.parent)
        );

        buttonWidth = minecraft.font.width(Component.translatable("vulkanmod.options.buttons.apply")) + 2 * padding;
        x0 -= (buttonWidth + VGuiConstants.WIDGET_MARGIN);
        this.applyButton = new VButtonWidget(
                x0, y0,
                buttonWidth, VGuiConstants.WIDGET_HEIGHT,
                Component.translatable("vulkanmod.options.buttons.apply"),
                button -> this.applyOptions()
        );

        buttonWidth = minecraft.font.width(Component.translatable("vulkanmod.options.buttons.undo")) + 2 * padding;
        x0 -= (buttonWidth + VGuiConstants.WIDGET_MARGIN);
        this.undoButton = new VButtonWidget(
                x0, y0,
                buttonWidth, VGuiConstants.WIDGET_HEIGHT,
                Component.translatable("vulkanmod.options.buttons.undo"),
                button -> {
                    undo();
                }
        );

        buttonWidth = minecraft.font.width(Component.translatable("vulkanmod.options.buttons.kofi")) + 10;
        x0 = (this.width - buttonWidth - rightMargin);
        this.supportButton = new VButtonWidget(
                x0, 6,
                buttonWidth, VGuiConstants.WIDGET_HEIGHT,
                Component.translatable("vulkanmod.options.buttons.kofi"),
                button -> Util.getPlatform().openUri("https://ko-fi.com/xcollateral")
        );

        this.buttons.add(this.applyButton);
        this.buttons.add(this.doneButton);
        this.buttons.add(this.supportButton);
        this.buttons.add(this.undoButton);

        this.addWidget(this.applyButton);
        this.addWidget(this.doneButton);
        this.addWidget(this.supportButton);
        this.addWidget(this.undoButton);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        for (GuiEventListener element : this.children()) {
            if (element.mouseClicked(event, bl)) {
                this.setFocused(element);
                if (event.button() == 0) {
                    this.setDragging(true);
                }

                this.updateState();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.setDragging(false);
        this.updateState();
        return this.getChildAt(event.x(), event.y())
                .filter(guiEventListener -> guiEventListener.mouseReleased(event))
                .isPresent();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        GuiRenderer.guiGraphics = guiGraphics;
        VRenderSystem.enableBlend();

        int size = minecraft.font.lineHeight * 4;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ICON, 30, 4, 0f, 0f, size, size, size, size);

        VOptionList currentList = this.optionPages.get(this.currentListIdx).getOptionList();
        currentList.updateState(mouseX, mouseY);
        currentList.renderWidget(mouseX, mouseY);
        renderButtons(mouseX, mouseY);

        VAbstractWidget hoveredWidget = null;

        for (var b : buttons) {
            if (b.isMouseOver(mouseX, mouseY)) {
                hoveredWidget = b;
                break;
            }
        }

        if (hoveredWidget == null) {
            hoveredWidget = currentList.getHoveredWidget(mouseX, mouseY);
        }

        if (hoveredWidget != null) {
            List<FormattedCharSequence> tooltip = getWidgetTooltip(hoveredWidget);
            if (tooltip != null) {
                int padding = 3;
                int tooltipWidth = GuiRenderer.getMaxTextWidth(this.font, tooltip);
                int tooltipX = hoveredWidget.getX() + hoveredWidget.getWidth() - tooltipWidth - padding;
                int tooltipY = hoveredWidget.getY() + hoveredWidget.getHeight() + 3 + 1; // 3 is the padding inside of renderTooltip and 1 is the custom padding
                this.renderTooltip(tooltip, tooltipX, tooltipY);
            }
        }
    }

    public void renderButtons(int mouseX, int mouseY) {
        for (VButtonWidget button : buttons) {
            button.updateState(mouseX, mouseY);
            button.render(mouseX, mouseY);
        }
    }

    private void renderTooltip(List<FormattedCharSequence> list, int x, int y) {
        if (list.isEmpty()) return;
        int padding = 3;
        int width = GuiRenderer.getMaxTextWidth(this.font, list);
        int height = list.size() * 10;
        GuiRenderer.fill(x - padding, y - padding, x + width + padding, y + height + padding,
                ColorUtil.ARGB.pack(0.05f, 0.05f, 0.05f, 0.6f));

        GuiRenderer.renderBorder(x - padding, y - padding, x + width + padding, y + height + padding, 1, VGuiConstants.COLOR_RED);

        int yOffset = 0;
        for (var text : list) {
            GuiRenderer.drawString(this.font, text, x, y + yOffset, 0xffffffff);
            yOffset += 10;
        }
    }

    private List<FormattedCharSequence> getWidgetTooltip(VAbstractWidget widget) {
        var tooltip = widget.getTooltip();
        if (tooltip == null)
            return null;

        return this.font.split(tooltip, this.tooltipWidth);
    }

    private void updateState() {
        boolean modified = false;
        for (var page : this.optionPages) {
            modified |= page.optionChanged();
        }

        this.applyButton.active = modified;
        this.undoButton.visible = modified;
    }

    private void setOptionList(int i) {
        this.currentListIdx = i;

        this.buildPage();

        this.pageButtons.get(i).setSelected(true);
    }

    private void applyOptions() {
        List<OptionPage> pages = List.copyOf(this.optionPages);
        for (var page : pages) {
            page.applyOptionChanges();
        }

        Initializer.CONFIG.write();
    }
}