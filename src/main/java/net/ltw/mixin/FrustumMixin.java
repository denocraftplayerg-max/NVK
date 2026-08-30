package net.ltw.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;

/**
 * This mixin exists to make Frustum's internal FrustumIntersection accessible
 * via the accessWidener. No injection needed — FrustumExtractor handles the math.
 *
 * The actual frustum data is extracted in MinecraftMixin from the projection matrix,
 * which is more reliable than trying to read JOML's internal plane storage.
 */
@Mixin(Frustum.class)
public class FrustumMixin {
    // Access widener handles visibility. No code injection needed.
}
