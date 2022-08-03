package vulkan;

import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {

    public static Matrix4f projection, view;
    public static Vector3f position, rotation;

    public static void init() {
        projection = new Matrix4f();
        view = new Matrix4f();

        position = new Vector3f(0, 0, 0);
        rotation = new Vector3f(0, 0, 0);

        calculateProjection();
        calculateView();
    }

    public static void calculateProjection() {
        projection.identity();
        projection.perspective((float)Math.toRadians(70), 16f / 9f, 0.1f, 100f);
    }

    public static void calculateView() {
        view.identity();
        view.translate(-position.x, -position.y, -position.z);
        view.rotate((float)Math.toRadians(rotation.x), new Vector3f(1, 0, 0));
        view.rotate((float)Math.toRadians(rotation.y), new Vector3f(0, 1, 0));
        view.rotate((float)Math.toRadians(rotation.z), new Vector3f(0, 0, 1));
    }

}
