package vulkan.Rendering;

import engine.Window;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.tinylog.Logger;
import vulkan.Commands.CommandPool;
import vulkan.Devices.Device;
import vulkan.Devices.PhysicalDevice;
import vulkan.Instance;
import vulkan.Model.ModelData;
import vulkan.Model.VulkanModel;
import vulkan.Pipeline.PipelineCache;
import vulkan.Queue;
import vulkan.Vulkan;
import vulkan.VulkanSurface;
import vulkan.SwapChain.SwapChain;

import java.nio.LongBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.*;
import static vulkan.VulkanUtils.vkCheck;

public class Render {

    public final CommandPool commandPool;
    public final Device device;
    public final Queue.GraphicsQueue graphQueue;
    public final Instance instance;
    public final PhysicalDevice physicalDevice;
    public final PipelineCache pipelineCache;
    public final Queue.PresentQueue presentQueue;
    public final VulkanSurface surface;
    public SwapChain swapChain;
    public final List<VulkanModel> vulkanModels;
    public final List<VulkanRenderer> renderers;
    public long descriptorSetLayout;

    public static Vulkan.VulkanSettings VulkanSettings;

    public Render(Vulkan.VulkanSettings settings) {
        VulkanSettings = settings;
        instance = new Instance(settings.validate);
        physicalDevice = PhysicalDevice.createPhysicalDevice(instance, null);
        device = new Device(physicalDevice);
        surface = new VulkanSurface(physicalDevice, Window.GetWindow());
        graphQueue = new Queue.GraphicsQueue(device, 0);
        presentQueue = new Queue.PresentQueue(device, surface, 0);
        swapChain = new SwapChain(device, surface, 1,
                settings.vSync);
        commandPool = new CommandPool(device, graphQueue.getQueueFamilyIndex());
        pipelineCache = new PipelineCache(device);
        vulkanModels = new ArrayList<>();
        renderers = new ArrayList<>();
        createDescriptorSetLayout();
    }

    public void cleanup() {
        VK10.vkDestroyPipelineLayout(device.getVkDevice(), descriptorSetLayout, null);
        presentQueue.waitIdle();
        graphQueue.waitIdle();
        device.waitIdle();
        vulkanModels.forEach(VulkanModel::cleanup);
        pipelineCache.cleanup();
        renderers.forEach(VulkanRenderer::cleanup);
        commandPool.cleanup();
        swapChain.cleanup();
        surface.cleanup();
        device.cleanup();
        physicalDevice.cleanup();
        instance.cleanup();
    }

    public void render() {
        if (Window.GetWidth() <= 0 && Window.GetHeight() <= 0) {
            return;
        }
        if (Window.IsResized() || swapChain.acquireNextImage()) {
            Window.ResetResized();
            resize();
            // Recalculate projection
            swapChain.acquireNextImage();
        }

        for (VulkanRenderer renderer : renderers) {
            renderer.recordCommandBuffer(renderer.vulkanModels);
            renderer.submit(presentQueue);
        }

        if (swapChain.presentImage(graphQueue)) {
            Window.SetResized(true);
        }
    }

    private void resize() {
        device.waitIdle();
        graphQueue.waitIdle();

        swapChain.cleanup();

        swapChain = new SwapChain(device, surface, 1,
                VulkanSettings.vSync);

        for (VulkanRenderer renderer : renderers) {
            renderer.resize(swapChain);
        }
    }

    private void createDescriptorSetLayout() {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer uboLayoutBinding = VkDescriptorSetLayoutBinding.calloc(1, stack);
            uboLayoutBinding.binding(0);
            uboLayoutBinding.descriptorCount(1);
            uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uboLayoutBinding.pImmutableSamplers(null);
            uboLayoutBinding.stageFlags(VK_SHADER_STAGE_ALL);

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(uboLayoutBinding);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            vkCheck(VK10.vkCreateDescriptorSetLayout(device.getVkDevice(), layoutInfo, null, pDescriptorSetLayout), "Failed to create descriptor set layout");
            descriptorSetLayout = pDescriptorSetLayout.get(0);
        }
    }

}
