package net.vulkanmod.vulkan.pass;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.vulkanmod.render.engine.VkGpuDevice;
import net.vulkanmod.render.engine.VkGpuTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;

import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class DefaultMainPass implements MainPass {

    public static DefaultMainPass create() {
        return new DefaultMainPass();
    }

    private final Framebuffer mainFramebuffer;

    private RenderPass mainRenderPass;
    private RenderPass auxRenderPass;

    private GpuTexture[] colorAttachmentTextures;
    private GpuTextureView[] colorAttachmentTextureViews;
    private GpuTexture depthAttachmentTexture;

    DefaultMainPass() {
        this.mainFramebuffer = Renderer.getInstance().getSwapChain();

        createRenderPasses();
        createAttachmentTextures();
    }

    private void createRenderPasses() {
        RenderPass.Builder builder = RenderPass.builder(this.mainFramebuffer);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);

        this.mainRenderPass = builder.build();

        // Create an auxiliary RenderPass needed in case of main target rebinding
        builder = RenderPass.builder(this.mainFramebuffer);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        this.auxRenderPass = builder.build();
    }

    @Override
    public void begin(VkCommandBuffer commandBuffer, MemoryStack stack) {
        SwapChain framebuffer = Renderer.getInstance().getSwapChain();

        // ACQUIRE: ownership transfer geometry transfer→graphics
        net.vulkanmod.render.chunk.buffer.UploadManager.INSTANCE
            .recordAcquireBarriers(commandBuffer);

        // Pre-transition bound textures to SHADER_READ_ONLY_OPTIMAL BEFORE the render
        // pass begins. Image layout transitions (VkImageMemoryBarrier) are INVALID
        // inside an active render pass per Vulkan spec. On Mali this causes silent
        // corruption → purple/invisible textures.
        //
        // Only transition from layouts where the image is known to be idle.
        // TRANSFER_DST/SRC means an upload is still in flight — skip those,
        // the upload pipeline will handle their transition itself.
        try (MemoryStack transStack = MemoryStack.stackPush()) {
            for (int i = 0; i < VTextureSelector.SIZE; i++) {
                VulkanImage tex = VTextureSelector.getImage(i);
                if (tex == null)
                    continue;

                int layout = tex.getCurrentLayout();

                // Already correct — nothing to do
                if (layout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    continue;

                // Only transition from safe/idle layouts.
                // Unknown or mid-operation layouts are left alone.
                switch (layout) {
                    case VK_IMAGE_LAYOUT_UNDEFINED:
                    case VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL:
                    case VK_IMAGE_LAYOUT_PRESENT_SRC_KHR:
                        tex.readOnlyLayout(transStack, commandBuffer);
                        break;
                    default:
                        // Skip — texture mid-operation or unknown layout
                        break;
                }
            }
        }

        Renderer.getInstance().beginRenderPass(this.mainRenderPass, framebuffer);
    }

    @Override
    public void end(VkCommandBuffer commandBuffer) {
        Renderer.getInstance().endRenderPass(commandBuffer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            SwapChain framebuffer = Renderer.getInstance().getSwapChain();
            framebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
        }

        int result = vkEndCommandBuffer(commandBuffer);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer:" + result);
        }
    }

    @Override
    public void cleanUp() {
        this.mainRenderPass.cleanUp();
        this.auxRenderPass.cleanUp();
        // NOTE: colorAttachmentTextures, colorAttachmentTextureViews and depthAttachmentTexture
        // are wrappers over swapchain images owned by SwapChain — do NOT call close()/free()
        // on them here. The swapchain manages their lifecycle. Calling doFree() on them
        // would attempt vkDestroyImageView on handles already destroyed by the swapchain → SIGSEGV.
    }

    @Override
    public void onResize() {
        // NOTE: do NOT close/free the old wrappers here.
        // They wrap swapchain images managed by SwapChain — calling close() would
        // attempt vkDestroyImageView on handles the swapchain already owns and will
        // destroy itself → double-free → SIGSEGV in libGLES_mali.so.
        // Just recreate the wrappers; the old ones will be GC'd harmlessly.
        this.createAttachmentTextures();
    }

    public void rebindMainTarget() {
        SwapChain swapChain = Renderer.getInstance().getSwapChain();
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        // Do not rebind if the framebuffer is already bound
        RenderPass boundRenderPass = Renderer.getInstance().getBoundRenderPass();
        if (boundRenderPass == this.mainRenderPass || boundRenderPass == this.auxRenderPass)
            return;

        Renderer.getInstance().endRenderPass(commandBuffer);
        Renderer.getInstance().beginRenderPass(this.auxRenderPass, swapChain);
    }

    @Override
    public void bindAsTexture() {
        SwapChain swapChain = Renderer.getInstance().getSwapChain();
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        // Check if render pass is using the framebuffer
        RenderPass boundRenderPass = Renderer.getInstance().getBoundRenderPass();
        if (boundRenderPass == this.mainRenderPass || boundRenderPass == this.auxRenderPass)
            Renderer.getInstance().endRenderPass(commandBuffer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }

        VTextureSelector.bindTexture(swapChain.getColorAttachment());
    }

    @Override
    public GpuTexture getColorAttachment() {
        return this.colorAttachmentTextures[Renderer.getCurrentImage()];
    }

    @Override
    public GpuTextureView getColorAttachmentView() {
        return this.colorAttachmentTextureViews[Renderer.getCurrentImage()];
    }

    @Override
    public GpuTexture getDepthAttachment() {
        return this.depthAttachmentTexture;
    }

    private void createAttachmentTextures() {
        VkGpuDevice device = (VkGpuDevice) RenderSystem.getDevice();

        SwapChain swapChain = Renderer.getInstance().getSwapChain();
        var swapChainImages = swapChain.getImages();

        if (swapChain.getWidth() == 0 && swapChain.getHeight() == 0)
            return;

        int imageCount = swapChainImages.size();
        this.colorAttachmentTextures = new GpuTexture[imageCount];
        this.colorAttachmentTextureViews = new GpuTextureView[imageCount];

        for (int i = 0; i < imageCount; ++i) {
            VkGpuTexture attachmentTexture = device.gpuTextureFromVulkanImage(swapChainImages.get(i));
            GpuTextureView attachmentTextureView = device.createTextureView(attachmentTexture);
            this.colorAttachmentTextures[i] = attachmentTexture;
            this.colorAttachmentTextureViews[i] = attachmentTextureView;
        }

        this.depthAttachmentTexture = device.gpuTextureFromVulkanImage(swapChain.getDepthAttachment());
    }
            }
