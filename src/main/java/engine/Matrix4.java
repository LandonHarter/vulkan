package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Matrix4 {

    public static Matrix4f CalculateTransform(Vector3f position) {
        Matrix4f transform = new Matrix4f();
        transform.translate(position);
        return transform;
    }

}
