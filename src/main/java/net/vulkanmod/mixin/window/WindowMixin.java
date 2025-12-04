package net.vulkanmod.mixin.window;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.platform.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.video.VideoModeManager;
import net.vulkanmod.config.option.Options;
import net.vulkanmod.config.video.VideoModeSet;
import net.vulkanmod.config.video.WindowMode;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(Window.class)
public abstract class WindowMixin {
    @Final @Shadow private long handle;

    @Shadow private boolean vsync;
    @Shadow private boolean fullscreen;

    @Shadow @Final private static Logger LOGGER;

    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int width;
    @Shadow private int height;

    @Shadow private int framebufferWidth;
    @Shadow private int framebufferHeight;

    @Shadow public abstract int getWidth();

    @Shadow public abstract int getHeight();

    @Shadow protected abstract void updateFullscreen(boolean bl, @Nullable TracyFrameCapture tracyFrameCapture);

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V"))
    private void redirect(int hint, int value) { }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwCreateWindow(IILjava/lang/CharSequence;JJ)J"))
    private void vulkanHint(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        //Fix Gnome/Wayland Client-Side Decorators
        boolean b = (Platform.isGnome() | Platform.isWeston() | Platform.isGeneric()) && Platform.isWayLand();
        GLFW.glfwWindowHint(GLFW_DECORATED, (b ? GLFW_FALSE : GLFW_TRUE));
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void getHandle(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        VRenderSystem.setWindow(this.handle);
    }

    /**
     * @author
     */
    @Overwrite
    public void updateVsync(boolean vsync) {
        this.vsync = vsync;
        Vulkan.setVsync(vsync);
    }

    /**
     * @author
     */
    @Overwrite
    public void toggleFullScreen() {
        this.fullscreen = !this.fullscreen;
        Options.fullscreenDirty = true;
    }

    /**
     * @author
     */
    @Overwrite
    public void updateDisplay(@Nullable TracyFrameCapture tracyFrameCapture) {
        RenderSystem.flipFrame((Window) ((Object)this), tracyFrameCapture);

        if (Options.fullscreenDirty) {
            Options.fullscreenDirty = false;
            this.updateFullscreen(this.vsync, tracyFrameCapture);
        }
    }

    private boolean wasOnFullscreen = false;

    // --- Helper to get the correct monitor ---
    private long getMonitor() {
        if (this.wasOnFullscreen && this.fullscreen) {
            long m = GLFW.glfwGetWindowMonitor(this.handle);
            if (m != 0L) return m;
        }

        if (Initializer.CONFIG == null) {
            return GLFW.glfwGetPrimaryMonitor();
        }

        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (monitors == null || monitors.limit() == 0) {
            return GLFW.glfwGetPrimaryMonitor();
        }

        int targetIndex = Initializer.CONFIG.targetMonitor;

        if (targetIndex < 0 || targetIndex >= monitors.limit()) {
            targetIndex = 0;
            Initializer.CONFIG.targetMonitor = 0;
        }

        return monitors.get(targetIndex);
    }

    /**
     * @author
     */
    @Overwrite
    private void setMode() {
        Config config = Initializer.CONFIG;
        long monitor = this.getMonitor();

        // Update modes for current monitor
        VideoModeManager.updateMonitor(monitor);

        // --- 1. EXCLUSIVE FULLSCREEN ---
        if (this.fullscreen) {
            VideoModeSet.VideoMode videoMode = (config != null) ? config.videoMode : VideoModeManager.getFirstAvailable().getVideoMode();

            boolean supported;
            VideoModeSet set = VideoModeManager.getFromVideoMode(videoMode);

            if (set != null) {
                supported = set.hasRefreshRate(videoMode.refreshRate);
            }
            else {
                supported = false;
            }

            if(!supported) {
                LOGGER.error("Resolution not supported, using first available as fallback");
                videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
            }

            if (!this.wasOnFullscreen) {
                this.windowedX = this.x;
                this.windowedY = this.y;
                this.windowedWidth = this.width;
                this.windowedHeight = this.height;
            }

            this.x = 0;
            this.y = 0;
            this.width = videoMode.width;
            this.height = videoMode.height;

            GLFW.glfwSetWindowMonitor(this.handle, monitor, this.x, this.y, this.width, this.height, videoMode.refreshRate);

            this.wasOnFullscreen = true;
        }
        // --- 2. WINDOWED FULLSCREEN (BORDERLESS) - ADAPTIVE WAYLAND FIX ---
        else if (config != null && config.windowMode == WindowMode.WINDOWED_FULLSCREEN.mode) {

            // Get monitor position
            int[] monitorX = new int[1];
            int[] monitorY = new int[1];
            GLFW.glfwGetMonitorPos(monitor, monitorX, monitorY);

            if (!this.wasOnFullscreen) {
                this.windowedX = this.x;
                this.windowedY = this.y;
                this.windowedWidth = this.width;
                this.windowedHeight = this.height;
            }

            // Remove window decorations (borders)
            GLFW.glfwSetWindowAttrib(this.handle, GLFW_DECORATED, GLFW_FALSE);

            // Move window to the target monitor at (x, y).
            // We set a dummy size (800x600) initially because we will maximize it immediately.
            // Using 0L as monitor handle means "Windowed mode", which is correct for Borderless.
            GLFW.glfwSetWindowMonitor(this.handle, 0L, monitorX[0], monitorY[0], 800, 600, -1);

            // Force Maximize. This lets the Window Manager (KWin/Wayland) handle the scaling logic.
            // It will fill the logical screen area perfectly, ignoring incorrect GLFW scaling reports.
            GLFW.glfwMaximizeWindow(this.handle);

            // Now read back the Logic Size assigned by the OS
            int[] realWidth = new int[1];
            int[] realHeight = new int[1];
            GLFW.glfwGetWindowSize(this.handle, realWidth, realHeight);

            this.width = realWidth[0];
            this.height = realHeight[0];
            this.x = monitorX[0];
            this.y = monitorY[0];

            LOGGER.info("Adaptive Borderless: System assigned logical size {}x{}", this.width, this.height);

            int[] fbW = new int[1];
            int[] fbH = new int[1];
            GLFW.glfwGetFramebufferSize(this.handle, fbW, fbH);

            this.framebufferWidth = fbW[0];
            this.framebufferHeight = fbH[0];

            LOGGER.info("Adaptive Borderless: Physical framebuffer size {}x{}", this.framebufferWidth, this.framebufferHeight);

            // Force update if framebuffer size changed significantly
            if (this.framebufferWidth > 0 && this.framebufferHeight > 0) {
                Renderer.scheduleSwapChainUpdate();
            }

            this.wasOnFullscreen = true;
        }
        // --- 3. WINDOWED MODE ---
        else {
            this.x = this.windowedX;
            this.y = this.windowedY;
            this.width = this.windowedWidth;
            this.height = this.windowedHeight;

            // Important: If we were maximized/borderless, we must restore the window
            // so it can be resized and decorated again.
            GLFW.glfwRestoreWindow(this.handle);

            GLFW.glfwSetWindowMonitor(this.handle, 0L, this.x, this.y, this.width, this.height, -1);
            GLFW.glfwSetWindowAttrib(this.handle, GLFW_DECORATED, GLFW_TRUE);

            this.wasOnFullscreen = false;
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void onFramebufferResize(long window, int width, int height) {
        if (window == this.handle) {
            if(width > 0 && height > 0) {
                // Update internal framebuffer state
                this.framebufferWidth = width;
                this.framebufferHeight = height;

                // Trigger Vulkan SwapChain rebuild
                Renderer.scheduleSwapChainUpdate();
            }
        }
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    private void onResize(long window, int width, int height) {
        this.width = width;
        this.height = height;

        if(width > 0 && height > 0)
            Renderer.scheduleSwapChainUpdate();
    }
}