package vulkan.Rendering;

import engine.Matrix4;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.*;
import org.tinylog.Logger;
import vulkan.*;
import vulkan.Commands.CommandBuffer;
import vulkan.Commands.CommandPool;
import vulkan.Devices.Device;
import vulkan.Image.Attachment;
import vulkan.Image.ImageView;
import vulkan.Model.ModelData;
import vulkan.Model.VulkanModel;
import vulkan.Pipeline.Pipeline;
import vulkan.Pipeline.PipelineCache;
import vulkan.Shaders.ShaderCompiler;
import vulkan.Shaders.VulkanShader;
import vulkan.SwapChain.SwapChain;
import vulkan.SwapChain.SwapChainRenderPass;
import vulkan.Vertex.VertexBufferStructure;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanRenderer {

    private final CommandBuffer[] commandBuffers;
    private final Fence[] fences;
    private FrameBuffer[] frameBuffers;
    private final VulkanShader shader;
    private final Pipeline pipeLine;
    private final SwapChainRenderPass renderPass;
    private SwapChain swapChain;
    private final CommandPool commandPool;
    private final Queue.GraphicsQueue graphQueue;
    private final Device device;

    public final List<VulkanModel> vulkanModels;
    private Attachment[] depthAttachments;

    public VulkanRenderer(String vertexShaderPath, String fragmentShaderPath) {
        Render render = Vulkan.render;
        PipelineCache pipelineCache = render.pipelineCache;
        this.swapChain = render.swapChain;
        this.commandPool = render.commandPool;
        this.graphQueue = render.graphQueue;
        this.device = render.device;
        vulkanModels = new ArrayList<>();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Device device = swapChain.getDevice();
            VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();
            ImageView[] imageViews = swapChain.getImageViews();

            int numImages = swapChain.getImageViews().length;
            createDepthImages();
            renderPass = new SwapChainRenderPass(swapChain, depthAttachments[0].getImage().getFormat());
            createFrameBuffers();

            ShaderCompiler.compileShaderIfChanged(vertexShaderPath, Shaderc.shaderc_glsl_vertex_shader);
            ShaderCompiler.compileShaderIfChanged(fragmentShaderPath, Shaderc.shaderc_glsl_fragment_shader);
            shader = new VulkanShader(device, new VulkanShader.ShaderModuleData[]
                    {
                            new VulkanShader.ShaderModuleData(VK_SHADER_STAGE_VERTEX_BIT, vertexShaderPath + ".spv"),
                            new VulkanShader.ShaderModuleData(VK_SHADER_STAGE_FRAGMENT_BIT, fragmentShaderPath + ".spv"),
                    });
            Pipeline.PipeLineCreationInfo pipeLineCreationInfo = new Pipeline.PipeLineCreationInfo(
                    renderPass.getVkRenderPass(), shader, 1, true, GraphConstants.MAT4X4_SIZE * 3, new VertexBufferStructure());
            pipeLine = new Pipeline(pipelineCache, pipeLineCreationInfo);
            pipeLineCreationInfo.cleanup();

            commandBuffers = new CommandBuffer[numImages];
            fences = new Fence[numImages];
            for (int i = 0; i < numImages; i++) {
                commandBuffers[i] = new CommandBuffer(commandPool, true, false);
                fences[i] = new Fence(device, true);
            }
        }
    }

    public void cleanup() {
        pipeLine.cleanup();
        shader.cleanup();
        Arrays.stream(depthAttachments).forEach(Attachment::cleanup);
        Arrays.stream(frameBuffers).forEach(FrameBuffer::cleanup);
        renderPass.cleanup();
        Arrays.stream(commandBuffers).forEach(CommandBuffer::cleanup);
        Arrays.stream(fences).forEach(Fence::cleanup);
    }

    public void resize(SwapChain swapChain) {
        this.swapChain = swapChain;
        Arrays.stream(frameBuffers).forEach(FrameBuffer::cleanup);
        Arrays.stream(depthAttachments).forEach(Attachment::cleanup);
        createDepthImages();
        createFrameBuffers();
    }

    private void createDepthImages() {
        int numImages = swapChain.getNumImages();
        VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();
        depthAttachments = new Attachment[numImages];
        for (int i = 0; i < numImages; i++) {
            depthAttachments[i] = new Attachment(device, swapChainExtent.width(), swapChainExtent.height(),
                    VK_FORMAT_D32_SFLOAT, VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
        }
    }

    private void createFrameBuffers() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();
            ImageView[] imageViews = swapChain.getImageViews();
            int numImages = imageViews.length;

            LongBuffer pAttachments = stack.mallocLong(2);
            frameBuffers = new FrameBuffer[numImages];
            for (int i = 0; i < numImages; i++) {
                pAttachments.put(0, imageViews[i].getVkImageView());
                pAttachments.put(1, depthAttachments[i].getImageView().getVkImageView());
                frameBuffers[i] = new FrameBuffer(device, swapChainExtent.width(), swapChainExtent.height(),
                        pAttachments, renderPass.getVkRenderPass());
            }
        }
    }

    public void addMeshes(List<ModelData> modelDataList) {
        Logger.debug("Loading {} model(s)", modelDataList.size());
        vulkanModels.addAll(VulkanModel.transformModels(modelDataList, commandPool, graphQueue));
        Logger.debug("Loaded {} model(s)", modelDataList.size());
    }

    public void addMesh(ModelData modelData) {
        Logger.debug("Loading model...");
        vulkanModels.add(VulkanModel.transformModels(modelData, commandPool, graphQueue));
        Logger.debug("Loaded model");
    }

    public void recordCommandBuffer(List<VulkanModel> vulkanModelList) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkExtent2D swapChainExtent = swapChain.getSwapChainExtent();
            int width = swapChainExtent.width();
            int height = swapChainExtent.height();
            int idx = swapChain.getCurrentFrame();

            Fence fence = fences[idx];
            CommandBuffer commandBuffer = commandBuffers[idx];
            FrameBuffer frameBuffer = frameBuffers[idx];

            fence.fenceWait();
            fence.reset();

            commandBuffer.reset();
            VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
            clearValues.apply(0, v -> v.color().float32(0, 0.5f).float32(1, 0.7f).float32(2, 0.9f).float32(3, 1));
            clearValues.apply(1, v -> v.depthStencil().depth(1.0f));

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass.getVkRenderPass())
                    .pClearValues(clearValues)
                    .renderArea(a -> a.extent().set(width, height))
                    .framebuffer(frameBuffer.getVkFrameBuffer());

            commandBuffer.beginRecording();
            VkCommandBuffer cmdHandle = commandBuffer.getVkCommandBuffer();
            vkCmdBeginRenderPass(cmdHandle, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);

            vkCmdBindPipeline(cmdHandle, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeLine.getVkPipeline());

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                    .x(0)
                    .y(height)
                    .height(-height)
                    .width(width)
                    .minDepth(0.0f)
                    .maxDepth(1.0f);
            vkCmdSetViewport(cmdHandle, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack)
                    .extent(it -> it
                            .width(width)
                            .height(height))
                    .offset(it -> it
                            .x(0)
                            .y(0));
            vkCmdSetScissor(cmdHandle, 0, scissor);

            LongBuffer offsets = stack.mallocLong(1);
            offsets.put(0, 0L);
            LongBuffer vertexBuffer = stack.mallocLong(1);
            ByteBuffer pushConstantBuffer = stack.malloc(GraphConstants.MAT4X4_SIZE * 3);
            for (VulkanModel vulkanModel : vulkanModelList) {
                String modelId = vulkanModel.getModelId();

                for (VulkanModel.VulkanMesh mesh : vulkanModel.getVulkanMeshList()) {
                    vertexBuffer.put(0, mesh.verticesBuffer().getBuffer());
                    vkCmdBindVertexBuffers(cmdHandle, 0, vertexBuffer, offsets);
                    vkCmdBindIndexBuffer(cmdHandle, mesh.indicesBuffer().getBuffer(), 0, VK_INDEX_TYPE_UINT32);

                    setPushConstants(cmdHandle, Camera.projection, Camera.view, Matrix4.CalculateTransform(new Vector3f(0, 0, -3)),
                            pushConstantBuffer);
                    vkCmdDrawIndexed(cmdHandle, mesh.numIndices(), 1, 0, 0, 0);
                }
            }

            vkCmdEndRenderPass(cmdHandle);
            commandBuffer.endRecording();
        }
    }

    private void setPushConstants(VkCommandBuffer cmdHandle, Matrix4f projMatrix, Matrix4f view, Matrix4f modelMatrix,
                                  ByteBuffer pushConstantBuffer) {
        projMatrix.get(pushConstantBuffer);
        view.get(GraphConstants.MAT4X4_SIZE, pushConstantBuffer);
        modelMatrix.get(GraphConstants.MAT4X4_SIZE * 2, pushConstantBuffer);
        vkCmdPushConstants(cmdHandle, pipeLine.getVkPipelineLayout(),
                VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstantBuffer);
    }

    public void submit(Queue queue) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int idx = swapChain.getCurrentFrame();
            CommandBuffer commandBuffer = commandBuffers[idx];
            Fence currentFence = fences[idx];
            SwapChain.SyncSemaphores syncSemaphores = swapChain.getSyncSemaphoresList()[idx];
            queue.submit(stack.pointers(commandBuffer.getVkCommandBuffer()),
                    stack.longs(syncSemaphores.imgAcquisitionSemaphore().getVkSemaphore()),
                    stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                    stack.longs(syncSemaphores.renderCompleteSemaphore().getVkSemaphore()), currentFence);
        }
    }

}
