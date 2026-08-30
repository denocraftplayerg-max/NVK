package net.ltw.mixin;

import net.ltw.bridge.FrustumExtractor;
import net.ltw.bridge.LTWBridge;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    private static final float[] MATRIX_BUF = new float[16];

    /**
     * At the START of each render tick:
     * 1. Clear chunk positions from last frame
     * 2. Extract frustum planes from current camera
     * 3. Send to LTW via JNI
     */
    @Inject(method = "runTick", at = @At("HEAD"))
    private void ltw$onTickStart(boolean renderLevel, CallbackInfo ci) {
        if (!LTWBridge.isAvailable()) return;

        // 1. Clear chunk positions
        LTWBridge.clearChunkPositions();

        // 2. Extract frustum from current projection * view matrix
        Minecraft mc = (Minecraft)(Object) this;
        if (mc.gameRenderer == null) return;

        try {
            float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            Matrix4f proj = mc.gameRenderer.getProjectionMatrix(
                mc.gameRenderer.getFov(mc.gameRenderer.getMainCamera(), partialTick, true)
            );
            Matrix4f view = mc.gameRenderer.getMainCamera().getViewMatrix(partialTick);
            Matrix4f vp = new Matrix4f(proj).mul(view);
            vp.get(MATRIX_BUF);

            float[] planes = FrustumExtractor.fromMatrix4f(MATRIX_BUF);
            if (planes != null) {
                LTWBridge.updateFrustumPlanes(planes);
            }
        } catch (Throwable t) {
            // Silently fail — LTW will use fallback (no culling)
        }
    }
}
