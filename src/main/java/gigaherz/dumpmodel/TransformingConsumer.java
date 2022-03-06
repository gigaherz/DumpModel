package gigaherz.dumpmodel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

public class TransformingConsumer implements VertexConsumer
{
    private final VertexConsumer buffer;
    private final Matrix4f posMatrix;
    private final Matrix3f normalMatrix;

    public TransformingConsumer(VertexConsumer buffer, Matrix4f posMatrix, Matrix3f normalMatrix)
    {
        this.buffer = buffer;
        this.posMatrix = posMatrix;
        this.normalMatrix = normalMatrix;
    }

    @Override
    public VertexConsumer vertex(double p_85945_, double p_85946_, double p_85947_)
    {
        return buffer.vertex(posMatrix, (float) p_85945_, (float) p_85946_, (float) p_85947_);
    }

    @Override
    public VertexConsumer color(int p_85973_, int p_85974_, int p_85975_, int p_85976_)
    {
        return buffer.color(p_85973_, p_85974_, p_85975_, p_85976_);
    }

    @Override
    public VertexConsumer uv(float p_85948_, float p_85949_)
    {
        return buffer.uv(p_85948_, p_85949_);
    }

    @Override
    public VertexConsumer overlayCoords(int p_85971_, int p_85972_)
    {
        return buffer.overlayCoords(p_85971_, p_85972_);
    }

    @Override
    public VertexConsumer uv2(int p_86010_, int p_86011_)
    {
        return buffer.uv2(p_86010_, p_86011_);
    }

    @Override
    public VertexConsumer normal(float p_86005_, float p_86006_, float p_86007_)
    {
        return buffer.normal(normalMatrix, p_86005_, p_86006_, p_86007_);
    }

    @Override
    public void endVertex()
    {
        buffer.endVertex();
    }

    @Override
    public void defaultColor(int p_166901_, int p_166902_, int p_166903_, int p_166904_)
    {
        buffer.defaultColor(p_166901_, p_166902_, p_166903_, p_166904_);
    }

    @Override
    public void unsetDefaultColor()
    {
        buffer.unsetDefaultColor();
    }
}
