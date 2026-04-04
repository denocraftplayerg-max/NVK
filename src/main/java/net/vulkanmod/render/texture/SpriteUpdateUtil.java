package net.vulkanmod.render.texture;

import net.vulkanmod.vulkan.queue.CommandPool;
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

    static int frameCounter = 0;  // ✅ Adicione esta linha
    
    CommandPool.CommandBuffer cb = ImageUploadHelper.INSTANCE.getCommandBuffer();
    if (cb == null) {
        // ✅ FIX: Timeout 3 frames
        if (++frameCounter > 3) {
            transitionedLayouts.clear();
            frameCounter = 0;
        }
        return;
    }

    VkCommandBuffer commandBuffer = cb.handle;
    try (MemoryStack stack = MemoryStack.stackPush()) {
        transitionedLayouts.forEach(image -> {
            image.readOnlyLayout(stack, commandBuffer);
        });
    }
    transitionedLayouts.clear();
    frameCounter = 0;  // Reset
     }
        VkCommandBuffer commandBuffer = cb.handle;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            transitionedLayouts.forEach(image -> {
                image.readOnlyLayout(stack, commandBuffer);
            });
        }

        transitionedLayouts.clear();
    }
}
