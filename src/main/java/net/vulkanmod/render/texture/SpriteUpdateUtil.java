package net.vulkanmod.render.texture;

import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.HashSet;
import java.util.Set;

public abstract class SpriteUpdateUtil {

    private static boolean doUpload = true;
    private static final Set<VulkanImage> transitionedLayouts = new HashSet<>();

    public static void setDoUpload(boolean b) {
        doUpload = b;
    }

    public static boolean doUploadFrame() {
        return doUpload;
    }

    public static void addTransitionedLayout(VulkanImage image) {
        transitionedLayouts.add(image);
    }

    public static void transitionLayouts() {
        if (transitionedLayouts.isEmpty()) {
            return;
        }

        // FIX: usar o main command buffer do Renderer em vez do CB do ImageUploadHelper.
        //
        // O ImageUploadHelper usa uma transfer queue. Gravar vkCmdPipelineBarrier com
        // VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT num transfer CB é INVÁLIDO na spec Vulkan
        // — a transfer queue não suporta fragment shader stages.
        // O Mali crasha com SIGSEGV dentro de libGLES_mali.so ao processar este barrier.
        //
        // O main CB (graphics queue) suporta todos os pipeline stages e é o lugar correcto
        // para transições de layout de texturas que vão ser lidas pelo fragment shader.
        // Este padrão é consistente com o FIX 15 aplicado em VulkanImage.readOnlyLayout().
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            transitionedLayouts.forEach(image -> {
                image.readOnlyLayout(stack, commandBuffer);
            });
        }

        transitionedLayouts.clear();
    }
}
