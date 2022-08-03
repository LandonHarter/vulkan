package vulkan;

import vulkan.Model.ModelData;
import vulkan.Rendering.Render;
import vulkan.Rendering.VulkanRenderer;

import java.util.List;

public class Vulkan {

    public static Render render;

    public static void Create(VulkanSettings settings) {
        render = new Render(settings);
    }

    public static void Render() {
        render.render();
    }

    public static void Destroy() {
        render.cleanup();
    }

    public static void AddRenderer(VulkanRenderer renderer) {
        render.renderers.add(renderer);
    }

    public static class VulkanSettings {

        public boolean validate;
        public boolean vSync = false;

        public VulkanSettings() {
            this(true, true);
        }

        public VulkanSettings(boolean validate, boolean vSync) {
            this.validate = validate;
            this.vSync = vSync;
        }

    }

}
