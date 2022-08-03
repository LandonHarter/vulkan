package engine;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;

public class Window {

    private static long window;
    private static int width, height;

    private static boolean resized;

    public static void CreateWindow() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);

        window = GLFW.glfwCreateWindow(1920, 1080, "Vulkan", 0, 0);
        width = 1920;
        height = 1080;

        GLFW.glfwSetFramebufferSizeCallback(window, (window, w, h) -> Resize(w, h));
    }

    public static void DestroyWindow() {
        Callbacks.glfwFreeCallbacks(window);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void Resize(int w, int h) {
        resized = true;
        width = w;
        height = h;
    }

    public static void ResetResized() {
        resized = false;
    }

    public static boolean IsResized() {
        return resized;
    }

    public static void SetResized(boolean res) {
        resized = res;
    }

    public static boolean ShouldClose() {
        return GLFW.glfwWindowShouldClose(window);
    }

    public static int GetWidth() {
        return width;
    }

    public static int GetHeight() {
        return height;
    }

    public static long GetWindow() {
        return window;
    }

}
