package net.ltw.mixin;

import net.ltw.bridge.FrustumExtractor;
import net.ltw.bridge.LTWBridge;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into Frustum.calculateFrustum to extract the 6 frustum planes.
 * This method receives modelView and projection matrices directly —
 * no need to access private GameRenderer methods.
 *
 * Based on VulkanMod's proven approach for MC 1.21.11.
 */
@Mixin(Frustum.class)
public class FrustumMixin {

    @Inject(method = "calculateFrustum", at = @At("HEAD"))
    private void ltw$onCalculateFrustum(Matrix4f modelView, Matrix4f projection, CallbackInfo ci) {
        if (!LTWBridge.isAvailable()) return;

        try {
            float[] planes = FrustumExtractor.extract(modelView, projection);
            if (planes != null) {
                LTWBridge.updateFrustumPlanes(planes);
            }
        } catch (Throwable t) {
            // Silently fail — LTW will use fallback (no culling)
        }
    }
}
