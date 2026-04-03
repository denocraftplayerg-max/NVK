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

        // Usar o CB actual do ImageUploadHelper (graphics queue).
        // getCommandBuffer() retorna null se não há CB aberto — isso acontece durante
        // recreateSwapChain() quando submitUploads() é chamado antes do main CB existir.
        //
        // Nesse caso NÃO descartamos as texturas — ficam na lista para serem
        // transicionadas no próximo submitUploads() quando o CB estiver disponível.
        // Descartar silenciosamente deixaria texturas em TRANSFER_DST → textura roxa.
        //
        // Se o CB estiver disponível, gravar as barriers e limpar a lista.
        CommandPool.CommandBuffer cb = ImageUploadHelper.INSTANCE.getCommandBuffer();
        if (cb == null) {
            // Nenhum CB aberto — adiar para o próximo submitUploads().
            // As texturas permanecem na lista e serão transicionadas quando
            // ImageUploadHelper tiver um CB activo.
            return;
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
