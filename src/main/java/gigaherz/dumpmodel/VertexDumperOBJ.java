package gigaherz.dumpmodel;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.io.*;
import java.util.List;

public class VertexDumperOBJ implements MultiBufferSource
{
    public List<Pair<RenderType, VertexAccumulator>> parts = Lists.newArrayList();

    @Override
    public VertexConsumer getBuffer(RenderType rt)
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

                VertexFormat fmt = rt.format();
                VertexFormat.Mode drawMode = rt.mode();
                int verticesPerElement;
                if (drawMode == VertexFormat.Mode.QUADS)
                    verticesPerElement = 4;
                else if (drawMode == VertexFormat.Mode.TRIANGLES)
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
                                    writer.write(String.format("v %s %s %s\n", data.pos.x, data.pos.y, data.pos.z));
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
                                    writer.write(String.format("vn %s %s %s\n", data.normal.x(), data.normal.y(), data.normal.z()));
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
                        for (int j = 0; j < 4; j++)
                        {indices.get(j).clear();}
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
}
