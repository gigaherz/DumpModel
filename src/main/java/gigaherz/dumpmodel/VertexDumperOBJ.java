package gigaherz.dumpmodel;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.*;
import java.util.List;

public class VertexDumperOBJ implements IRenderTypeBuffer
{
    public List<Pair<RenderType, VertexAccumulator>> parts = Lists.newArrayList();

    @Override
    public IVertexBuilder getBuffer(RenderType rt)
    {
        VertexAccumulator acc = new VertexAccumulator();
        parts.add(Pair.of(rt, acc));
        return acc;
    }

    public void dumpToOBJ(File file, String name)
    {
        try (OutputStream stream = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(stream))
        {
            int lastTex = 0;
            int lastPos = 0;
            int lastNorm = 0;
            int partNumber = 0;

            writer.write(String.format("o %s\n", name));

            for (Pair<RenderType, VertexAccumulator> part : parts)
            {
                RenderType rt = part.getFirst();
                VertexAccumulator acc = part.getSecond();
                writer.write(String.format("g %s_%s\n", rt.toString(), ++partNumber));

                VertexFormat fmt = rt.getVertexFormat();
                int drawMode = rt.getDrawMode();
                int verticesPerElement;
                if (drawMode == GL11.GL_QUADS)
                    verticesPerElement = 4;
                else if (drawMode == GL11.GL_TRIANGLES)
                    verticesPerElement = 3;
                else
                    throw new RuntimeException(String.format("Unsupported GL drawing mode %s", drawMode));

                List<List<Integer>> indices = Lists.newArrayList(
                        Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList()
                );
                int i = 0;
                for (VertexAccumulator.VertexData data : acc.vertices)
                {
                    boolean hasTex = false;
                    for (VertexFormatElement element : fmt.getElements())
                    {
                        if (element.getUsage() != VertexFormatElement.Usage.PADDING)
                        {
                            int index = element.getIndex();
                            switch (element.getUsage())
                            {
                                case POSITION:
                                    writer.write(String.format("v %s %s %s\n", data.pos.x,data.pos.y,data.pos.z));
                                    indices.get(i).add(++lastPos);
                                    break;
                                case UV:
                                    if (index == 0)
                                    {
                                        writer.write(String.format("vt %s %s\n", data.uv[0].x, data.uv[0].y));
                                        indices.get(i).add(++lastTex);
                                        hasTex = true;
                                    }
                                    else
                                    {
                                        writer.write(String.format("vt%s %s %s\n", index, data.uv[index].x, data.uv[index].y));
                                    }
                                    break;
                                case NORMAL:
                                    writer.write(String.format("vn %s %s %s\n", data.normal.getX(),data.normal.getY(),data.normal.getZ()));
                                    indices.get(i).add(++lastNorm);
                                    break;
                                case COLOR:
                                    writer.write(String.format("vColor %s %s %s %s\n", data.color[0], data.color[1], data.color[2], data.color[3]));
                                    indices.get(i).add(++lastNorm);
                                    break;
                            }
                        }
                    }

                    if (++i >= verticesPerElement)
                    {
                        if (verticesPerElement == 3)
                        {
                            writer.write(String.format("f %s %s %s\n",
                                    formatIndices(indices.get(0), hasTex),
                                    formatIndices(indices.get(1), hasTex),
                                    formatIndices(indices.get(2), hasTex)
                            ));
                        }
                        else
                        {
                            writer.write(String.format("f %s %s %s %s\n",
                                    formatIndices(indices.get(0), hasTex),
                                    formatIndices(indices.get(1), hasTex),
                                    formatIndices(indices.get(2), hasTex),
                                    formatIndices(indices.get(3), hasTex)
                            ));
                        }
                        for(int j=0;j<4;j++)
                            indices.get(j).clear();
                        i = 0;
                    }
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static String formatIndices(List<Integer> indices, boolean hasTex)
    {
        if (indices.size() == 1)
            return String.format("%d", indices.get(0));
        if (indices.size() == 2 && hasTex)
            return String.format("%d/%d", indices.get(0), indices.get(1));
        if (indices.size() == 2)
            return String.format("%d//%d", indices.get(0), indices.get(1));
        if (indices.size() >= 3)
            return String.format("%d/%d/%d", indices.get(0), indices.get(1), indices.get(2));
        return "1";
    }

    private class VertexAccumulator implements IVertexBuilder
    {
        public final List<VertexData> vertices = Lists.newArrayList();
        private VertexData current = new VertexData();

        @Override
        public IVertexBuilder pos(double x, double y, double z)
        {
            current.pos(x,y,z);
            return this;
        }

        @Override
        public IVertexBuilder color(int red, int green, int blue, int alpha)
        {
            current.color(red,green,blue,alpha);
            return this;
        }

        @Override
        public IVertexBuilder tex(float u, float v)
        {
            current.tex(u,v);
            return this;
        }

        @Override
        public IVertexBuilder overlay(int u, int v)
        {
            current.overlay(u,v);
            return this;
        }

        @Override
        public IVertexBuilder lightmap(int u, int v)
        {
            current.lightmap(u,v);
            return this;
        }

        @Override
        public IVertexBuilder normal(float x, float y, float z)
        {
            current.normal(x,y,z);
            return this;
        }

        @Override
        public void endVertex()
        {
            vertices.add(current);
            current = new VertexData();
        }

        private class VertexData
        {
            public Vector3d pos;
            public float[] color;
            public Vector2f[] uv = new Vector2f[3];
            public Vector3f normal;

            public void pos(double x, double y, double z)
            {
                this.pos = new Vector3d(x,y,z);
            }

            public void color(int red, int green, int blue, int alpha)
            {
                this.color = new float[]{red,green,blue,alpha};
            }

            public void tex(float u, float v)
            {
                this.uv[0] = new Vector2f(u,v);
            }

            public void overlay(int u, int v)
            {
                this.uv[1] = new Vector2f(u,v);
            }

            public void lightmap(int u, int v)
            {
                this.uv[2] = new Vector2f(u, v);
            }

            public void normal(float x, float y, float z)
            {
                this.normal = new Vector3f(x,y,z);
            }
        }
    }
}
