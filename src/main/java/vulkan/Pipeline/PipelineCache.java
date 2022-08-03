package vulkan.Pipeline;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;
import org.tinylog.Logger;
import vulkan.Devices.Device;
import vulkan.VulkanUtils;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class PipelineCache {

    private final Device device;
    private final long vkPipelineCache;

    public PipelineCache(Device device) {
        Logger.debug("Creating pipeline cache");
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineCacheCreateInfo createInfo = VkPipelineCacheCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);

            LongBuffer lp = stack.mallocLong(1);
            VulkanUtils.vkCheck(vkCreatePipelineCache(device.getVkDevice(), createInfo, null, lp),
                    "Error creating pipeline cache");
            vkPipelineCache = lp.get(0);
        }
    }

    public void cleanup() {
        Logger.debug("Destroying pipeline cache");
        vkDestroyPipelineCache(device.getVkDevice(), vkPipelineCache, null);
    }

    public Device getDevice() {
        return device;
    }

    public long getVkPipelineCache() {
        return vkPipelineCache;
    }
}