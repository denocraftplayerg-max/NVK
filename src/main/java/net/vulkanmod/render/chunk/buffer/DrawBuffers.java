    public void buildDrawBatchesIndirect(Vec3 cameraPos, IndirectBuffer indirectBuffer, StaticQueue<RenderSection> queue, TerrainRenderType terrainRenderType) {
        long bufferPtr = cmdBufferPtr;
        boolean isTranslucent = terrainRenderType == TerrainRenderType.TRANSLUCENT;
        boolean backFaceCulling = Initializer.CONFIG.backFaceCulling && !isTranslucent;
        int drawCount = 0;
        long drawParamsBasePtr = this.drawParamsPtr + (terrainRenderType.ordinal() * DrawParametersBuffer.SECTIONS * DrawParametersBuffer.FACINGS) * DrawParametersBuffer.STRIDE;
        final long facingsStride = DrawParametersBuffer.FACINGS * DrawParametersBuffer.STRIDE;
        int count = 0;

        if (backFaceCulling) {
            for (var iterator = queue.iterator(isTranslucent); iterator.hasNext(); ) {
                final RenderSection section = iterator.next();
                sectionIndices[count] = section.inAreaIndex;
                masks[count] = getMask(cameraPos, section);
                count++;
            }
            long ptr = bufferPtr;
            for (int j = 0; j < count; ++j) {
                final int sectionIdx = sectionIndices[j];
                int mask = masks[j];
                long drawParamsBasePtr2 = drawParamsBasePtr + (sectionIdx * facingsStride);
                int indexCount = 0;
                int firstIndex = 0;
                int vertexOffset = 0;
                int baseInstance = 0;

                for (int i = 0; i < QuadFacing.COUNT; i++) {
                    if ((mask & 1 << i) == 0) {
                        drawParamsBasePtr2 += DrawParametersBuffer.STRIDE;
                        if (indexCount > 0) {
                            MemoryUtil.memPutInt(ptr, indexCount);
                            MemoryUtil.memPutInt(ptr + 4, 1);
                            MemoryUtil.memPutInt(ptr + 8, firstIndex);
                            MemoryUtil.memPutInt(ptr + 12, vertexOffset);
                            MemoryUtil.memPutInt(ptr + 16, baseInstance);
                            // CORREÇÃO: Preenchimento de 12 bytes (3 ints) para alinhar ao CMD_STRIDE (32 bytes)
                            MemoryUtil.memPutInt(ptr + 20, 0);
                            MemoryUtil.memPutInt(ptr + 24, 0);
                            MemoryUtil.memPutInt(ptr + 28, 0);
                            ptr += CMD_STRIDE;
                            drawCount++;
                        }
                        indexCount = 0; firstIndex = 0; vertexOffset = 0; baseInstance = 0;
                        continue;
                    }
                    long drawParamsPtr = drawParamsBasePtr2;
                    indexCount += DrawParametersBuffer.getIndexCount(drawParamsPtr);
                    if (indexCount == DrawParametersBuffer.getIndexCount(drawParamsPtr)) {
                        firstIndex = DrawParametersBuffer.getFirstIndex(drawParamsPtr);
                        vertexOffset = DrawParametersBuffer.getVertexOffset(drawParamsPtr);
                        baseInstance = DrawParametersBuffer.getBaseInstance(drawParamsPtr);
                    }
                    drawParamsBasePtr2 += DrawParametersBuffer.STRIDE;
                }
                if (indexCount > 0) {
                    MemoryUtil.memPutInt(ptr, indexCount);
                    MemoryUtil.memPutInt(ptr + 4, 1);
                    MemoryUtil.memPutInt(ptr + 8, firstIndex);
                    MemoryUtil.memPutInt(ptr + 12, vertexOffset);
                    MemoryUtil.memPutInt(ptr + 16, baseInstance);
                    MemoryUtil.memPutInt(ptr + 20, 0);
                    MemoryUtil.memPutInt(ptr + 24, 0);
                    MemoryUtil.memPutInt(ptr + 28, 0);
                    ptr += CMD_STRIDE;
                    drawCount++;
                }
            }
        } else {
            for (var iterator = queue.iterator(isTranslucent); iterator.hasNext(); ) {
                sectionIndices[count++] = iterator.next().inAreaIndex;
            }
            final long facingOffset = UNDEFINED_FACING_IDX * DrawParametersBuffer.STRIDE;
            drawParamsBasePtr += facingOffset;
            long ptr = bufferPtr;
            for (int i = 0; i < count; ++i) {
                long drawParamsPtr = drawParamsBasePtr + (sectionIndices[i] * facingsStride);
                int indexCount = DrawParametersBuffer.getIndexCount(drawParamsPtr);
                if (indexCount <= 0) continue;
                MemoryUtil.memPutInt(ptr, indexCount);
                MemoryUtil.memPutInt(ptr + 4, 1);
                MemoryUtil.memPutInt(ptr + 8, DrawParametersBuffer.getFirstIndex(drawParamsPtr));
                MemoryUtil.memPutInt(ptr + 12, DrawParametersBuffer.getVertexOffset(drawParamsPtr));
                MemoryUtil.memPutInt(ptr + 16, DrawParametersBuffer.getBaseInstance(drawParamsPtr));
                // CORREÇÃO: Alinhamento de 32 bytes aqui também
                MemoryUtil.memPutInt(ptr + 20, 0);
                MemoryUtil.memPutInt(ptr + 24, 0);
                MemoryUtil.memPutInt(ptr + 28, 0);
                ptr += CMD_STRIDE;
                drawCount++;
            }
        }
        if (drawCount == 0) return;
        ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(cmdBufferPtr, queue.size() * QuadFacing.COUNT * CMD_STRIDE);
        indirectBuffer.recordCopyCmd(byteBuffer.position(0));
        vkCmdDrawIndexedIndirect(Renderer.getCommandBuffer(), indirectBuffer.getId(), indirectBuffer.getOffset(), drawCount, CMD_STRIDE);
        }
            
