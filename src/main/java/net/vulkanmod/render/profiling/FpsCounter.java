package net.vulkanmod.render.profiling;

import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

public class FpsCounter {
    private static final int WINDOW_SIZE = 100;
    private static final List<Integer> fpsHistory = new ArrayList<>(WINDOW_SIZE);

    private static int minFps = 0;
    private static int maxFps = 0;
    private static int averageFps = 0;
    
    private static long lastResetTime = System.currentTimeMillis();
    private static long lastStringUpdateTime = 0;
    
    // Cache da string para evitar alocação de lixo (GC Free por frame)
    private static String cachedFpsString = "FPS: 0 | Min: 0 | Max: 0 | Med: 0";

    public static void tick() {
        int currentFps = Minecraft.getInstance().getFps(); // Abordagem mais segura via instância

        if (currentFps <= 0) return;

        // Inicialização limpa no primeiro frame válido
        if (minFps == 0) minFps = currentFps;

        // Histórico em janela deslizante
        fpsHistory.add(currentFps);
        if (fpsHistory.size() > WINDOW_SIZE) {
            fpsHistory.remove(0);
        }

        // Reset cíclico de estabilidade (10 segundos)
        long now = System.currentTimeMillis();
        if (now - lastResetTime > 10000) {
            minFps = currentFps;
            maxFps = currentFps;
            lastResetTime = now;
        }

        if (currentFps < minFps) minFps = currentFps;
        if (currentFps > maxFps) maxFps = currentFps;

        // Cálculo da média
        int sum = 0;
        for (int fps : fpsHistory) {
            sum += fps;
        }
        averageFps = sum / fpsHistory.size();

        // OTIMIZAÇÃO: Atualiza a string apenas a cada 300ms (Legibilidade + Performance)
        if (now - lastStringUpdateTime > 300) {
            cachedFpsString = "FPS: " + currentFps + " | Min: " + minFps + " | Max: " + maxFps + " | Med: " + averageFps;
            lastStringUpdateTime = now;
        }
    }

    public static String getFpsString() {
        return cachedFpsString;
    }
}
