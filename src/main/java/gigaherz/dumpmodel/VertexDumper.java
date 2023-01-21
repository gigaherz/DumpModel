package gigaherz.dumpmodel;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.datafixers.util.Pair;
import gigaherz.dumpmodel.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class VertexDumper implements MultiBufferSource
{
    public List<Pair<RenderType, VertexAccumulator>> parts = Lists.newArrayList();

    public final ModelWriter<?> builder;

    private final Predicate<RenderType> doBuffer;
    private BlockPos origin = new BlockPos(0,0,0);

    public VertexDumper(ModelWriter<?> builder)
    {
        this(builder, false);
    }

    public VertexDumper(ModelWriter<?> builder, boolean optimized)
    {
        this(builder, rt -> optimized);
    }

    public VertexDumper(ModelWriter<?> builder, Predicate<RenderType> doBuffer)
    {
        this.builder = builder;
        this.doBuffer = doBuffer;
    }

    private class Hack extends RenderStateShard
    {
        public Hack(String p_110353_, Runnable p_110354_, Runnable p_110355_)
        {
            super(p_110353_, p_110354_, p_110355_);
        }

        public static boolean isOpaque(RenderType.CompositeState state)
        {
            return state.transparencyState == RenderStateShard.NO_TRANSPARENCY;
        }
    }

    public void finish(Function<ResourceLocation, String> textureDumper, String name)
    {
        ModelGroup<?> partBuilder = builder.part(name);

        Map<Pair<ResourceLocation,AlphaMode>, ModelMaterial> materials = new HashMap<>();

        int partNumber = 0;
        for (Pair<RenderType, VertexAccumulator> part : parts)
        {
            RenderType rt = part.getFirst();
            VertexAccumulator acc = part.getSecond();

            AlphaMode mode = AlphaMode.BLEND;

            final ResourceLocation texture;
            if (rt instanceof RenderType.CompositeRenderType composite)
            {
                var state = composite.state();
                var tex = state.textureState;
                if (Hack.isOpaque(state)) mode = AlphaMode.CUTOUT;
                texture = tex.cutoutTexture().orElse(null);
            }
            else
            {
                texture = null;
            }

            ModelMesh<?> groupBuilder = partBuilder.group(String.format("%s_part_%s", name, ++partNumber), null);

            if (texture != null)
            {
                var mode1 = mode;
                var texName = materials.computeIfAbsent(Pair.of(texture, mode), tx -> {
                    var path = textureDumper.apply(texture);

                    return builder.newMaterial(path, mode1);
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

            ModelFace<?> faceBuilder = groupBuilder.face();
            int vertexNumber = 0;
            for (VertexAccumulator.VertexData data : acc.vertices)
            {
                ModelFaceVertex<?> vertexBuilder = faceBuilder.vertex();
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
                                    case COLOR -> vertexBuilder.element(element, data.color[0],data.color[1],data.color[2],data.color[3]);
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
        VertexAccumulator acc = new VertexAccumulator(rt.mode().primitiveStride, origin);
        parts.add(Pair.of(rt, acc));
        return acc;
    }

    public void setOrigin(BlockPos origin)
    {
        this.origin = origin;
    }

    public BlockPos getOrigin()
    {
        return origin;
    }
}
