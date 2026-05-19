package net.vulkanmod.vulkan.memory.buffer;

import net.vulkanmod.render.chunk.buffer.UploadManager;
import net.vulkanmod.render.chunk.util.Util;
import net.vulkanmod.render.texture.ImageUploadHelper;
import net.vulkanmod.vulkan.Synchronization;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.system.libc.LibCString.nmemcpy;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;

public class StagingBuffer extends Buffer {
    private static final long DEFAULT_SIZE = 64 * 1024 * 1024;

    private final List<FencedRegion> fencedRegions = new ArrayList<>();
    private int flushStartOffset;

    public StagingBuffer() {
        this(DEFAULT_SIZE);
    }

    public StagingBuffer(long size) {
        super("Staging buffer", VK_BUFFER_USAGE_TRANSFER_SRC_BIT, MemoryTypes.HOST_MEM);
        this.createBuffer(size);
    }

    public void copyBuffer(int size, ByteBuffer byteBuffer) {
        this.copyBuffer(size, MemoryUtil.memAddress(byteBuffer));
    }

    public void copyBuffer(int size, long scrPtr) {
        if (size > this.bufferSize) {
            throw new IllegalArgumentException("Upload size is greater than staging buffer size.");
        }

        if (size > getRemaining()) {
            flush();
            reclaimCompleted();
        }

        if (this.usedBytes + size > this.bufferSize) {
            this.usedBytes = 0;
        }

        nmemcpy(this.dataPtr + this.usedBytes, scrPtr, size);

        this.offset = this.usedBytes;
        this.usedBytes += size;
    }

    public boolean copyBuffer(int size, ByteBuffer src, Buffer dstBuffer, long dstOffset) {
        long srcPtr = MemoryUtil.memAddress(src);

        if (size > this.bufferSize) {
            throw new IllegalArgumentException("Upload size is greater than staging buffer size.");
        }

        if (size > getRemaining()) {
            flush();
            reclaimCompleted();

            if (size > getRemaining()) {
                UploadManager.INSTANCE.recordUploadFallback(dstBuffer, dstOffset, size, src);
                return false;
            }
        }

        if (this.usedBytes + size > this.bufferSize) {
            this.usedBytes = 0;
        }

        nmemcpy(this.dataPtr + this.usedBytes, srcPtr, size);

        this.offset = this.usedBytes;
        this.usedBytes += size;
        return true;
    }

    public void align(int alignment) {
        long alignedOffset = Util.align(usedBytes, alignment);

        if (alignedOffset + alignment > this.bufferSize) {
            flush();
            reclaimCompleted();
            alignedOffset = 0;
        }

        this.usedBytes = alignedOffset;
    }

    public void flush() {
        long uploadFence = UploadManager.INSTANCE.flush();
        long imageFence = ImageUploadHelper.INSTANCE.submitCommands();

        int end = (int) this.usedBytes;

        if (end > flushStartOffset) {
            if (uploadFence != 0) {
                fencedRegions.add(new FencedRegion(uploadFence, flushStartOffset, end));
            }
            if (imageFence != 0) {
                fencedRegions.add(new FencedRegion(imageFence, flushStartOffset, end));
            }
        } else if (end < flushStartOffset) {
            if (uploadFence != 0) {
                fencedRegions.add(new FencedRegion(uploadFence, flushStartOffset, (int) this.bufferSize));
                fencedRegions.add(new FencedRegion(uploadFence, 0, end));
            }
            if (imageFence != 0) {
                fencedRegions.add(new FencedRegion(imageFence, flushStartOffset, (int) this.bufferSize));
                fencedRegions.add(new FencedRegion(imageFence, 0, end));
            }
        }

        flushStartOffset = end;
    }

    public void reclaimCompleted() {
        Iterator<FencedRegion> it = fencedRegions.iterator();
        while (it.hasNext()) {
            FencedRegion region = it.next();
            if (Synchronization.checkFenceStatus(region.fence)) {
                it.remove();
            }
        }
    }

    public long getRemaining() {
        long remaining = this.bufferSize - this.usedBytes;

        for (FencedRegion region : fencedRegions) {
            if (region.startOffset <= this.usedBytes && this.usedBytes < region.endOffset) {
                return 0L;
            }
            if (this.usedBytes < region.startOffset && region.startOffset - this.usedBytes < remaining) {
                remaining = region.startOffset - this.usedBytes;
            }
        }

        return remaining;
    }

    void submitUploads() {
        flush();
        reclaimCompleted();
    }

    static class FencedRegion {
        final long fence;
        final int startOffset;
        final int endOffset;

        FencedRegion(long fence, int startOffset, int endOffset) {
            this.fence = fence;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }
}
