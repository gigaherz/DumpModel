package gigaherz.dumpmodel;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.datafixers.util.Pair;
import gigaherz.dumpmodel.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.io.File;
import java.util.List;

public class VertexDumper implements MultiBufferSource
{
    public List<Pair<RenderType, VertexAccumulator>> parts = Lists.newArrayList();

    public final IBuilder<?, ?, ?, ?, ?> builder;

    public VertexDumper(IBuilder<?, ?, ?, ?, ?> builder)
    {
        this.builder = builder;
    }

    public void dumpToOBJ(File file, String name)
    {
        IBuilderPart<?, ?, ?, ?, ?> partBuilder = builder.part(name);

        int partNumber = 0;
        for (Pair<RenderType, VertexAccumulator> part : parts)
        {
            RenderType rt = part.getFirst();
            VertexAccumulator acc = part.getSecond();

            IBuilderGroup<?, ?, ?, ?, ?> groupBuilder = partBuilder.group(String.format("Part_%s\n", ++partNumber), null);

            VertexFormat fmt = rt.format();
            VertexFormat.Mode drawMode = rt.mode();
            int verticesPerElement;
            if (drawMode == VertexFormat.Mode.QUADS)
                verticesPerElement = 4;
            else if (drawMode == VertexFormat.Mode.TRIANGLES)
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
                        vertexBuilder = switch (element.getUsage())
                        {
                            case POSITION -> vertexBuilder.element(element, data.pos.x, data.pos.y, data.pos.z);
                            case UV -> vertexBuilder.element(element, data.uv[index].x, data.uv[index].y);
                            case NORMAL -> vertexBuilder.element(element, data.normal.x(), data.normal.y(), data.normal.z());
                            case COLOR -> vertexBuilder.element(element, data.color[0], data.color[1], data.color[2], data.color[3]);
                            default -> vertexBuilder;
                        };
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
    public VertexConsumer getBuffer(RenderType rt)
    {
        VertexAccumulator acc = new VertexAccumulator();
        parts.add(Pair.of(rt, acc));
        return acc;
    }
}
