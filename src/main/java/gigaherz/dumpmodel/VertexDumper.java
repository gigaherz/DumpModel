package gigaherz.dumpmodel;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.datafixers.util.Pair;
import gigaherz.dumpmodel.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class VertexDumper<M extends IMaterial<M>> implements MultiBufferSource
{
    public List<Pair<RenderType, VertexAccumulator>> parts = Lists.newArrayList();

    public final IBuilder<?, ?, ?, ?, ?, M> builder;

    private final Predicate<RenderType> doBuffer;

    public VertexDumper(IBuilder<?, ?, ?, ?, ?, M> builder)
    {
        this(builder, false);
    }

    public VertexDumper(IBuilder<?, ?, ?, ?, ?, M> builder, boolean optimized)
    {
        this(builder, rt -> optimized);
    }

    public VertexDumper(IBuilder<?, ?, ?, ?, ?, M> builder, Predicate<RenderType> doBuffer)
    {
        this.builder = builder;
        this.doBuffer = doBuffer;
    }

    public void dumpToOBJ(File file, String name)
    {
        IBuilderPart<?, ?, ?, ?, ?, M> partBuilder = builder.part(name);

        Map<ResourceLocation, M> textureMap = new HashMap<>();

        int partNumber = 0;
        for (Pair<RenderType, VertexAccumulator> part : parts)
        {
            RenderType rt = part.getFirst();
            VertexAccumulator acc = part.getSecond();

            ResourceLocation texture = null;
            if (rt instanceof RenderType.CompositeRenderType composite)
            {
                var state = composite.state();
                var tex = state.textureState;
                texture = tex.cutoutTexture().orElse(null);
            }

            IBuilderGroup<?, ?, ?, ?, ?, M> groupBuilder = partBuilder.group(String.format("%s_part_%s\n", name, ++partNumber), null);

            if (texture != null)
            {
                var texName = textureMap.computeIfAbsent(texture, tx -> {
                    var path = Utils.dumpTexture(file, tx).getAbsolutePath();
                    return builder.newMaterial(path);
                });
                groupBuilder.setMaterial(texName);
            }

            VertexFormat fmt = rt.format();
            VertexFormat.Mode drawMode = rt.mode();
            int verticesPerElement;
            if (drawMode == VertexFormat.Mode.QUADS)
                verticesPerElement = 4;
            else if (drawMode == VertexFormat.Mode.TRIANGLES)
                verticesPerElement = 3;
            else
                throw new RuntimeException(String.format("Unsupported GL drawing mode %s", drawMode));

            IBuilderFace<?, ?, ?, ?, ?, M> faceBuilder = groupBuilder.face();
            int vertexNumber = 0;
            for (VertexAccumulator.VertexData data : acc.vertices)
            {
                IBuilderVertex<?, ?, ?, ?, ?, M> vertexBuilder = faceBuilder.vertex();
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
    }

    @Override
    public VertexConsumer getBuffer(RenderType rt)
    {
        if (doBuffer.test(rt))
        {
            for(var pair : parts)
            {
                if (pair.getFirst() == rt)
                {
                    return pair.getSecond();
                }
            }
        }
        VertexAccumulator acc = new VertexAccumulator();
        parts.add(Pair.of(rt, acc));
        return acc;
    }
}
