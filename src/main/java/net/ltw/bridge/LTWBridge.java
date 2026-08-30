package net.ltw.bridge;

/**
 * JNI bridge to libltw.so.
 * LTW is already loaded by PojavLauncher — we just link to its symbols.
 */
public final class LTWBridge {
    private static boolean loaded = false;
    private static boolean available = false;

    public static void tryLoad() {
        if (loaded) return;
        loaded = true;
        try {
            // LTW is already loaded by PojavLauncher as libltw.so.
            // We register our native methods against it.
            // On PojavLauncher, System.loadLibrary("ltw") succeeds because it's in LD_LIBRARY_PATH.
            System.loadLibrary("ltw");
            available = true;
            System.out.println("[LTW-Bridge] Linked to libltw.so");
        } catch (Throwable t) {
            System.out.println("[LTW-Bridge] libltw.so not available: " + t.getMessage());
            available = false;
        }
    }

    public static boolean isAvailable() { return available; }

    /** Register a chunk's world position by its baseVertex (called per chunk per frame) */
    public static native void registerChunkPosition(int baseVertex, float x, float y, float z);

    /** Clear all registered chunk positions (call once per frame at start) */
    public static native void clearChunkPositions();

    /** Update the 6 frustum planes (call once per frame). planes = float[24] = 6 * vec4(Nx,Ny,Nz,D) */
    public static native void updateFrustumPlanes(float[] planes);

    /** Check if frustum planes have been set this frame */
    public static native boolean hasFrustumPlanes();
}
