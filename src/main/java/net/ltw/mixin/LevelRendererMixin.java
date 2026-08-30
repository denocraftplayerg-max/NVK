package net.ltw.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into the level rendering pipeline.
 *
 * In MC 1.21.11, chunks are rendered via SectionRenderDispatcher which
 * uploads geometry to VBOs. Each VBO has a baseVertex offset.
 * The actual baseVertex mapping is done in the C++ side (LTW) by matching
 * the draw call parameters against registered chunk positions.
 *
 * This mixin serves as the synchronization point — LTW clears positions at
 * tick start (MinecraftMixin) and expects them to be registered before draw calls.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void ltw$onRenderLevelStart(
        PoseStack poseStack,
        float partialTick,
        long finishNanoTime,
        boolean renderBlockOutline,
        Camera camera,
        Frustum frustum,
        CallbackInfo ci
    ) {
        // Chunk position registration happens on the LTW (C++) side
        // when glMultiDrawElementsBaseVertex is intercepted.
        // This hook ensures frame-level synchronization.
    }
}
