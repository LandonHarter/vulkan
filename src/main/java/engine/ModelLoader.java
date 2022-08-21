package engine;

import org.lwjgl.assimp.*;

import java.nio.file.Files;
import java.nio.file.Paths;

public class ModelLoader {

    public static Mesh[] LoadModel(String modelPath) {
        AIScene scene = Assimp.aiImportFile(modelPath, Assimp.aiProcessPreset_TargetRealtime_MaxQuality);
        if (scene == null) {
            System.err.println("Model at " + modelPath + " not found");
            System.exit(1);
            return null;
        }

        Mesh[] meshes = new Mesh[scene.mNumMeshes()];
        int currentIndex = 0;

        AINode root = scene.mRootNode();
        LoadMesh(scene, root, meshes, currentIndex);

        Assimp.aiFreeScene(scene);

        return meshes;
    }

    private static void LoadMesh(AIScene scene, AINode node, Mesh[] meshes, int currentIndex) {
        int numOfMeshes = node.mNumMeshes();
        if (numOfMeshes > 0) {
            for (int i = 0; i < numOfMeshes; i++) {
                AIMesh mesh = AIMesh.create(scene.mMeshes().get(node.mMeshes().get(i)));
                AIVector3D.Buffer vertices = mesh.mVertices();

                float[] position = new float[mesh.mNumVertices() * 3];
                float[] texture = new float[mesh.mNumVertices() * 2];
                int[] indices = new int[mesh.mNumFaces() * 3];

                for (int j = 0; j < mesh.mNumVertices(); j++) {
                    position[j * 3] = vertices.get(j).x();
                    position[j * 3 + 1] = vertices.get(j).y();
                    position[j * 3 + 2] = vertices.get(j).z();

                    if (mesh.mNumUVComponents().get(0) != 0) {
                        AIVector3D texCoord = mesh.mTextureCoords(0).get(j);
                        texture[j * 2] = texCoord.x();
                        texture[j * 2 + 1] = texCoord.y();
                    }
                }

                int faceCount = mesh.mNumFaces();
                AIFace.Buffer indicesBuf = mesh.mFaces();
                for (int j = 0; j < faceCount; j++) {
                    AIFace face = indicesBuf.get(j);
                    indices[j * 3] = face.mIndices().get(0);
                    indices[j * 3 + 1] = face.mIndices().get(1);
                    indices[j * 3 + 2] = face.mIndices().get(2);
                }

                meshes[currentIndex] = new Mesh(position, texture, indices);
                currentIndex++;
            }
        }

        for (int i = 0; i < node.mNumChildren(); i++) {
            LoadMesh(scene, AINode.create(node.mChildren().get(i)), meshes, currentIndex);
        }
    }

    public static class Mesh {

        public float[] positions;
        public float[] texCoords;
        public int[] indices;

        public Mesh(float[] positions, float[] texCoords, int[] indices) {
            this.positions = positions;
            this.texCoords = texCoords;
            this.indices = indices;
        }

    }

}
