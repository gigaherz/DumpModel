package gigaherz.dumpmodel.builders.writers;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import gigaherz.dumpmodel.builders.ModelGroup;
import gigaherz.dumpmodel.builders.ModelMesh;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GltfModelWriter extends ModelWriter<GltfModelWriter>
{
    public static GltfModelWriter begin()
    {
        return new GltfModelWriter();
    }

    private GltfModelWriter()
    {
    }

    private JsonObject save1(Path file) throws IOException
    {
        var obj = new JsonObject();

        var asset = new JsonObject();
        obj.add("asset", asset);

        asset.addProperty("generator", "DumpModel v1.0"); // TODO: mod version
        asset.addProperty("version", "2.0");

        obj.addProperty("scene", 0);

        var scenes = new JsonArray();
        obj.add("scenes", scenes);

        var scene = new JsonObject();
        scenes.add(scene);

        scene.addProperty("name", "Scene");

        var nodeIndices = new JsonArray();
        scene.add("nodes", nodeIndices);

        var nodes = new JsonArray();
        obj.add("nodes", nodes);

        var materials = new JsonArray();
        obj.add("materials", materials);

        var textures = new JsonArray();
        obj.add("textures", textures);

        var images = new JsonArray();
        obj.add("images", images);

        var imageMap = new HashMap<String, Integer>();
        var materialMap = new HashMap<String, Integer>();
        for (var entry : materialLibrary().entrySet())
        {
            var mat0 = entry.getValue();

            materialMap.put(entry.getKey(), materials.size());

            var material = new JsonObject();
            materials.add(material);
            switch (mat0.mat().alphaMode())
            {
                case OPAQUE:
                    break;
                case CUTOUT:
                    material.addProperty("alphaMode", "MASK");
                    material.addProperty("alphaCutoff", 0.5);
                    break;
                case BLEND:
                    material.addProperty("alphaMode", "BLEND");
                    break;
            }
            material.addProperty("doubleSided", mat0.mat().doubleSided());
            material.addProperty("name", entry.getKey());

            var pbrMetallicRoughness = new JsonObject();
            material.add("pbrMetallicRoughness", pbrMetallicRoughness);

            var baseColorTexture = new JsonObject();
            pbrMetallicRoughness.add("baseColorTexture", baseColorTexture);

            baseColorTexture.addProperty("index", imageMap.computeIfAbsent(mat0.mat().texture(), tx -> {
                var imageIndex = images.size();

                var texture = new JsonObject();
                textures.add(texture);
                texture.addProperty("sampler", 0);
                texture.addProperty("source", images.size());

                var image = new JsonObject();
                images.add(image);
                image.addProperty("mimeType", "image/png");
                image.addProperty("name", "brush" + images.size());

                var texPath = Paths.get(mat0.mat().texture());

                var rel = file.getParent().relativize(texPath);

                image.addProperty("uri", rel.toString().replaceAll("\\\\", "/"));

                return imageIndex;
            }));

            pbrMetallicRoughness.addProperty("metallicFactor", 0.67);
            pbrMetallicRoughness.addProperty("roughnessFactor", 0.98);
        }

        var meshes = new JsonArray();
        obj.add("meshes", meshes);

        var accessors = new JsonArray();
        obj.add("accessors", accessors);

        var bufferViews = new JsonArray();
        obj.add("bufferViews", bufferViews);

        var bufferData = new ByteArrayOutputStream();

        var buffers = new JsonArray();
        obj.add("buffers", buffers);

        var samplers = new JsonArray();
        obj.add("samplers", samplers);

        var sampler = new JsonObject();
        samplers.add(sampler);

        sampler.addProperty("magFilter", GL11.GL_NEAREST);
        sampler.addProperty("minFilter", GL11.GL_NEAREST);

        for (var group : groups())
        {
            for (var mesh : group.meshes())
            {
                saveMesh(group, mesh, nodes, nodeIndices, meshes, accessors, bufferViews, materialMap, bufferData);
            }
        }

        var buffer = new JsonObject();
        buffers.add(buffer);

        var bytes = bufferData.toByteArray();

        buffer.addProperty("byteLength", bytes.length);

        var base64 = "data:application/octet-stream;base64," + Base64.getEncoder().encodeToString(bytes);

        buffer.addProperty("uri", base64);

        return obj;
    }

    private void saveMesh(ModelGroup<GltfModelWriter> group, ModelMesh<GltfModelWriter> mesh0, JsonArray nodes, JsonArray nodeIndices, JsonArray meshes, JsonArray accessors, JsonArray bufferViews, Map<String, Integer> materialMap, ByteArrayOutputStream bufferData) throws IOException
    {
        var node = new JsonObject();
        var index = nodes.size();
        nodes.add(node);
        nodeIndices.add(index);

        // Entities should use local origin + translation/rotation/scale: "translation": [ x, y, z]

        node.addProperty("name", mesh0.name());

        // mesh
        var mesh = new JsonObject();
        var index1 = meshes.size();
        meshes.add(mesh);
        node.addProperty("mesh", index1);

        mesh.addProperty("name", mesh0.name());

        var primitives = new JsonArray();
        mesh.add("primitives", primitives);

        var primitive = new JsonObject();
        primitives.add(primitive);

        var attributes = new JsonObject();
        primitive.add("attributes", attributes);

        var indices = new ArrayList<Integer>();
        var minPos = new double[3];
        var maxPos = new double[3];
        var positions = new ArrayList<Double>();
        var colors = new ArrayList<Double>();
        var texcoords = new ArrayList<Double>();
        var normals = new ArrayList<Double>();

        for (var face : mesh0.faces())
        {
            var vertexList = face.vertices();

            if (vertexList.size() == 3)
            {
                var offset = positions.size() / 3;
                indices.add(offset + 0);
                indices.add(offset + 1);
                indices.add(offset + 2);
            }
            else if (vertexList.size() >= 4)
            {
                var offset = positions.size() / 3;
                indices.add(offset + 0);
                indices.add(offset + 1);
                indices.add(offset + 2);
                indices.add(offset + 0);
                indices.add(offset + 2);
                indices.add(offset + 3);
            }

            for (var vtx : vertexList)
            {
                boolean hasP = vtx.indices().containsKey(VertexFormatElement.POSITION);
                boolean hasT = vtx.indices().containsKey(VertexFormatElement.UV0);
                boolean hasN = vtx.indices().containsKey(VertexFormatElement.NORMAL);
                boolean hasC = vtx.indices().containsKey(VertexFormatElement.COLOR);

                var p = hasP ? elementDatas().get(VertexFormatElement.POSITION).get(vtx.indices().get(VertexFormatElement.POSITION)) : new double[]{0, 0, 0};
                var t = hasT ? elementDatas().get(VertexFormatElement.UV0).get(vtx.indices().get(VertexFormatElement.UV0)) : new double[]{0, 0};
                var n = hasN ? elementDatas().get(VertexFormatElement.NORMAL).get(vtx.indices().get(VertexFormatElement.NORMAL)) : new double[]{0, 0, 0};
                var c = hasC ? elementDatas().get(VertexFormatElement.COLOR).get(vtx.indices().get(VertexFormatElement.COLOR)) : new double[]{0, 0, 0, 0};

                if (positions.size() == 0)
                {
                    minPos[0] = p[0];
                    minPos[1] = p[1];
                    minPos[2] = p[2];
                    maxPos[0] = p[0];
                    maxPos[1] = p[1];
                    maxPos[2] = p[2];
                }
                else
                {
                    minPos[0] = Math.min(minPos[0], p[0]);
                    minPos[1] = Math.min(minPos[1], p[1]);
                    minPos[2] = Math.min(minPos[2], p[2]);
                    maxPos[0] = Math.max(maxPos[0], p[0]);
                    maxPos[1] = Math.max(maxPos[1], p[1]);
                    maxPos[2] = Math.max(maxPos[2], p[2]);
                }

                Arrays.stream(p).forEach(positions::add);
                Arrays.stream(t).forEach(texcoords::add);
                Arrays.stream(n).forEach(normals::add);
                Arrays.stream(c).forEach(colors::add);
            }
        }

        byte[] positionData;
        {
            var buffer = Unpooled.buffer();
            positions.forEach(d -> buffer.writeFloatLE((float) (double) d));
            positionData = ByteBufUtil.getBytes(buffer);
            buffer.release();
        }

        byte[] colorData;
        {
            var buffer = Unpooled.buffer();
            colors.forEach(d -> buffer.writeByte((byte) (double) d));
            colorData = ByteBufUtil.getBytes(buffer);
            buffer.release();
        }

        byte[] uvData;
        {
            var buffer = Unpooled.buffer();
            texcoords.forEach(d -> buffer.writeFloatLE((float) (double) d));
            uvData = ByteBufUtil.getBytes(buffer);
            buffer.release();
        }

        byte[] normalData;
        {
            var buffer = Unpooled.buffer();
            normals.forEach(d -> buffer.writeFloatLE((float) (double) d));
            normalData = ByteBufUtil.getBytes(buffer);
            buffer.release();
        }

        byte[] indexData;
        {
            var buffer = Unpooled.buffer();
            if (indices.size() > 65536)
                indices.forEach(buffer::writeIntLE);
            else
                indices.forEach(buffer::writeShortLE);
            indexData = ByteBufUtil.getBytes(buffer);
            buffer.release();
        }

        var vertexCount = positions.size() / 3;
        var indexCount = indices.size();
        int posIndex = addPositionAccessor(accessors, bufferViews, bufferData, positionData, vertexCount, minPos, maxPos);
        int colorIndex = addColorAccessor(accessors, bufferViews, bufferData, colorData, vertexCount);
        int uvIndex = addTexCoord0Accessor(accessors, bufferViews, bufferData, uvData, vertexCount);
        int normalIndex = addNormalAccessor(accessors, bufferViews, bufferData, normalData, vertexCount);
        int indexIndex = addIndexAccessor(accessors, bufferViews, bufferData, indexData, indexCount);

        attributes.addProperty("POSITION", posIndex);
        attributes.addProperty("COLOR_0", colorIndex);
        attributes.addProperty("TEXCOORD_0", uvIndex);
        attributes.addProperty("NORMAL", normalIndex);

        primitive.addProperty("indices", indexIndex);
        primitive.addProperty("material", materialMap.getOrDefault(mesh0.material().name(), 0));
    }

    private int addBufferView(JsonArray bufferViews, ByteArrayOutputStream bufferData, byte[] data, int target) throws IOException
    {
        var bufferIndex = bufferViews.size();

        var bufferView = new JsonObject();
        bufferViews.add(bufferView);

        bufferView.addProperty("buffer", 0);
        bufferView.addProperty("byteOffset", bufferData.size());
        bufferView.addProperty("byteLength", data.length);
        bufferView.addProperty("target", target);

        bufferData.write(data);
        return bufferIndex;
    }


    private int addIndexAccessor(JsonArray accessors, JsonArray bufferViews, ByteArrayOutputStream bufferData, byte[] data, int indexCount) throws IOException
    {
        int bufferIndex = addBufferView(bufferViews, bufferData, data, 34963 /* element array buffer */);

        var index = accessors.size();

        var accessor = new JsonObject();
        accessors.add(accessor);

        accessor.addProperty("bufferView", bufferIndex);
        accessor.addProperty("componentType", indexCount <= 65535 ? GL11.GL_UNSIGNED_SHORT : GL11.GL_UNSIGNED_INT);
        accessor.addProperty("count", indexCount);
        accessor.addProperty("type", "SCALAR");

        return index;
    }

    private int addPositionAccessor(JsonArray accessors, JsonArray bufferViews, ByteArrayOutputStream bufferData, byte[] data, int vertexCount, double[] minPos, double[] maxPos) throws IOException
    {
        int bufferIndex = addBufferView(bufferViews, bufferData, data, 34962 /* array buffer */);

        var index = accessors.size();

        var accessor = new JsonObject();
        accessors.add(accessor);

        accessor.addProperty("bufferView", bufferIndex);
        accessor.addProperty("componentType", GL11.GL_FLOAT);
        accessor.addProperty("count", vertexCount);
        accessor.addProperty("type", "VEC3");

        var min = new JsonArray();
        accessor.add("min", min);
        min.add(minPos[0]);
        min.add(minPos[1]);
        min.add(minPos[2]);

        var max = new JsonArray();
        accessor.add("max", max);
        max.add(maxPos[0]);
        max.add(maxPos[1]);
        max.add(maxPos[2]);

        return index;
    }

    private int addColorAccessor(JsonArray accessors, JsonArray bufferViews, ByteArrayOutputStream bufferData, byte[] data, int vertexCount) throws IOException
    {
        int bufferIndex = addBufferView(bufferViews, bufferData, data, 34962 /* array buffer */);

        var index = accessors.size();

        var accessor = new JsonObject();
        accessors.add(accessor);

        accessor.addProperty("bufferView", bufferIndex);
        accessor.addProperty("componentType", GL11.GL_UNSIGNED_BYTE);
        accessor.addProperty("count", vertexCount);
        accessor.addProperty("type", "VEC4");
        accessor.addProperty("normalized", true);

        return index;
    }

    private int addNormalAccessor(JsonArray accessors, JsonArray bufferViews, ByteArrayOutputStream bufferData, byte[] data, int vertexCount) throws IOException
    {
        int bufferIndex = addBufferView(bufferViews, bufferData, data, 34962 /* array buffer */);

        var index = accessors.size();

        var accessor = new JsonObject();
        accessors.add(accessor);

        accessor.addProperty("bufferView", bufferIndex);
        accessor.addProperty("componentType", GL11.GL_FLOAT);
        accessor.addProperty("count", vertexCount);
        accessor.addProperty("type", "VEC3");

        return index;
    }

    private int addTexCoord0Accessor(JsonArray accessors, JsonArray bufferViews, ByteArrayOutputStream bufferData, byte[] data, int vertexCount) throws IOException
    {
        int bufferIndex = addBufferView(bufferViews, bufferData, data, 34962 /* array buffer */);

        var index = accessors.size();

        var accessor = new JsonObject();
        accessors.add(accessor);

        accessor.addProperty("bufferView", bufferIndex);
        accessor.addProperty("componentType", GL11.GL_FLOAT);
        accessor.addProperty("count", vertexCount);
        accessor.addProperty("type", "VEC2");

        return index;
    }

    public void save(Path file)
    {
        try (OutputStream output = new FileOutputStream(file.toFile());
             OutputStreamWriter writer = new OutputStreamWriter(output))
        {
            (new GsonBuilder()).setPrettyPrinting().create()
                    .toJson(save1(file), new JsonWriter(writer));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
