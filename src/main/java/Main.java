import engine.ModelLoader;
import engine.Window;
import org.lwjgl.glfw.GLFW;
import vulkan.Camera;
import vulkan.Model.ModelData;
import vulkan.Rendering.VulkanRenderer;
import vulkan.Vulkan;

import java.util.ArrayList;
import java.util.List;

public class Main {

    private static VulkanRenderer vulkanRenderer;

    protected static void Start() {
        Window.CreateWindow();
        Vulkan.VulkanSettings vk = new Vulkan.VulkanSettings();
        Vulkan.Create(vk);
        Camera.init();

        vulkanRenderer = new VulkanRenderer("resources/shaders/vert.glsl", "resources/shaders/frag.glsl");
        Vulkan.AddRenderer(vulkanRenderer);

        Initialize();

        while (!Window.ShouldClose()) {
            Update();
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
        ModelLoader.Mesh[] meshes = ModelLoader.LoadModel("resources/models/Sphere.fbx");
        List<ModelData.MeshData> meshDataList = new ArrayList<>();
        for (ModelLoader.Mesh mesh : meshes) {
            meshDataList.add(new ModelData.MeshData(mesh.positions, mesh.texCoords, mesh.indices));
        }
        ModelData modelData = new ModelData("Sphere", meshDataList);
        vulkanRenderer.addMesh(modelData);
    }

    public static void main(String[] args) {
        Start();
    }

}
