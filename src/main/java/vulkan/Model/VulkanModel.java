package vulkan.Model;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.VkBufferCopy;
import org.tinylog.Logger;
import vulkan.Commands.CommandBuffer;
import vulkan.Commands.CommandPool;
import vulkan.Devices.Device;
import vulkan.Fence;
import vulkan.GraphConstants;
import vulkan.Queue;
import vulkan.VulkanBuffer;

import java.nio.*;
import java.util.*;

import static org.lwjgl.vulkan.VK11.*;

public class VulkanModel {

    private final String modelId;
    private final List<VulkanMesh> vulkanMeshList;

    public VulkanModel(String modelId) {
        this.modelId = modelId;
        vulkanMeshList = new ArrayList<>();
    }

    private static TransferBuffers createIndicesBuffers(Device device, ModelData.MeshData meshData) {
        int[] indices = meshData.indices();
        int numIndices = indices.length;
        int bufferSize = numIndices * GraphConstants.INT_LENGTH;

        VulkanBuffer srcBuffer = new VulkanBuffer(device, bufferSize,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        VulkanBuffer dstBuffer = new VulkanBuffer(device, bufferSize,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

        long mappedMemory = srcBuffer.map();
        IntBuffer data = MemoryUtil.memIntBuffer(mappedMemory, (int) srcBuffer.getRequestedSize());
        data.put(indices);
        srcBuffer.unMap();

        return new TransferBuffers(srcBuffer, dstBuffer);
    }

    private static TransferBuffers createVerticesBuffers(Device device, ModelData.MeshData meshData) {
        float[] vertices = meshData.positions();
        float[] textureCoords = meshData.textureCoords();
        if (textureCoords == null || textureCoords.length == 0) {
            textureCoords = new float[(vertices.length / 3) * 2];
        }
        int numElements = vertices.length + textureCoords.length;
        int bufferSize = numElements * GraphConstants.FLOAT_LENGTH;

        VulkanBuffer srcBuffer = new VulkanBuffer(device, bufferSize,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        VulkanBuffer dstBuffer = new VulkanBuffer(device, bufferSize,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

        long mappedMemory = srcBuffer.map();
        FloatBuffer data = MemoryUtil.memFloatBuffer(mappedMemory, (int) srcBuffer.getRequestedSize());

        int rows = vertices.length / 3;
        for (int row = 0; row < rows; row++) {
            int startPos = row * 3;
            int startTextCoord = row * 2;
            data.put(vertices[startPos]);
            data.put(vertices[startPos + 1]);
            data.put(vertices[startPos + 2]);
            data.put(textureCoords[startTextCoord]);
            data.put(textureCoords[startTextCoord + 1]);
        }

        srcBuffer.unMap();

        return new TransferBuffers(srcBuffer, dstBuffer);
    }

    private static void recordTransferCommand(CommandBuffer cmd, TransferBuffers transferBuffers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCopy.Buffer copyRegion = VkBufferCopy.calloc(1, stack)
                    .srcOffset(0).dstOffset(0).size(transferBuffers.srcBuffer().getRequestedSize());
            vkCmdCopyBuffer(cmd.getVkCommandBuffer(), transferBuffers.srcBuffer().getBuffer(),
                    transferBuffers.dstBuffer().getBuffer(), copyRegion);
        }
    }

    public static VulkanModel transformModels(ModelData modelData, CommandPool commandPool, Queue queue) {
        Device device = commandPool.getDevice();
        CommandBuffer cmd = new CommandBuffer(commandPool, true, true);
        List<VulkanBuffer> stagingBufferList = new ArrayList<>();

        cmd.beginRecording();

        VulkanModel vulkanModel = new VulkanModel(modelData.getModelId());
        for (ModelData.MeshData meshData : modelData.getMeshDataList()) {
            TransferBuffers verticesBuffers = createVerticesBuffers(device, meshData);
            TransferBuffers indicesBuffers = createIndicesBuffers(device, meshData);
            stagingBufferList.add(verticesBuffers.srcBuffer());
            stagingBufferList.add(indicesBuffers.srcBuffer());
            recordTransferCommand(cmd, verticesBuffers);
            recordTransferCommand(cmd, indicesBuffers);

            VulkanModel.VulkanMesh vulkanMesh = new VulkanModel.VulkanMesh(verticesBuffers.dstBuffer(),
                    indicesBuffers.dstBuffer(), meshData.indices().length);

            vulkanModel.vulkanMeshList.add(vulkanMesh);
        }

        cmd.endRecording();
        Fence fence = new Fence(device, true);
        fence.reset();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            queue.submit(stack.pointers(cmd.getVkCommandBuffer()), null, null, null, fence);
        }
        fence.fenceWait();
        fence.cleanup();
        cmd.cleanup();

        stagingBufferList.forEach(VulkanBuffer::cleanup);

        return vulkanModel;
    }

    public static List<VulkanModel> transformModels(List<ModelData> modelDataList, CommandPool commandPool, Queue queue) {
        List<VulkanModel> vulkanModelList = new ArrayList<>();
        Device device = commandPool.getDevice();
        CommandBuffer cmd = new CommandBuffer(commandPool, true, true);
        List<VulkanBuffer> stagingBufferList = new ArrayList<>();

        cmd.beginRecording();

        for (ModelData modelData : modelDataList) {
            VulkanModel vulkanModel = new VulkanModel(modelData.getModelId());
            vulkanModelList.add(vulkanModel);

            for (ModelData.MeshData meshData : modelData.getMeshDataList()) {
                TransferBuffers verticesBuffers = createVerticesBuffers(device, meshData);
                TransferBuffers indicesBuffers = createIndicesBuffers(device, meshData);
                stagingBufferList.add(verticesBuffers.srcBuffer());
                stagingBufferList.add(indicesBuffers.srcBuffer());
                recordTransferCommand(cmd, verticesBuffers);
                recordTransferCommand(cmd, indicesBuffers);

                VulkanModel.VulkanMesh vulkanMesh = new VulkanModel.VulkanMesh(verticesBuffers.dstBuffer(),
                        indicesBuffers.dstBuffer(), meshData.indices().length);

                vulkanModel.vulkanMeshList.add(vulkanMesh);
            }
        }

        cmd.endRecording();
        Fence fence = new Fence(device, true);
        fence.reset();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            queue.submit(stack.pointers(cmd.getVkCommandBuffer()), null, null, null, fence);
        }
        fence.fenceWait();
        fence.cleanup();
        cmd.cleanup();

        stagingBufferList.forEach(VulkanBuffer::cleanup);

        return vulkanModelList;
    }

    public void cleanup() {
        vulkanMeshList.forEach(VulkanMesh::cleanup);
    }

    public void setPosition(Vector3f position) {
        vulkanMeshList.forEach(mesh -> mesh.setPosition(position));
    }

    public void setRotation(Vector3f rotation) {
        vulkanMeshList.forEach(mesh -> mesh.setRotation(rotation));
    }

    public void setScale(Vector3f scale) {
        vulkanMeshList.forEach(mesh -> mesh.setScale(scale));
    }

    public void translate(Vector3f translation) {
        vulkanMeshList.forEach(mesh -> mesh.translate(translation));
    }

    public void rotate(Vector3f rotation) {
        vulkanMeshList.forEach(mesh -> mesh.rotate(rotation));
    }

    public void scale(Vector3f scale) {
        vulkanMeshList.forEach(mesh -> mesh.scale(scale));
    }

    public String getModelId() {
        return modelId;
    }

    public List<VulkanModel.VulkanMesh> getVulkanMeshList() {
        return vulkanMeshList;
    }

    private record TransferBuffers(VulkanBuffer srcBuffer, VulkanBuffer dstBuffer) {
    }

    public static class VulkanMesh {

        public VulkanBuffer verticesBuffer, indicesBuffer;
        public int numIndices;

        private Vector3f position = new Vector3f(), rotation = new Vector3f();
        private Vector3f scale = new Vector3f(1);

        private Matrix4f modelMatrix = new Matrix4f().identity();

        public VulkanMesh(VulkanBuffer verticesBuffer, VulkanBuffer indicesBuffer, int numIndices) {
            this.verticesBuffer = verticesBuffer;
            this.indicesBuffer = indicesBuffer;
            this.numIndices = numIndices;
        }

        public void cleanup() {
            verticesBuffer.cleanup();
            indicesBuffer.cleanup();
        }

        public void setPosition(Vector3f position) {
            this.position = position;
            calculateModelMatrix();
        }

        public void setRotation(Vector3f rotation) {
            this.rotation = rotation;
            calculateModelMatrix();
        }

        public void setScale(Vector3f scale) {
            this.scale = scale;
            calculateModelMatrix();
        }

        public void translate(Vector3f translation) {
            position = position.add(translation);
            calculateModelMatrix();
        }

        public void rotate(Vector3f rotation) {
            this.rotation = this.rotation.add(rotation);
            calculateModelMatrix();
        }

        public void scale(Vector3f scale) {
            this.scale = this.scale.mul(scale);
            calculateModelMatrix();
        }

        public Matrix4f getModelMatrix() {
            return modelMatrix;
        }

        private void calculateModelMatrix() {
            modelMatrix = new Matrix4f().identity();
            modelMatrix.translate(position);
            modelMatrix.rotate((float) Math.toRadians(rotation.x), new Vector3f(1, 0, 0));
            modelMatrix.rotate((float) Math.toRadians(rotation.y), new Vector3f(0, 1, 0));
            modelMatrix.rotate((float) Math.toRadians(rotation.z), new Vector3f(0, 0, 1));
            modelMatrix.scale(scale);
        }

    }
}