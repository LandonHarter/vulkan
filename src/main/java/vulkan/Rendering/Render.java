package vulkan.Rendering;

import engine.Window;
import vulkan.Commands.CommandPool;
import vulkan.Devices.Device;
import vulkan.Devices.PhysicalDevice;
import vulkan.Instance;
import vulkan.Model.VulkanModel;
import vulkan.Pipeline.PipelineCache;
import vulkan.Queue;
import vulkan.Vulkan;
import vulkan.VulkanSurface;
import vulkan.SwapChain.SwapChain;

import java.util.*;

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

    public static Vulkan.VulkanSettings VulkanSettings;
    public static Render Instance;

    public Render(Vulkan.VulkanSettings settings) {
        Instance = this;
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
    }

    public void cleanup() {
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

}
