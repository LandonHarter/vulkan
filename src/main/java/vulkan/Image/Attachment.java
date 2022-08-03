package vulkan.Image;

import org.lwjgl.vulkan.VK10;
import vulkan.Devices.Device;

public class Attachment {

    private final Image image;
    private final ImageView imageView;

    private boolean depthAttachment;

    public Attachment(Device device, int width, int height, int format, int usage) {
        Image.ImageData imageData = new Image.ImageData().width(width).height(height).
                usage(usage | VK10.VK_IMAGE_USAGE_SAMPLED_BIT).
                format(format);
        image = new Image(device, imageData);

        int aspectMask = 0;
        if ((usage & VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) > 0) {
            aspectMask = VK10.VK_IMAGE_ASPECT_COLOR_BIT;
            depthAttachment = false;
        }
        if ((usage & VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) > 0) {
            aspectMask = VK10.VK_IMAGE_ASPECT_DEPTH_BIT;
            depthAttachment = true;
        }

        ImageView.ImageViewData imageViewData = new ImageView.ImageViewData().format(image.getFormat()).aspectMask(aspectMask);
        imageView = new ImageView(device, image.getVkImage(), imageViewData);
    }

    public void cleanup() {
        imageView.cleanup();
        image.cleanup();
    }

    public Image getImage() {
        return image;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public boolean isDepthAttachment() {
        return depthAttachment;
    }

}
