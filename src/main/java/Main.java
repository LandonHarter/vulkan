import engine.ModelLoader;
import engine.Window;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import vulkan.Camera;
import vulkan.Model.ModelData;
import vulkan.Model.VulkanModel;
import vulkan.Rendering.VulkanRenderer;
import vulkan.Vulkan;

import java.util.ArrayList;
import java.util.List;

public class Main {

    private static VulkanRenderer vulkanRenderer;
    private static VulkanModel sponza;

    private static long fpsTime;
    private static int fps;

    protected static void Start() {
        Window.CreateWindow();
        Vulkan.VulkanSettings vk = new Vulkan.VulkanSettings();
        vk.vSync = false;
        Vulkan.Create(vk);
        Camera.init();

        vulkanRenderer = new VulkanRenderer("resources/shaders/vert.glsl", "resources/shaders/frag.glsl");
        Vulkan.AddRenderer(vulkanRenderer);

        Initialize();

        float begin = getTime();
        float end, delta;
        while (!Window.ShouldClose()) {
            Update();

            end = getTime();
            begin = end;
            delta = end - begin;

            fps++;
            if (System.currentTimeMillis() > fpsTime + 1000) {
                Window.SetTitle(Window.title + " | " + fps + "fps");
                fpsTime = System.currentTimeMillis();
                fps = 0;
            }
        }

        Vulkan.Destroy();
        Window.DestroyWindow();
    }

    protected static void Update() {
        Camera.calculateView();
        GLFW.glfwPollEvents();
        Vulkan.Render();
    }

    protected static void Initialize() {
        ModelLoader.Mesh[] meshes = ModelLoader.LoadModel("resources/models/Sponza/Sponza.gltf");
        List<ModelData.MeshData> meshDataList = new ArrayList<>();
        for (ModelLoader.Mesh mesh : meshes) {
            meshDataList.add(new ModelData.MeshData(mesh.positions, mesh.texCoords, mesh.indices));
        }
        ModelData modelData = new ModelData("Sponza", meshDataList);
        sponza = vulkanRenderer.addMesh(modelData);
        sponza.setPosition(new Vector3f(0, 0, -25));
        sponza.setRotation(new Vector3f(90, 0, 0));
        sponza.setScale(new Vector3f(0.005f));
    }

    private static final float timeStarted = System.nanoTime();
    private static float getTime() {
        return (float)((System.nanoTime() - timeStarted) * 1E-9);
    }

    public static void main(String[] args) {
        Start();
    }

}
