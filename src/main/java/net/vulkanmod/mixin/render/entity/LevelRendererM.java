package net.vulkanmod.mixin.render.entity;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelRenderer.class)
public class LevelRendererM {

    // TODO
//    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;
//    @Shadow @Final private Minecraft minecraft;
//
//    @Unique private Object2ReferenceOpenHashMap<MultiBufferSource, Map<Class<? extends Entity>, ObjectArrayList<Entity>>> bufferSourceMap = new Object2ReferenceOpenHashMap<>();
//    @Unique boolean managed;
//
//    @Inject(method = "renderLevel",
//            at = @At(value = "INVOKE",
//                    target = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
//                    shift = At.Shift.AFTER)
//    )
//    private void clearMap(GraphicsResourceAllocator graphicsResourceAllocator, DeltaTracker deltaTracker, boolean bl,
//                          Camera camera, Matrix4f matrix4f, Matrix4f matrix4f2, Matrix4f matrix4f3,
//                          GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl2, CallbackInfo ci) {
//        for (var bufferSource : this.bufferSourceMap.keySet()) {
//            var entityMap = this.bufferSourceMap.get(bufferSource);
//            entityMap.clear();
//        }
//
//        this.managed = true;
//    }
//
//    /**
//     * @author
//     * @reason
//     */
//    @Overwrite
//    private void renderEntity(Entity entity, double d, double e, double f, float partialTicks, PoseStack poseStack, MultiBufferSource multiBufferSource) {
//        if (!Initializer.CONFIG.entityCulling || !this.managed) {
//            double h = Mth.lerp(partialTicks, entity.xOld, entity.getX());
//            double i = Mth.lerp(partialTicks, entity.yOld, entity.getY());
//            double j = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
//            this.entityRenderDispatcher.render(entity, h - d, i - e, j - f, partialTicks, poseStack, multiBufferSource, this.entityRenderDispatcher.getPackedLightCoords(entity, partialTicks));
//            return;
//        }
//
//        var entityClass = entity.getClass();
//
//        var entityMap = this.bufferSourceMap.computeIfAbsent(multiBufferSource, bufferSource -> new Object2ReferenceOpenHashMap<>());
//        var list = entityMap.computeIfAbsent(entityClass, k -> new ObjectArrayList<>());
//        list.add(entity);
//    }
//
//    @Inject(method = "renderEntities", at = @At("RETURN"))
//    private void renderEntities(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource1, Camera camera, DeltaTracker deltaTracker, List<Entity> entityList, CallbackInfo ci) {
//        if (!Initializer.CONFIG.entityCulling)
//            return;
//
//        Vec3 cameraPos = WorldRenderer.getCameraPos();
//        TickRateManager tickRateManager = this.minecraft.level.tickRateManager();
//
////        PoseStack poseStack = new PoseStack();
//
//        for (var bufferSource : this.bufferSourceMap.keySet()) {
//            var entityMap = this.bufferSourceMap.get(bufferSource);
//
//            for (var list : entityMap.values()) {
//                for (Entity entity : list) {
//                    float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(!tickRateManager.isEntityFrozen(entity));
//
//                    double h = Mth.lerp(partialTicks, entity.xOld, entity.getX());
//                    double i = Mth.lerp(partialTicks, entity.yOld, entity.getY());
//                    double j = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
//                    this.entityRenderDispatcher.render(entity, h - cameraPos.x, i - cameraPos.y, j - cameraPos.z, partialTicks, poseStack, bufferSource, this.entityRenderDispatcher.getPackedLightCoords(entity, partialTicks));
//                }
//            }
//        }
//
//        this.managed = false;
//    }
}
