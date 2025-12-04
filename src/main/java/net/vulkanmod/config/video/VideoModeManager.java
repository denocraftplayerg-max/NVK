package net.vulkanmod.config.video;

import net.vulkanmod.Initializer;
import net.vulkanmod.config.Config;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public abstract class VideoModeManager {
    private static VideoModeSet.VideoMode osVideoMode;
    private static VideoModeSet[] videoModeSets;

    public static VideoModeSet.VideoMode selectedVideoMode;

    public static void init() {
        // Отримуємо доступ до конфігурації
        Config config = Initializer.CONFIG;

        PointerBuffer monitors = GLFW.glfwGetMonitors();
        long monitor;

        // БЕЗПЕЧНА ПЕРЕВІРКА: Якщо конфіг ще не ініціалізовано, використовуємо дефолтний монітор
        if (config == null) {
            monitor = glfwGetPrimaryMonitor();
        } else {
            int targetIndex = config.targetMonitor;
            if (monitors != null && targetIndex >= 0 && targetIndex < monitors.limit()) {
                monitor = monitors.get(targetIndex);
            } else {
                monitor = glfwGetPrimaryMonitor();
                if (monitors != null && targetIndex >= monitors.limit()) {
                    config.targetMonitor = 0;
                }
            }
        }

        osVideoMode = getCurrentVideoMode(monitor);
        videoModeSets = populateVideoResolutions(monitor);
    }

    public static void applySelectedVideoMode() {
        if (Initializer.CONFIG != null) {
            Initializer.CONFIG.videoMode = selectedVideoMode;
        }
    }

    public static VideoModeSet[] getVideoResolutions() {
        return videoModeSets;
    }

    public static VideoModeSet getFirstAvailable() {
        if(videoModeSets != null && videoModeSets.length > 0)
            return videoModeSets[videoModeSets.length - 1];
        else
            return VideoModeSet.getDummy();
    }

    public static VideoModeSet.VideoMode getOsVideoMode() {
        return osVideoMode;
    }

    public static VideoModeSet.VideoMode getCurrentVideoMode(long monitor){
        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);

        if (vidMode == null)
            vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

        if (vidMode == null)
            throw new NullPointerException("Unable to get current video mode");

        return new VideoModeSet.VideoMode(vidMode.width(), vidMode.height(), vidMode.redBits(), vidMode.refreshRate());
    }

    public static VideoModeSet[] populateVideoResolutions(long monitor) {
        GLFWVidMode.Buffer buffer = GLFW.glfwGetVideoModes(monitor);

        if (buffer == null) return new VideoModeSet[0];

        List<VideoModeSet> videoModeSets = new ArrayList<>();

        int currWidth = 0, currHeight = 0, currBitDepth = 0;
        VideoModeSet videoModeSet = null;

        for (int i = 0; i < buffer.limit(); i++) {
            buffer.position(i);
            int bitDepth = buffer.redBits();
            if (buffer.redBits() < 8 || buffer.greenBits() != bitDepth || buffer.blueBits() != bitDepth)
                continue;

            int width = buffer.width();
            int height = buffer.height();
            int refreshRate = buffer.refreshRate();

            if (currWidth != width || currHeight != height || currBitDepth != bitDepth) {
                currWidth = width;
                currHeight = height;
                currBitDepth = bitDepth;

                videoModeSet = new VideoModeSet(currWidth, currHeight, currBitDepth);
                videoModeSets.add(videoModeSet);
            }

            if (videoModeSet != null) {
                videoModeSet.addRefreshRate(refreshRate);
            }
        }

        VideoModeSet[] arr = new VideoModeSet[videoModeSets.size()];
        videoModeSets.toArray(arr);

        return arr;
    }

    public static VideoModeSet getFromVideoMode(VideoModeSet.VideoMode videoMode) {
        if (videoModeSets == null) return null;

        for (var set : videoModeSets) {
            if (set.width == videoMode.width && set.height == videoMode.height)
                return set;
        }

        return null;
    }
}