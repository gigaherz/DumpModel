package gigaherz.dumpmodel;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class VertexAccumulator extends DefaultedVertexConsumer implements VertexConsumer
{
    public final List<VertexData> vertices = Lists.newArrayList();
    private final int primitiveLength;
    private final BlockPos origin;
    private int index = 0;
    private VertexData current = new VertexData();

    public VertexAccumulator(int primitiveLength, BlockPos origin)
    {
        this.primitiveLength = primitiveLength;
        this.origin = origin;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
        current.pos(x-origin.getX(), y-origin.getY(), z-origin.getZ());
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        current.color(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer uv(float u, float v)
    {
        current.tex(u, v);
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int u, int v)
    {
        current.overlay(u, v);
        return this;
    }

    @Override
    public VertexConsumer uv2(int u, int v)
    {
        current.lightmap(u, v);
        return this;
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        current.normal(x, y, z);
        return this;
    }

    @Override
    public void endVertex()
    {
        vertices.add(current);
        current = new VertexData();

        if (this.defaultColorSet)
        {
            color(this.defaultR, this.defaultG, this.defaultB, this.defaultA);
        }

        index=(index+1)%primitiveLength;
    }

    public static class VertexData
    {
        public Vec3 pos;
        public float[] color;
        public Vec2[] uv = new Vec2[3];
        public Vector3f normal;

        public void pos(double x, double y, double z)
        {
            this.pos = new Vec3(x, y, z);
        }

        public void color(int red, int green, int blue, int alpha)
        {
            this.color = new float[]{red, green, blue, alpha};
        }

        public void tex(float u, float v)
        {
            this.uv[0] = new Vec2(u, v);
        }

        public void overlay(int u, int v)
        {
            this.uv[1] = new Vec2(u, v);
        }

        public void lightmap(int u, int v)
        {
            this.uv[2] = new Vec2(u, v);
        }

        public void normal(float x, float y, float z)
        {
            this.normal = new Vector3f(x, y, z);
        }
    }
}
