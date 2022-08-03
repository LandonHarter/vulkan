package vulkan.Image;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import vulkan.Devices.Device;
import vulkan.VulkanUtils;

import java.nio.LongBuffer;

import static vulkan.VulkanUtils.vkCheck;

public class Image {

    public Device device;
    public int format, mipLevels;

    public long vkImage, vkMemory;

    public Image(Device device, ImageData imageData) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.format = imageData.format;
            this.mipLevels = imageData.mipLevels;

            VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK10.VK_IMAGE_TYPE_2D)
                    .format(format)
                    .extent(it -> it
                            .width(imageData.width)
                            .height(imageData.height)
                            .depth(1)
                    )
                    .mipLevels(mipLevels)
                    .arrayLayers(imageData.arrayLayers)
                    .samples(imageData.sampleCount)
                    .initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED)
                    .sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE)
                    .tiling(VK10.VK_IMAGE_TILING_OPTIMAL)
                    .usage(imageData.usage);
            LongBuffer lp = stack.mallocLong(1);
            vkCheck(VK10.vkCreateImage(device.getVkDevice(), imageCreateInfo, null, lp), "Failed to create image");
            vkImage = lp.get(0);

            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
            VK10.vkGetImageMemoryRequirements(device.getVkDevice(), vkImage, memReqs);
            VkMemoryAllocateInfo memAlloc = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(VulkanUtils.memoryTypeFromProperties(device.getPhysicalDevice(),
                            memReqs.memoryTypeBits(), 0));
            vkCheck(VK10.vkAllocateMemory(device.getVkDevice(), memAlloc, null, lp), "Failed to allocate memory");
            vkMemory = lp.get(0);

            vkCheck(VK10.vkBindImageMemory(device.getVkDevice(), vkImage, vkMemory, 0),
                    "Failed to bind image memory");
        }
    }

    public void cleanup() {
        VK10.vkDestroyImage(device.getVkDevice(), vkImage, null);
        VK10.vkFreeMemory(device.getVkDevice(), vkMemory, null);
    }

    public int getFormat() {
        return format;
    }

    public int getMipLevels() {
        return mipLevels;
    }

    public long getVkImage() {
        return vkImage;
    }

    public long getVkMemory() {
        return vkMemory;
    }

    public static class ImageData {

        public int format, mipLevels, sampleCount, arrayLayers, width, height, usage;

        public ImageData() {
            this.format = VK10.VK_FORMAT_R8G8B8A8_SRGB;
            this.mipLevels = 1;
            this.sampleCount = 1;
            this.arrayLayers = 1;
        }

        public ImageData arrayLayers(int arrayLayers) {
            this.arrayLayers = arrayLayers;
            return this;
        }

        public ImageData format(int format) {
            this.format = format;
            return this;
        }

        public ImageData height(int height) {
            this.height = height;
            return this;
        }

        public ImageData mipLevels(int mipLevels) {
            this.mipLevels = mipLevels;
            return this;
        }

        public ImageData sampleCount(int sampleCount) {
            this.sampleCount = sampleCount;
            return this;
        }

        public ImageData usage(int usage) {
            this.usage = usage;
            return this;
        }

        public ImageData width(int width) {
            this.width = width;
            return this;
        }

    }

}
