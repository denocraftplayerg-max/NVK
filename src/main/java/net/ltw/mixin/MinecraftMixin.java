package net.ltw.mixin;

import net.ltw.bridge.LTWBridge;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into Minecraft.runTick to clear chunk positions at frame start.
 * Frustum planes are updated separately in FrustumMixin.
 *
 * Based on VulkanMod's MinecraftMixin for MC 1.21.11.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "runTick", at = @At("HEAD"))
    private void ltw$onFrameStart(boolean bl, CallbackInfo ci) {
        if (!LTWBridge.isAvailable()) return;
        LTWBridge.clearChunkPositions();
    }
}
