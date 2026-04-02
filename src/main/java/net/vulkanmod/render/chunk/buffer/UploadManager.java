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
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkMemoryBarrier;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class UploadManager {
    public static UploadManager INSTANCE;

    public static void createInstance() {
        INSTANCE = new UploadManager();
    }

    Queue queue = DeviceManager.getTransferQueue();
    CommandPool.CommandBuffer commandBuffer;

    LongOpenHashSet dstBuffers = new LongOpenHashSet();

    public void submitUploads() {
    if (this.commandBuffer == null)
        return;

    // RELEASE: ceder ownership dos buffers à graphics queue
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
                    .dstAccessMask(0)
                    .srcQueueFamilyIndex(transferFamily)
                    .dstQueueFamilyIndex(graphicsFamily)
                    .buffer(bufferIds[i])
                    .offset(0)
                    .size(VK_WHOLE_SIZE);
            }

            vkCmdPipelineBarrier(
                this.commandBuffer.getHandle(),
                VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
                0, null, releaseBarriers, null);
        }
    }

    this.queue.submitCommands(this.commandBuffer);
    this.queue.waitIdle();
    this.commandBuffer.reset();
    this.commandBuffer = null;
    // dstBuffers NÃO limpar — acquire usa-os
    }

    public void recordUpload(Buffer buffer, long dstOffset, long bufferSize, ByteBuffer src) {
        StagingBuffer stagingBuffer = Vulkan.getStagingBuffer();

        // Fix #1: use used bytes to avoid offset reset bugs
        long srcOffset = stagingBuffer.getUsedBytes();
        stagingBuffer.copyBuffer((int) bufferSize, src);

        beginCommands();
        VkCommandBuffer commandBuffer = this.commandBuffer.getHandle();

        if (!this.dstBuffers.add(buffer.getId())) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkMemoryBarrier.Buffer barrier = VkMemoryBarrier.calloc(1, stack);
                barrier.sType$Default();
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0,
                        barrier,
                        null,
                        null);
            }

            // Fix #2: only remove/re-add this specific buffer, not clear all tracking
            this.dstBuffers.remove(buffer.getId());
            this.dstBuffers.add(buffer.getId());
        }

        TransferQueue.uploadBufferCmd(commandBuffer, stagingBuffer.getId(), srcOffset, buffer.getId(), dstOffset, bufferSize);
    }

    public void copyBuffer(Buffer src, Buffer dst) {
        copyBuffer(src, 0, dst, 0, src.getBufferSize());
    }

    public void copyBuffer(Buffer src, long srcOffset, Buffer dst, long dstOffset, long size) {
        beginCommands();

        VkCommandBuffer commandBuffer = this.commandBuffer.getHandle();

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

            vkCmdPipelineBarrier(commandBuffer,
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                    0,
                    barrier,
                    bufferMemoryBarriers,
                    null);
        }

        // Fix #3: guard dst the same way recordUpload does — emit barrier if already tracked
        if (!this.dstBuffers.add(dst.getId())) {
            try (MemoryStack stack2 = MemoryStack.stackPush()) {
                VkMemoryBarrier.Buffer barrier2 = VkMemoryBarrier.calloc(1, stack2);
                barrier2.sType$Default();
                barrier2.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier2.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT,
                        0,
                        barrier2,
                        null,
                        null);
            }

            this.dstBuffers.remove(dst.getId());
            this.dstBuffers.add(dst.getId());
        }

        TransferQueue.uploadBufferCmd(commandBuffer, src.getId(), srcOffset, dst.getId(), dstOffset, size);
    }

    public void recordAcquireBarriers(VkCommandBuffer graphicsCmdBuffer) {
    if (this.dstBuffers.isEmpty())
        return;

    int transferFamily = DeviceManager.getTransferQueue().getFamilyIndex();
    int graphicsFamily = DeviceManager.getGraphicsQueue().getFamilyIndex();

    if (transferFamily == graphicsFamily) {
        this.dstBuffers.clear();
        return; // mesma família — ownership transfer não necessário
    }

    try (MemoryStack stack = MemoryStack.stackPush()) {
        long[] bufferIds = this.dstBuffers.toLongArray();
        VkBufferMemoryBarrier.Buffer acquireBarriers =
            VkBufferMemoryBarrier.calloc(bufferIds.length, stack);

        for (int i = 0; i < bufferIds.length; i++) {
            acquireBarriers.get(i)
                .sType$Default()
                .srcAccessMask(0)
                .dstAccessMask(VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT)
                .srcQueueFamilyIndex(transferFamily)
                .dstQueueFamilyIndex(graphicsFamily)
                .buffer(bufferIds[i])
                .offset(0)
                .size(VK_WHOLE_SIZE);
        }

        vkCmdPipelineBarrier(
            graphicsCmdBuffer,
            VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT,
            VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
            0, null, acquireBarriers, null);
    }

    this.dstBuffers.clear();
    }

    public void syncUploads() {
        submitUploads();
    }

    private void beginCommands() {
        if (this.commandBuffer == null)
            this.commandBuffer = queue.beginCommands();
    }

                }
