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
 * Hooks into Frustum.calculateFrustum to extract frustum planes.
 * This method receives modelView and projection as parameters —
 * no field access needed, no accessWidener required.
 */
@Mixin(Frustum.class)
public class FrustumMixin {

    @Inject(method = "prepare", at = @At("HEAD"))
    private void ltw$onPrepare(double camX, double camY, double camZ, Matrix4f projection, Matrix4f modelView, CallbackInfo ci) {
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
