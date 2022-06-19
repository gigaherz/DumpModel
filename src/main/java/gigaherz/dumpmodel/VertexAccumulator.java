package gigaherz.dumpmodel;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VertexAccumulator extends DefaultedVertexConsumer implements VertexConsumer
{
    public final List<VertexData> vertices = Lists.newArrayList();
    public final Set<Integer> colorsUsed = new HashSet<>();
    private final int primitiveLength;
    private int index = 0;
    private int currentPacked = -1;
    private int currentR = -1;
    private int currentG = -1;
    private int currentB = -1;
    private int currentA = -1;
    private VertexData current = new VertexData();

    public VertexAccumulator(int primitiveLength)
    {
        this.primitiveLength = primitiveLength;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z)
    {
        current.pos(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        var packed = FastColor.ARGB32.color(red,green,blue,alpha);
        if (index == 0)
        {
            currentR=red;
            currentG=green;
            currentB=blue;
            currentA=alpha;
            currentPacked = packed;
            colorsUsed.add(packed);
        }
        else if (currentPacked != packed)
        {
            red= currentR;
            green= currentG;
            blue= currentB;
            alpha= currentA;
            packed = currentPacked;
        }

        current.color(red, green, blue, alpha, packed);
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
        public int packedColor;

        public void pos(double x, double y, double z)
        {
            this.pos = new Vec3(x, y, z);
        }

        public void color(int red, int green, int blue, int alpha, int packed)
        {
            this.color = new float[]{red, green, blue, alpha};
            this.packedColor = packed;
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
