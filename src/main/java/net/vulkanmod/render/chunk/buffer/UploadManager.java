package net.vulkanmod.render.chunk.buffer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import net.vulkanmod.vulkan.memory.buffer.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.Queue;
import net.vulkanmod.vulkan.queue.TransferQueue;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class UploadManager {
    public static UploadManager INSTANCE;

    public static void createInstance() {
        INSTANCE = new UploadManager();
    }

    private final Queue queue = DeviceManager.getTransferQueue();
    private CommandPool.CommandBuffer commandBuffer;

    private final LongOpenHashSet dstBuffers = new LongOpenHashSet();

    // =========================
    // SUBMIT
    // =========================
    public void submitUploads() {
        if (this.commandBuffer == null)
            return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            int transferFamily = DeviceManager.getTransferQueue().getFamilyIndex();
            int graphicsFamily = DeviceManager.getGraphicsQueue().getFamilyIndex();

            if (transferFamily != graphicsFamily && !this.dstBuffers.isEmpty()) {

                long[] bufferIds = this.dstBuffers.toLongArray();

                VkBufferMemoryBarrier.Buffer releaseBarriers =
                        VkBufferMemoryBarrier.calloc(bufferIds.length, stack);

                for (int i = 0; i < bufferIds.length; i++) {
                    releaseBarriers.get(i)
                            .sType$Default()
                            .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                            .dstAccessMask(
                                    VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT |
                                    VK_ACCESS_INDEX_READ_BIT |
                                    VK_ACCESS_INDIRECT_COMMAND_READ_BIT
                            )
                            .srcQueueFamilyIndex(transferFamily)
                            .dstQueueFamilyIndex(graphicsFamily)
                            .buffer(bufferIds[i])
                            .offset(0)
                            .size(VK_WHOLE_SIZE);
                }

                vkCmdPipelineBarrier(
                        this.commandBuffer.getHandle(),
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT | VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
                        0,
                        null,
                        releaseBarriers,
                        null
                );
            }
        }

        this.queue.submitCommands(this.commandBuffer);
        this.queue.waitIdle();

        this.commandBuffer.reset();
        this.commandBuffer = null;
    }

    // =========================
    // UPLOAD
    // =========================
    public void recordUpload(Buffer buffer, long dstOffset, long bufferSize, ByteBuffer src) {
        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();

        stagingBuffer.align((int) Math.min(bufferSize, 4));
        stagingBuffer.copyBuffer((int) bufferSize, src);
        long srcOffset = stagingBuffer.getOffset();

        beginCommands();
        VkCommandBuffer cmd = this.commandBuffer.getHandle();

        if (!this.dstBuffers.add(buffer.getId())) {
            try (MemoryStack stack = MemoryStack.stackPush()) {

                VkMemoryBarrier.Buffer barrier = VkMemoryBarrier.calloc(1, stack);
                barrier.sType$Default();
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(
                        VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT |
                        VK_ACCESS_INDEX_READ_BIT |
                        VK_ACCESS_INDIRECT_COMMAND_READ_BIT
                );

                vkCmdPipelineBarrier(
                        cmd,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT | VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
                        0,
                        barrier,
                        null,
                        null
                );
            }
        }

        TransferQueue.uploadBufferCmd(
                cmd,
                stagingBuffer.getId(), srcOffset,
                buffer.getId(), dstOffset,
                bufferSize
        );
    }

    // =========================
    // COPY BUFFER
    // =========================
    public void copyBuffer(Buffer src, Buffer dst) {
        copyBuffer(src, 0, dst, 0, src.getBufferSize());
    }

    public void copyBuffer(Buffer src, long srcOffset, Buffer dst, long dstOffset, long size) {
        beginCommands();

        VkCommandBuffer cmd = this.commandBuffer.getHandle();

        try (MemoryStack stack = MemoryStack.stackPush()) {

            VkBufferMemoryBarrier.Buffer bufferBarrier =
                    VkBufferMemoryBarrier.calloc(1, stack);

            bufferBarrier.get(0)
                    .sType$Default()
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    .dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                    .buffer(src.getId())
                    .offset(0)
                    .size(VK_WHOLE_SIZE);

            vkCmdPipelineBarrier(
                    cmd,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0,
                    null,
                    bufferBarrier,
                    null
            );
        }

        if (!this.dstBuffers.add(dst.getId())) {
            try (MemoryStack stack = MemoryStack.stackPush()) {

                VkMemoryBarrier.Buffer barrier = VkMemoryBarrier.calloc(1, stack);
                barrier.sType$Default();
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(
                        VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT |
                        VK_ACCESS_INDEX_READ_BIT |
                        VK_ACCESS_INDIRECT_COMMAND_READ_BIT
                );

                vkCmdPipelineBarrier(
                        cmd,
                        VK_PIPELINE_STAGE_TRANSFER_BIT,
                        VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT | VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
                        0,
                        barrier,
                        null,
                        null
                );
            }
        }

        TransferQueue.uploadBufferCmd(
                cmd,
                src.getId(), srcOffset,
                dst.getId(), dstOffset,
                size
        );
    }

    // =========================
    // ACQUIRE (GRAPHICS SIDE)
    // =========================
    public void recordAcquireBarriers(VkCommandBuffer graphicsCmdBuffer) {
        if (this.dstBuffers.isEmpty())
            return;

        int transferFamily = DeviceManager.getTransferQueue().getFamilyIndex();
        int graphicsFamily = DeviceManager.getGraphicsQueue().getFamilyIndex();

        if (transferFamily == graphicsFamily) {
            this.dstBuffers.clear();
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {

            long[] bufferIds = this.dstBuffers.toLongArray();

            VkBufferMemoryBarrier.Buffer acquireBarriers =
                    VkBufferMemoryBarrier.calloc(bufferIds.length, stack);

            for (int i = 0; i < bufferIds.length; i++) {
                acquireBarriers.get(i)
                        .sType$Default()
                        .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                        .dstAccessMask(
                                VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT |
                                VK_ACCESS_INDEX_READ_BIT |
                                VK_ACCESS_INDIRECT_COMMAND_READ_BIT
                        )
                        .srcQueueFamilyIndex(transferFamily)
                        .dstQueueFamilyIndex(graphicsFamily)
                        .buffer(bufferIds[i])
                        .offset(0)
                        .size(VK_WHOLE_SIZE);
            }

            vkCmdPipelineBarrier(
                    graphicsCmdBuffer,
                    VK_PIPELINE_STAGE_TRANSFER_BIT,
                    VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT | VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
                    0,
                    null,
                    acquireBarriers,
                    null
            );
        }

        this.dstBuffers.clear();
    }

    // =========================
    public void syncUploads() {
        submitUploads();
    }

    private void beginCommands() {
        if (this.commandBuffer == null)
            this.commandBuffer = queue.beginCommands();
    }
}
