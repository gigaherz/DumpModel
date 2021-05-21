package gigaherz.dumpmodel;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mojang.datafixers.util.Pair;
import gigaherz.dumpmodel.builders.*;
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

public class VertexDumper implements IRenderTypeBuffer
{
    public List<Pair<RenderType, VertexAccumulator>> parts = Lists.newArrayList();

    public final IBuilder<?,?,?,?,?> builder;

    public VertexDumper(IBuilder<?, ?, ?, ?, ?> builder)
    {
        this.builder = builder;
    }

    public void dumpToOBJ(File file, String name)
    {
        IBuilderPart<?,?,?,?,?> partBuilder = builder.part(name);

        int partNumber = 0;
        for (Pair<RenderType, VertexAccumulator> part : parts)
        {
            RenderType rt = part.getFirst();
            VertexAccumulator acc = part.getSecond();

            IBuilderGroup<?, ?, ?, ?, ?> groupBuilder = partBuilder.group(String.format("Part_%s\n", ++partNumber), null);

            VertexFormat fmt = rt.getVertexFormat();
            int drawMode = rt.getDrawMode();
            int verticesPerElement;
            if (drawMode == GL11.GL_QUADS)
                verticesPerElement = 4;
            else if (drawMode == GL11.GL_TRIANGLES)
                verticesPerElement = 3;
            else
                throw new RuntimeException(String.format("Unsupported GL drawing mode %s", drawMode));

            IBuilderFace<?, ?, ?, ?, ?> faceBuilder = groupBuilder.face();
            int vertexNumber = 0;
            for (VertexAccumulator.VertexData data : acc.vertices)
            {
                IBuilderVertex<?, ?, ?, ?, ?> vertexBuilder = faceBuilder.vertex();
                for (VertexFormatElement element : fmt.getElements())
                {
                    if (element.getUsage() != VertexFormatElement.Usage.PADDING)
                    {
                        int index = element.getIndex();
                        switch (element.getUsage())
                        {
                            case POSITION:
                                vertexBuilder = vertexBuilder.element(element, data.pos.x,data.pos.y,data.pos.z);
                                break;
                            case UV:
                                vertexBuilder = vertexBuilder.element(element, data.uv[index].x, data.uv[index].y);
                                break;
                            case NORMAL:
                                vertexBuilder = vertexBuilder.element(element, data.normal.getX(),data.normal.getY(),data.normal.getZ());
                                break;
                            case COLOR:
                                vertexBuilder = vertexBuilder.element(element, data.color[0], data.color[1], data.color[2], data.color[3]);
                                break;
                        }
                    }
                }
                faceBuilder = vertexBuilder.end();

                if (++vertexNumber >= verticesPerElement)
                {
                    faceBuilder = faceBuilder.end().face();
                    vertexNumber = 0;
                }
            }
            partBuilder = faceBuilder.end().end();
        }

        builder.save(file);
    }

    @Override
    public IVertexBuilder getBuffer(RenderType rt)
    {
        VertexAccumulator acc = new VertexAccumulator();
        parts.add(Pair.of(rt, acc));
        return acc;
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
