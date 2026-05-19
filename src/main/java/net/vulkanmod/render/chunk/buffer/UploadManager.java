package net.vulkanmod.render.chunk.buffer;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import net.vulkanmod.vulkan.memory.buffer.StagingBuffer;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.queue.TransferQueue;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkMemoryBarrier;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class UploadManager {
    public static UploadManager INSTANCE;

    public static void createInstance() {
        INSTANCE = new UploadManager();
    }

    TransferQueue queue = DeviceManager.getTransferQueue();
    CommandPool.CommandBuffer commandBuffer;

    LongOpenHashSet dstBuffers = new LongOpenHashSet();

    private final List<PendingCopy> pendingCopies = new ArrayList<>();
    private long lastFence;

    public void submitUploads() {
        flush();
    }

    public void recordUpload(Buffer buffer, long dstOffset, long bufferSize, ByteBuffer src) {
        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
        boolean copied = stagingBuffer.copyBuffer((int) bufferSize, src, buffer, dstOffset);

        if (copied) {
            pendingCopies.add(new PendingCopy(buffer, dstOffset, bufferSize, stagingBuffer.getOffset()));
        }
    }

    public boolean hasPendingWork() {
        return !pendingCopies.isEmpty() || this.commandBuffer != null;
    }

    public long flush() {
        if (pendingCopies.isEmpty() && this.commandBuffer == null) {
            return 0L;
        }

        beginCommands();
        VkCommandBuffer vkCmdBuffer = this.commandBuffer.getHandle();

        List<PendingCopy> consolidated = consolidateCopies();

        for (PendingCopy copy : consolidated) {
            if (!this.dstBuffers.add(copy.dstBuffer.getId())) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    VkMemoryBarrier.Buffer barrier = VkMemoryBarrier.calloc(1, stack);
                    barrier.sType$Default();
                    barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                    barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                    vkCmdPipelineBarrier(vkCmdBuffer,
                            VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                            0,
                            barrier,
                            null,
                            null);
                }

                this.dstBuffers.clear();
            }

            StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();
            TransferQueue.uploadBufferCmd(vkCmdBuffer,
                    stagingBuffer.getId(), copy.srcOffset,
                    copy.dstBuffer.getId(), copy.dstOffset,
                    copy.bufferSize);
        }

        long fence = this.queue.submitCommands(this.commandBuffer);

        Synchronization.INSTANCE.addCommandBuffer(this.commandBuffer);

        this.lastFence = fence;
        this.commandBuffer = null;
        this.dstBuffers.clear();
        this.pendingCopies.clear();

        return fence;
    }

    public long getLastFence() {
        return lastFence;
    }

    public void recordUploadFallback(Buffer dst, long dstOffset, long bufferSize, ByteBuffer src) {
        StagingBuffer tempStaging = new StagingBuffer(bufferSize);
        tempStaging.copyBuffer((int) bufferSize, src);

        TransferQueue transferQueue = DeviceManager.getTransferQueue();
        transferQueue.uploadBufferImmediate(
                tempStaging.getId(), 0L,
                dst.getId(), dstOffset,
                bufferSize);

        tempStaging.scheduleFree();
    }

    private List<PendingCopy> consolidateCopies() {
        if (pendingCopies.size() <= 1) {
            return new ArrayList<>(pendingCopies);
        }

        List<PendingCopy> sorted = new ArrayList<>(pendingCopies);
        sorted.sort(Comparator.comparingLong((PendingCopy c) -> c.dstBuffer.getId())
                .thenComparingLong(c -> c.dstOffset));

        List<PendingCopy> merged = new ArrayList<>();
        PendingCopy current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            PendingCopy next = sorted.get(i);

            if (current.dstBuffer == next.dstBuffer
                    && current.dstOffset + current.bufferSize == next.dstOffset
                    && current.srcOffset + current.bufferSize == next.srcOffset) {
                current = new PendingCopy(current.dstBuffer, current.dstOffset,
                        current.bufferSize + next.bufferSize, current.srcOffset);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        return merged;
    }

    public void copyBuffer(Buffer src, Buffer dst) {
        copyBuffer(src, 0, dst, 0, src.getBufferSize());
    }

    public void copyBuffer(Buffer src, long srcOffset, Buffer dst, long dstOffset, long size) {
        if (!this.pendingCopies.isEmpty()) {
            flush();
        }

        beginCommands();

        VkCommandBuffer vkCmdBuffer = this.commandBuffer.getHandle();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkMemoryBarrier.Buffer barrier = VkMemoryBarrier.calloc(1, stack);
            barrier.sType$Default();

            VkBufferMemoryBarrier.Buffer bufferMemoryBarriers = VkBufferMemoryBarrier.calloc(1, stack);
            VkBufferMemoryBarrier bufferMemoryBarrier = bufferMemoryBarriers.get(0);
            bufferMemoryBarrier.sType$Default();
            bufferMemoryBarrier.buffer(src.getId());
            bufferMemoryBarrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            bufferMemoryBarrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
            bufferMemoryBarrier.size(VK_WHOLE_SIZE);

            vkCmdPipelineBarrier(vkCmdBuffer,
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0,
                    barrier,
                    bufferMemoryBarriers,
                    null);
        }

        this.dstBuffers.add(dst.getId());

        TransferQueue.uploadBufferCmd(vkCmdBuffer, src.getId(), srcOffset, dst.getId(), dstOffset, size);
    }

    public void syncUploads() {
        submitUploads();

        Synchronization.INSTANCE.waitFences();
    }

    private void beginCommands() {
        if (this.commandBuffer == null)
            this.commandBuffer = queue.beginCommands();
    }

    record PendingCopy(Buffer dstBuffer, long dstOffset, long bufferSize, long srcOffset) {
    }
}
