package net.vulkanmod.interfaces.shader;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.vulkanmod.render.engine.EGlProgram;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;

public interface ExtendedRenderPipeline {

    static ExtendedRenderPipeline of(RenderPipeline renderPipeline) {
        return (ExtendedRenderPipeline) renderPipeline;
    }

    Pipeline getPipeline();

    void setPipeline(GraphicsPipeline pipeline);

    EGlProgram getProgram();

    void setProgram(EGlProgram program);
}
