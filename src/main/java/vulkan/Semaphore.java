package vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import vulkan.Devices.Device;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK11.*;

public class Semaphore {

    private final Device device;
    private final long vkSemaphore;

    public Semaphore(Device device) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            LongBuffer lp = stack.mallocLong(1);
            VulkanUtils.vkCheck(vkCreateSemaphore(device.getVkDevice(), semaphoreCreateInfo, null, lp),
                    "Failed to create semaphore");
            vkSemaphore = lp.get(0);
        }
    }

    public void cleanup() {
        vkDestroySemaphore(device.getVkDevice(), vkSemaphore, null);
    }

    public long getVkSemaphore() {
        return vkSemaphore;
    }
}
