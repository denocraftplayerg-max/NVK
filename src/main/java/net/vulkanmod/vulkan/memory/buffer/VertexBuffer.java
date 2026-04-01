    class CompressedVertexBuilder implements VertexBuilder {
        private static final int VERTEX_SIZE = 16;
        public static final float POS_CONV_MUL = 2048.0f;
        public static final float POS_OFFSET = -4.0f;
        public static final float POS_OFFSET_CONV = POS_OFFSET * POS_CONV_MUL;
        public static final float UV_CONV_MUL = 32768.0f;

        public void vertex(long ptr, float x, float y, float z, int color, float u, float v, int light, int packedNormal) {
            MemoryUtil.memPutShort(ptr + 0, (short) (x * POS_CONV_MUL + POS_OFFSET_CONV));
            MemoryUtil.memPutShort(ptr + 2, (short) (y * POS_CONV_MUL + POS_OFFSET_CONV));
            MemoryUtil.memPutShort(ptr + 4, (short) (z * POS_CONV_MUL + POS_OFFSET_CONV));

            // CORREÇÃO: Encoding de luz corrigido para descompactar os 32 bits do Minecraft
            int blockLight = light & 0xFFFF;
            int skyLight = (light >>> 16) & 0xFFFF;
            final short l = (short) (((skyLight & 0xFF) << 8) | (blockLight & 0xFF));
            MemoryUtil.memPutShort(ptr + 6, l);

            MemoryUtil.memPutShort(ptr + 8, (short) (u * UV_CONV_MUL));
            MemoryUtil.memPutShort(ptr + 10, (short) (v * UV_CONV_MUL));
            MemoryUtil.memPutInt(ptr + 12, color);
        }

        @Override public void position(long ptr, float x, float y, float z) {
            MemoryUtil.memPutShort(ptr + 0, (short) (x * POS_CONV_MUL + POS_OFFSET_CONV));
            MemoryUtil.memPutShort(ptr + 2, (short) (y * POS_CONV_MUL + POS_OFFSET_CONV));
            MemoryUtil.memPutShort(ptr + 4, (short) (z * POS_CONV_MUL + POS_OFFSET_CONV));
        }

        @Override public void color(long ptr, int color) { MemoryUtil.memPutInt(ptr + 12, color); }
        @Override public void uv(long ptr, float u, float v) {
            MemoryUtil.memPutShort(ptr + 8, (short) (u * UV_CONV_MUL));
            MemoryUtil.memPutShort(ptr + 10, (short) (v * UV_CONV_MUL));
        }

        @Override public void light(long ptr, int light) {
            // CORREÇÃO: Mesma lógica de luz aplicada aqui
            int blockLight = light & 0xFFFF;
            int skyLight = (light >>> 16) & 0xFFFF;
            final short l = (short) (((skyLight & 0xFF) << 8) | (blockLight & 0xFF));
            MemoryUtil.memPutShort(ptr + 6, l);
        }

        @Override public void normal(long ptr, int normal) {}
        @Override public int getStride() { return VERTEX_SIZE; }
    }
