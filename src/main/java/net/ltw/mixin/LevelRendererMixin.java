package net.ltw.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ltw.bridge.LTWBridge;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into LevelRenderer.renderLevel for frame synchronization.
 * Chunk position registration happens on the LTW (C++) side when
 * glMultiDrawElementsBaseVertex is intercepted.
 *
 * Based on VulkanMod's LevelRendererMixin for MC 1.21.11.
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
        // Synchronization point.
        // Frustum planes already updated via FrustumMixin.calculateFrustum.
        // Chunk positions cleared via MinecraftMixin.runTick.
        // LTW C++ side handles baseVertex -> position mapping.
    }
}
